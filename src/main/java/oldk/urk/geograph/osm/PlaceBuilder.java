package oldk.urk.geograph.osm;

import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.slimjars.dist.gnu.trove.map.hash.TLongObjectHashMap;
import com.slimjars.dist.gnu.trove.set.TLongSet;
import com.slimjars.dist.gnu.trove.set.hash.TLongHashSet;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.geometry.RegionBuilderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class PlaceBuilder  {
    static Logger LOGGER = LoggerFactory.getLogger(PlaceBuilder.class);
    static final Set<String> REQUIRED_ENTITIES = Set.of(
            "outer", "inner", "admin_centre", "subarea", "label");

    final long id;
    final OsmRelation rel;

    final Map<String, String> tags;
    final String name;

    public PlaceBuilder(long id, OsmRelation rel) {
        this.id = id;
        this.rel = rel;
        this.tags = OsmModelUtil.getTagsAsMap(rel);
        this.name = tags.getOrDefault("name", null);
    }



    public NodeRequirements getRequirements(RequirementHandler wayAdded) {
        TLongSet requiredEntities = new TLongHashSet();
        var rels = OsmModelUtil.membersAsList(rel);
        for(var r : rels) {
            if (REQUIRED_ENTITIES.contains(r.getRole()))
                requiredEntities.add(r.getId());
        }
        NodeRequirements r = NodeRequirements.empty();
        return r.add(requiredEntities, wayAdded);
    }

    public OsmPlace build(GeometryBuilder gb, OsmEntityProvider entityProvider) {
        try {
            RegionBuilderResult rbr = gb.getRegionBuilder().build(rel, entityProvider);
            return new OsmPlace(id, rel, OsmModelUtil.getTagsAsMap(rel), rbr.getMultiPolygon());
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Can't build object:{}:{}. Some linked entity not found:{}", id, name, e.getMessage());
            return null;
        }
    }

    public long getId() {
        return id;
    }

    public OsmRelation getRel() {
        return rel;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getName() {
        return name;
    }
}
