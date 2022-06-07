package oldk.urk.geograph.osm;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmWay;

@FunctionalInterface
public interface RequirementHandler {
    NodeRequirements handle(OsmEntity e);
}
