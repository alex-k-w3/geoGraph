package oldk.urk.geograph.meta;

import java.util.Set;

public interface MetaTypes {
    String REL_CONTAINS = "CONTAINS";   // (A) -[:CONTAINS] -> (B) Node A contains node B
    String NOD_PLACE = "Place";         // The node is place node
    String NOD_ENT = "Ent";             // Entity with uid

    Set<String> NODE_TYPES = Set.of(NOD_PLACE, NOD_ENT);
    Set<String> REL_TYPES = Set.of(REL_CONTAINS);
}
