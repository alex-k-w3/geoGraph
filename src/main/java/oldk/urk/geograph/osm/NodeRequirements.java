package oldk.urk.geograph.osm;

import com.slimjars.dist.gnu.trove.TLongCollection;
import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.slimjars.dist.gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Set;

public class NodeRequirements {
    private static final NodeRequirements EMPTY = new NodeRequirements();
    final private TLongObjectMap<RequirementHandler> handlers;

    public NodeRequirements() {
        this(new TLongObjectHashMap<>());
    }

    public NodeRequirements(TLongObjectMap<RequirementHandler> handlers) {
        this.handlers = handlers;
    }

    public static NodeRequirements empty() {
        return EMPTY;
    }

    public NodeRequirements add(TLongCollection requiredId, RequirementHandler wayAdded) {
        if (!requiredId.isEmpty()) {
            TLongObjectMap<RequirementHandler> newMap = new TLongObjectHashMap<>(handlers);
            requiredId.forEach(id -> {
                newMap.put(id, wayAdded);
                return true;
            });
            return new NodeRequirements(newMap);
        } else
            return this;
    }

    public TLongObjectMap<RequirementHandler> getHandlers() {
        return handlers;
    }
}
