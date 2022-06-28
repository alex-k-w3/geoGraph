package oldk.urk.geograph.osm;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class Relation {
    final UUID uid;
    final long from;
    final long to;
    final String type;
    final Map<String, String> tags;

    public Relation(long from, long to, String type) {
        this(from, to, type, Collections.emptyMap());
    }

    public Relation(long from, long to, String type, Map<String, String> tags) {
        this(UUID.randomUUID(), from, to, type, tags);
    }

    public Relation(UUID uid, long from, long to, String type, Map<String, String> tags) {
        this.uid = uid;
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

    public UUID getUid() {
        return uid;
    }
}
