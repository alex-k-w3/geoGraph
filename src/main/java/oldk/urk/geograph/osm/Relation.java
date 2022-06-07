package oldk.urk.geograph.osm;

import java.util.Collections;
import java.util.Map;

public class Relation {
    final long from;
    final long to;
    final String type;
    final Map<String, String> tags;

    public Relation(long from, long to, String type) {
        this(from, to, type, Collections.emptyMap());
    }

    public Relation(long from, long to, String type, Map<String, String> tags) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.tags = tags;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}
