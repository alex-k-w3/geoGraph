package oldk.urk.geograph.osm;

import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.slimjars.dist.gnu.trove.map.hash.TLongObjectHashMap;
import de.topobyte.osm4j.core.model.iface.OsmEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IterationRequiremets {

    public static IterationRequiremets empty() {
        return new IterationRequiremets();
    }

    public IterationRequiremets() {
        requirements = new TLongObjectHashMap<>();
    }

    public TLongObjectMap<List<RequirementHandler>> requirements;


    public IterationRequiremets handle(OsmEntity ent) {
        long id = ent.getId();
        IterationRequiremets r = IterationRequiremets.empty();
        var handlers = requirements.get(id);
        if (handlers!=null) {
            for(RequirementHandler h : handlers) {
                r.add(h.handle(ent));
            }
        }
        return r;
    }

    public void add(NodeRequirements nr) {
        nr.getHandlers().forEachEntry(this::addRequirement);
    }

    private boolean addRequirement(long l, RequirementHandler handler) {
        var list = requirements.get(l);
        if (list == null) {
            var newList  = new ArrayList<RequirementHandler>();
            newList.add(handler);
            requirements.put(l, newList);
        } else
            list.add(handler);
        return true;
    }

    private boolean addRequirements(long l, List<RequirementHandler> handlers) {
        var list = requirements.get(l);
        if (list == null)
            requirements.put(l, new ArrayList<>(handlers));
        else
            list.addAll(handlers);
        return true;
    }


    public long size() {
        return requirements.size();
    }

    public boolean isEmpty() {
        return requirements.isEmpty();
    }

    public void add(IterationRequiremets other) {
        other.requirements.forEachEntry(this::addRequirements);
    }
}
