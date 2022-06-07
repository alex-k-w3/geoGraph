package oldk.urk.geograph.osm;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.lang.Nullable;

import java.util.Map;

public class OsmPlace extends OSMNode {
    public final MultiPolygon geometry;


    public OsmPlace(long id, OsmEntity entity, @Nullable Map<String, String> tags, MultiPolygon geometry) {
        super(id, entity, tags);
        this.geometry = geometry;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }
}
