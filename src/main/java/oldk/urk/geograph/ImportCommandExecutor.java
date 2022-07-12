package oldk.urk.geograph;

import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.slimjars.dist.gnu.trove.map.hash.TLongObjectHashMap;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import me.tongfei.progressbar.ProgressBar;
import oldk.urk.geograph.meta.MetaTypes;
import oldk.urk.geograph.osm.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportCommandExecutor {
    Logger LOGGER = LoggerFactory.getLogger(CommandLineHandler.class);
    public final String fileName;
    public final DataSource graphDs;
    public final DataSource geoDs;
    final TLongObjectMap<OsmWay> waysMap;
    final TLongObjectMap<OsmNode> nodesMap;



    public ImportCommandExecutor(String fileName, DataSource graphDs, DataSource geoDs) {
        this.fileName = fileName;
        this.graphDs = graphDs;
        this.geoDs = geoDs;
        this.waysMap = new TLongObjectHashMap<>();
        this.nodesMap = new TLongObjectHashMap<>();
    }

    public void execute() {
        LOGGER.info("Start importing {}", fileName);
        try {
            Map<Long, PlaceBuilder> rootObjects = loadInputFile();
            List<OsmPlace> places = buildPlaces(rootObjects);
            OSMGraph graph = buildGraph(places);
            LOGGER.info("Graph by administrative data from OSM. Nodes:{} Edges:{}. Connected components: {}", graph.nodesCount(), graph.edgesCount(), graph.connectedComponentsCount());

            saveToGeo(places);
            wireFromGeo(graph);
            saveToGraphDB(graph);
            LOGGER.info("Items collected: {}", places.size());

        } catch (IOException x) {
            LOGGER.error("File operation error", x);
        } catch (SQLException x) {
            LOGGER.error("SQL operation error", x);
        }
    }

    private void wireFromGeo(OSMGraph graph) throws SQLException {
        LOGGER.info("Connecting graph nodes with no relations in OSM...");
        var start = System.currentTimeMillis();
        var rootNodes = graph.rootsOfTrees();
        try(ProgressBar pb = new ProgressBar("Connecting nodes", rootNodes.size())) {
            Quadtree tree = new Quadtree();
            for(var n : graph.getNodes()) {
                if (n instanceof OsmPlace) {
                    OsmPlace pl = (OsmPlace) n;
                    tree.insert(pl.getGeometry().getEnvelopeInternal(), n.getId());
                }
            }
            rootNodes.forEach(id -> {
                        linkToContainer(graph, tree ,id);
                        pb.step();
                    });
        }
        var duration = System.currentTimeMillis() - start;
        LOGGER.info("Graph after connecting by geometry in {} [ms]. Nodes:{} Edges:{}. Connected components: {}",
                duration, graph.nodesCount(), graph.edgesCount(), graph.connectedComponentsCount());
    }

    private void linkToContainer(OSMGraph graph, Quadtree rects, Long id) {
        OSMNode n = graph.getNode(id);
        if (n instanceof OsmPlace) {
            OsmPlace place = (OsmPlace) n;
            final Geometry placeGeo = place.getGeometry();

            List<Long> ids = rects.query(placeGeo.getEnvelopeInternal());
            if (ids!=null) {
                var ownerFound = ids.stream()
                        .filter(i -> !Objects.equals(i, id))
                        .map(graph::getNode)
                        .filter(nd -> nd instanceof OsmPlace)
                        .map(i -> (OsmPlace) i)
                        .filter(OsmPlace::isValidGeometry)
                        .filter(i -> checkContains(placeGeo, i))
                        .min(Comparator.comparingDouble(OsmPlace::getArea))
                        .map(i -> graph.addRelation(i, place, MetaTypes.REL_CONTAINS))
                        .orElse(false);
                if (!ownerFound)
                    LOGGER.warn("Owner of OSM entity is not found: {} osm id: {}", place.getName(), id);
            }
            /*
            var ownerFound = graph.getNodes().stream()
                    .filter(i -> i instanceof OsmPlace)
                    .filter(i -> i.getId() != id)
                    .map(i -> (OsmPlace)i)
                    .filter(i -> i.getGeometry().isValid())
                    .filter(i -> i.getGeometry().contains(placeGeo))
                    .sorted(Comparator.comparingDouble(i->i.getGeometry().getArea()))
                    .map(i -> graph.addRelation(i, place, MetaTypes.REL_CONTAINS))
                    .findFirst()
                    .orElse(false);
             */
        }
    }

    private boolean checkContains(Geometry placeGeo, OsmPlace i) {
        try {
            return i.getGeometry().covers(placeGeo);
        } catch (TopologyException x) {
            LOGGER.warn("Problem with geometry:{} details:{}" , i, x.getMessage());
        }
        return false;
    }

    private void saveToGraphDB(OSMGraph graph) throws SQLException {
        LOGGER.info("Saving Graph info...");
        Connection conn = graphDs.getConnection();
        conn.setAutoCommit(false);
        try(PreparedStatement stmNodes = conn.prepareStatement("CREATE (n:Ent:Place ?)");
            ProgressBar pb = new ProgressBar("Saving graph nodes", graph.nodesCount())) {
            graph.getNodes().forEach(n -> {
                saveGraphNode(n, conn, stmNodes);
                pb.step();
            });
        }

        var edgesByType = graph.getEdges().stream()
                .collect(Collectors.groupingBy(Relation::getType));
        try(ProgressBar pb = new ProgressBar("Saving graph relations", graph.edgesCount())) {
            for (var kv : edgesByType.entrySet()) {
                try (PreparedStatement stmRels = conn.prepareStatement(String.format("MATCH\n" +
                        "  (a:Place),\n" +
                        "  (b:Place)\n" +
                        "WHERE a.uid = ? AND b.uid = ?\n" +
                        "MERGE (a)-[r:%s]->(b)" +
                        "ON CREATE SET r.uid=?, r.src=\"osm\"", kv.getKey()))) {

                    kv.getValue().forEach(r -> {
                        saveRelation(graph, r, conn, stmRels);
                        pb.step();
                    });
                }

            }
        }
    }

    private void saveRelation(OSMGraph graph, Relation r, Connection conn, PreparedStatement stmRels) {
        try {
            UUID uidFrom = graph.getNode(r.getFrom()).getUuid();
            UUID uidTo = graph.getNode(r.getTo()).getUuid();
            stmRels.setString(1, uidFrom.toString());
            stmRels.setString(2, uidTo.toString());
            stmRels.setString(3, r.getUid().toString());
            stmRels.execute();
            conn.commit();
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    private void saveGraphNode(OSMNode n, Connection conn, PreparedStatement stmNodes) {
        try {
            var nodeProps = getNodeProps(n);
            stmNodes.setObject(1, nodeProps);
            stmNodes.executeUpdate();
            conn.commit();
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    final static Map<String, String> OSM_TO_GRAPH_PROPS = new HashMap<>();
    static {
        OSM_TO_GRAPH_PROPS.put("name:en",       "name_en"       );
        OSM_TO_GRAPH_PROPS.put("name:uk",       "name_uk"       );
        OSM_TO_GRAPH_PROPS.put("name:ru",       "name_ru"       );
        OSM_TO_GRAPH_PROPS.put("place",         "osm_place"     );
        OSM_TO_GRAPH_PROPS.put("amenity",       "osm_amenity"   );
        OSM_TO_GRAPH_PROPS.put("boundary",      "osm_boundary"  );
        OSM_TO_GRAPH_PROPS.put("admin_level",   "osm_admin_level");
        OSM_TO_GRAPH_PROPS.put("border_type",   "osm_border_type");
        OSM_TO_GRAPH_PROPS.put("start_date",    "osm_start_date");
        OSM_TO_GRAPH_PROPS.put("military",      "osm_military");
        OSM_TO_GRAPH_PROPS.put("name:prefix",   "osm_name_prefix");
        OSM_TO_GRAPH_PROPS.put("population",    "osm_population");
    }
/*
            "aeroway", "osm_aeroway",
            "barrier", "osm_barrier",
            "building", "osm_building",
            "height", "osm_height",
            "craft", "osm_craft",
            "emergency", "osm_emergency",
            "lifeguard", "osm_lifeguard",
            "healthcare", "osm_healthcare",
            "highway", "osm_highway",
            "footway", "osm_footway",
            "historic", "osm_historic",
            "landuse", "osm_landuse",

            "man_made", "osm_man_made",
            "natural", "osm_natural",
            "office", "osm_office",
            "power", "osm_power",
            "public_transport", "osm_public_transport",
            "railway", "osm_railway",
            "route", "osm_route",
            "shop", "osm_shop",
            "telecom", "osm_telecom",
            "tourism", "osm_tourism",
            "water", "osm_water",
            "waterway", "osm_waterway",
            "addr:housenumber", "osm_addr_housenumber",
            "addr:housename", "osm_addr_housename",
            "addr:flats", "osm_addr_flats",
            "addr:conscriptionnumber", "osm_addr_conscriptionnumber",
            "addr:street", "osm_addr_street",
            "addr:place",
            "addr:city",
            "addr:country"
            "addr:full",
            "end_date",
            "traffic_sign"
*/

    private Map<String, Object> getNodeProps(OSMNode n) {
        var rv = new HashMap<String, Object>();
        rv.put("uid", n.getUuid().toString());
        rv.put("src", "osm");
        rv.put("name", n.getName());
        var tags = n.getTags();
        if (tags!=null) {
            tags.entrySet().stream()
                    .filter(kv -> OSM_TO_GRAPH_PROPS.containsKey(kv.getKey()))
                    .forEach(kv -> rv.put(OSM_TO_GRAPH_PROPS.get(kv.getKey()), kv.getValue()));
        }
        return rv;
    }

    private void saveToGeo(List<OsmPlace> places) throws SQLException {
        LOGGER.info("Saving Geo info...");
        Connection conn = geoDs.getConnection();
        conn.setAutoCommit(false);
        PreparedStatement stmObjects = conn.prepareStatement("INSERT INTO geo_objects(uid, object_type, osm_id) values (?, ?, ?)");
        PreparedStatement stmRegs = conn.prepareStatement("INSERT INTO geo_regions(uid, region) VALUES(?, ST_GeomFromEWKT(?))");
        PreparedStatement stmCheck = conn.prepareStatement("SELECT uid FROM geo_objects WHERE osm_id=?");
        try(ProgressBar pb = new ProgressBar("Saving geometry database items", places.size())) {
            places.forEach(p -> {
                insertPlace(p, conn, stmObjects, stmRegs, stmCheck);
                pb.step();
            });
        }
    }

    private void insertPlace(OsmPlace p, Connection conn, PreparedStatement stmObjects, PreparedStatement stmRegs, PreparedStatement stmCheck) {
        try {
            stmCheck.setLong(1, p.getId());
            ResultSet rs = stmCheck.executeQuery();
            if (rs.next()) {
              UUID existingUID = (UUID) rs.getObject(1);
              p.setUuid(existingUID);
              LOGGER.warn("Place already exists:{}", p.getName());
            } else {
                stmObjects.setObject(1, p.getUuid());
                stmObjects.setString(2, MetaTypes.NOD_PLACE);
                stmObjects.setLong(3, p.getId());
                stmObjects.execute();
                stmRegs.setObject(1, p.getUuid());
                stmRegs.setString(2, p.geometry.toText());
                stmRegs.execute();
                conn.commit();
            }
        } catch (SQLException x) {
            LOGGER.error("Error inserting place:" + p.getId(), x);
            throw new RuntimeException(x);
        }
    }

    private OSMGraph buildGraph(List<OsmPlace> places) {
        LOGGER.info("Building graph...");
        Map<Long, OSMNode> nodesById = places.stream().collect(Collectors.toMap(OSMNode::getId, Function.identity()));
        var rv = OSMGraph.fromNodesMap(nodesById, n -> linksOfNode(n, nodesById));
        LOGGER.info("Building graph DONE. Nodes: {} relations {}", rv.nodesCount(), rv.edgesCount());
        return rv;
    }

    private Set<Pair<Long, String>> linksOfNode(OSMNode n, Map<Long, OSMNode> nodesById) {
        var rels = OsmModelUtil.membersAsList((OsmRelation) n.getOsmEntity());
        Set<Pair<Long, String>> links = new HashSet<>();
        for(var rel : rels) {
            if("subarea".equals(rel.getRole())) {
                OSMNode relNode = nodesById.get(rel.getId());
                if (relNode!=null) {
                    links.add(Pair.of(relNode.getId(), MetaTypes.REL_CONTAINS));
                }
            }
        }
        return links;
    }

    private List<OsmPlace> buildPlaces(Map<Long, PlaceBuilder> rootObjects) {
        LOGGER.info("Building geometry...");
        GeometryBuilder gb = new GeometryBuilder();
        List<OsmPlace> places = rootObjects.values().stream()
                .map(b -> b.build(gb, new Resolver(rootObjects)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        LOGGER.info("Building geometry DONE. Places: " + places.size());
        return places;
    }

    private Map<Long, PlaceBuilder> loadInputFile() throws IOException {
        Map<Long, PlaceBuilder> rootObjects = new HashMap<>();
        int iterCounter = 0;
        IterationRequiremets prevReqs = IterationRequiremets.empty();
        long maxItems = -1;
        long items = 0;
        do {
            LOGGER.info("Iteration #{} started", iterCounter + 1);
            try(ProgressBar pb = new ProgressBar("Loading...Iteration #" + (iterCounter+1), maxItems)) {

                IterationRequiremets newReqs = IterationRequiremets.empty();
                try (InputStream pstr = Files.newInputStream(Paths.get(fileName))) {
                    OsmIterator iterator = new PbfIterator(pstr, false);
                    for (var object : iterator) {
                        OsmEntity ent = object.getEntity();
                        pb.step();

/*
                    if (ent instanceof OsmRelation) {
                        OsmRelation rrr = (OsmRelation) ent;
                        var rels = OsmModelUtil.membersAsList(rrr);
                        var r = rels.stream().filter(m -> m.getId()==2175078L).findFirst().orElse(null);
                        if (r != null)
                            System.out.println("Break>>");
                    }

 */
                        newReqs.add(prevReqs.handle(ent));
                        var tags = OsmModelUtil.getTagsAsMap(ent);
                        if (isAcceptable(ent, tags)) {
                            if (!rootObjects.containsKey(ent.getId())) {
                                PlaceBuilder placeBuilder = new PlaceBuilder(ent.getId(), (OsmRelation) ent);
                                rootObjects.put(ent.getId(), placeBuilder);
                                NodeRequirements req = placeBuilder.getRequirements(this::entityAdded);
                                newReqs.add(req);
                            }
                        }
                    items++;
                    }
                }
                LOGGER.info("Iteration #{} done. Required {}", iterCounter + 1, prevReqs.size());
                prevReqs = newReqs;
                maxItems = items;
                items = 0;
            }
            iterCounter++;
        } while (!prevReqs.isEmpty());
        return rootObjects;
    }

    Set<String> PLACE_TYPES = Set.of("country", "state", "region", "province", "city", "borough", "suburb", "quarter",
            "town", "village", "hamlet", "isolated_dwelling", "farm", "island");

    protected boolean isAcceptable(OsmEntity ent, Map<String, String> tags) {
        //String type = tags.get("type");
        //if ("boundary".equals(type))
        {
            String boundary = tags.get("boundary");
            if ("administrative".equals(boundary))
                return (ent.getType() == EntityType.Relation);
        }   {
            String place = tags.get("place");
            if (place != null && PLACE_TYPES.contains(place))
                return (ent.getType() == EntityType.Relation);
        }
        return false;
    }

    public NodeRequirements entityAdded(OsmEntity entity) {
        if (entity.getType()== EntityType.Way) {
            OsmWay w = (OsmWay) entity;
            waysMap.put(entity.getId(), w);
            var nodes = OsmModelUtil.nodesAsList(w);
            var req = NodeRequirements.empty();
            return req.add(nodes, this::entityAdded);
        } else if(entity.getType() == EntityType.Node) {
            nodesMap.put(entity.getId(), (OsmNode) entity);
        }
        return NodeRequirements.empty();
    }

    private  class Resolver implements OsmEntityProvider {
        final Map<Long, PlaceBuilder> rootObjects;

        public Resolver(Map<Long, PlaceBuilder> rootObjects) {
            this.rootObjects = rootObjects;
        }

        @Override
        public OsmNode getNode(long id) throws EntityNotFoundException {
            var rv = nodesMap.get(id);
            if (rv==null)
                throw new EntityNotFoundException("node:"+id);
            return rv;
        }

        @Override
        public OsmWay getWay(long id) throws EntityNotFoundException {
            var rv = waysMap.get(id);
            if (rv==null) {
                throw new EntityNotFoundException("way:" + id);
            }
            return rv;
        }

        @Override
        public OsmRelation getRelation(long id) throws EntityNotFoundException {
            var o = rootObjects.get(id);
            if (o == null)
                throw new EntityNotFoundException("rel:"+id);
            return o.getRel();
        }
    }



}
