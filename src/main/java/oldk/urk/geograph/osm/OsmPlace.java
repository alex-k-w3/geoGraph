package oldk.urk.geograph.osm;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.lang.Nullable;

import java.util.Map;

public class OsmPlace extends OSMNode {
    public final MultiPolygon geometry;
    private Double area = null;
    private Boolean geoIsValid = null;


    public OsmPlace(long id, OsmEntity entity, @Nullable Map<String, String> tags, MultiPolygon geometry) {
        super(id, entity, tags);
        this.geometry = geometry;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }

    public double getArea() {
        if (area == null)
            area = geometry.getArea();
        return area;
    }

    public boolean isValidGeometry() {
        if (geoIsValid == null)
            geoIsValid = geometry.isValid();
        return geoIsValid;
    }
}
