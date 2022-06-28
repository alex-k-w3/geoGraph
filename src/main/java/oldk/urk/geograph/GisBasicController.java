package oldk.urk.geograph;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import oldk.urk.geograph.meta.MetaTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Profile("web-service")
@Api(value = "ГИС и графы - микросервис")
public class GisBasicController {
    final DataSource graphData;
    final DataSource geoData;
    final JdbcTemplate geoDb;
    final JdbcTemplate graphDb;


    @Autowired
    public GisBasicController(@Qualifier("graphData") DataSource graphData, @Qualifier("geoData") DataSource geoData) {
        this.graphData = graphData;
        this.geoData = geoData;
        geoDb = new JdbcTemplate(geoData);
        graphDb = new JdbcTemplate(graphData);
    }

    @GetMapping("/gis/search")
    @ApiOperation(value = "Поиск узлов графа, находящихся в определенном радиусе от указанных координат",
            response = SearchGeoResult.class,
            responseContainer = "List")
    public List<SearchGeoResult> search(
            @ApiParam(value = "Latitude/Широта", example = "50.450441")
            @RequestParam(value = "lat")
                    Double lat,
            @ApiParam(value = "Longitude/Долгота", example = "30.523550")
            @RequestParam(value = "lon")
                    Double lon,
            @ApiParam(value = "Радиус в метрах", example = "1000")
            @RequestParam(value = "radius")
                    Double r) throws SQLException {
        String sql = "select uid from geo_regions r where ST_DWithin(r.region, ST_MakePoint(?,?)::geography, ?)";
        List<Pair<UUID, Double>> uids = geoDb.query(searchDist, (rs, i) -> geoPair(rs), lon, lat, r);

        return uids.stream()
                .map(this::selectNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @GetMapping("/graph/nodes/{uid}")
    @ApiOperation(value = "Получение информации об узле в виде звезды - центрального узла плюс его связи")
    public GraphStar getNode(
            @ApiParam(value = "GUID узла")
            @PathVariable
                    UUID uid) {
        GraphNode n = selectSingleNode(uid);
        Set<GraphRelation> rels = getStarsRelations(uid);
        return new GraphStar(n, rels);
    }

    /*
    {
"props": { "aaa": 1.32, "bbb": 2.22 },
  "types": [
    "Ent", "Place"
  ],
  "uid": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
     */
    @PostMapping("/graph/nodes")
    @ApiOperation(value = "Создать новый узел")
    public ResponseEntity createNode(
            @ApiParam(value = "Узел")
            @RequestBody
                    GraphNode newNode) {
        if (!newNode.types.contains(MetaTypes.NOD_ENT))
            newNode.types.add(MetaTypes.NOD_ENT);
        if (newNode.types.stream().anyMatch(t -> !MetaTypes.NODE_TYPES.contains(t)))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid node type");

        String types  = newNode.types.stream()
                .collect(Collectors.joining(":"));
        String sql = String.format("CREATE (n:%s ?)", types);
        Map<String, Object> props = new HashMap<>(newNode.props);
        props.put("uid", newNode.uid.toString());
        graphDb.update(sql, props);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/graph/rels")
    @ApiOperation(value = "Создать новую связь между существующими узлами (Пока не дергаем - не работает!!!)")
    public ResponseEntity createRelation(@RequestBody GraphRelation relation, @RequestParam("from") UUID from, @RequestParam("to") UUID to) {
        if (!MetaTypes.REL_TYPES.contains(relation.relType))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid relation type");
        String sqlPattern = "MATCH\n" +
                "  (a),\n" +
                "  (b)\n" +
                "WHERE a.uid = ? AND b.uid = ?\n" +
                "MERGE (a)-[r:%s]->(b)\n" +
                "ON CREATE SET %s";
        Map<String, Object> props = new HashMap<>(relation.props);
        props.put("uid", relation.uid.toString());
        List<String> keyList = new ArrayList<>(relation.props.keySet());
        String propsStr = keyList.stream().map(k -> toKeyValue("r", k)).collect(Collectors.joining(", "));
        Object[] vals = keyList.stream().map(k -> props.get(k)).toArray();
        String sql = String.format(sqlPattern, relation.relType, propsStr);
        graphDb.update(sql, vals);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    private String toKeyValue(String prefix, String key) {
        return prefix + "." + key + "=?";
    }

    Set<GraphRelation> getStarsRelations(UUID nodeUid) {
        final var sql =
                "match (a)-[r]-(b)\n" +
                "where a.uid=?\n" +
                "return ID(a), r, ID(b), b.uid";
        List<RawStarsData> rawList = graphDb.query(sql, (rs, i)->RawStarsData.of(rs), nodeUid.toString());
        return rawList.stream()
                .map(r -> r.toGraphRelation(nodeUid))
                .collect(Collectors.toSet());
    }

    private static class RawStarsData {
        final long centerId;
        final long otherId;
        final UUID otherUid;
        final Map<String, Object> relRawProps;

        public RawStarsData(long centerId, long otherId, UUID otherUid, Map<String, Object> relRawProps) {
            this.centerId = centerId;
            this.otherId = otherId;
            this.otherUid = otherUid;
            this.relRawProps = relRawProps;
        }

        public static RawStarsData of(ResultSet rs) throws SQLException {
            Map<String, Object> props = (Map<String, Object>) rs.getObject(2);
            UUID uid = UUID.fromString(rs.getString(4));

            return new RawStarsData(
                rs.getLong(1),
                rs.getLong(3),
                uid,
                props
            );
        }

        public GraphRelation toGraphRelation(UUID nodeUid) {
            Long start = (Long) relRawProps.get("_startId");
            Long end = (Long) relRawProps.get("_endId");
            if (start==null || end==null)
                throw new RuntimeException("Invalid start-end rel ids: nulls");
            String rtype = (String) relRawProps.get("_type");
            UUID relUid = UUID.fromString((String)relRawProps.get("uid"));
            Map<String, Object> props = new HashMap<>(relRawProps);
            props.remove("uid");
            props.remove("_startId");
            props.remove("_endId");
            props.remove("_type");
            props.remove("_id");
            if (start == centerId)
                return new GraphRelation(relUid, nodeUid, otherUid, rtype, props);
            else
                return new GraphRelation(relUid, otherUid, nodeUid, rtype, props);
        }
    }

    static final String searchDist = "with pnt as (select ST_MakePoint(?, ?)::geography as pnt),\n" +
            "\tregions_near as (select uid, ST_Distance(region, pnt.pnt) as distance, ST_Area(region) as area from geo_regions r, pnt where ST_DWithin(r.region, pnt.pnt, ?)),\n" +
            "\tregions_sorted as (\n" +
            "(select * from regions_near rn where rn.distance = 0 order by rn.area limit 1)\n" +
            "union\n" +
            "(select * from regions_near rn where rn.distance > 0 order by rn.area)\n" +
            ")\n" +
            "select rs.uid, rs.distance from regions_sorted rs order by rs.distance\n";

    private static Pair<UUID, Double> geoPair(ResultSet rs) throws SQLException {
        var uid = (UUID)rs.getObject(1);
        var dist = rs.getDouble(2);
        return Pair.of(uid, dist);
    }

    @Nullable
    private SearchGeoResult selectNode(Pair<UUID, Double> pair) {
        GraphNode n = selectSingleNode(pair.getFirst());
        if (n == null) return null;
        return new SearchGeoResult(pair.getSecond(), n);
    }

    @Nullable
    private GraphNode selectSingleNode(UUID uid) {
        String c = "match (n) where n.uid=? return n";
        var l = graphDb.query(c,(rs, i)-> mapObject(rs), uid.toString());
        if (l.size() == 0) {
            return null;
        }
        Map<String, Object> dirtyMap = new HashMap<>(l.get(0));
        var types = (List<String>)dirtyMap.get("_labels");
        dirtyMap.remove("_id");
        dirtyMap.remove("_labels");
        dirtyMap.remove("uid");
        var n = new GraphNode(uid, types, dirtyMap);
        return n;
    }

    private Map<String, Object> mapObject(ResultSet rs) throws SQLException {
        return (Map<String, Object>) rs.getObject(1);
    }
}
