package oldk.urk.geograph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Profile("web-service")
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

    /*
    Показать расстояние
    1 объект минимальной площади с расстоянием 0
    with pnt as (select ST_MakePoint(31.683521, 47.585532)::geography as pnt),
	regions_near as (select uid, ST_Distance(region, pnt.pnt) as distance, ST_Area(region) as area from geo_regions r, pnt where ST_DWithin(r.region, pnt.pnt, 200000))
select * from regions_near rn
order by area

     */

    @GetMapping("/gis/search")
    public List<Map<String, Object>> search(@RequestParam(value = "lat") Double lat, @RequestParam(value = "lon") Double lon, @RequestParam(value = "radius") Double r) throws SQLException {
        String sql = "select uid from geo_regions r where ST_DWithin(r.region, ST_MakePoint(?,?)::geography, ?)";
        List<UUID> uids = geoDb.query(sql, (rs, i) -> (UUID)rs.getObject(1), lon, lat, r);


        return uids.stream()
                .map(this::selectNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> selectNode(UUID uid) {
        String c = "match (n) where n.uid=? return properties(n)";
        var l = graphDb.query(c,(rs, i)-> mapObject(rs), uid.toString());
        if (l.size() == 0)
            return null;
        return l.get(0);
    }

    private Map<String, Object> mapObject(ResultSet rs) throws SQLException {
        return (Map<String, Object>) rs.getObject(1);
    }
}
