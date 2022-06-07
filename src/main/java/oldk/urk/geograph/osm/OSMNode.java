package oldk.urk.geograph.osm;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class OSMNode {
    final long id;
    @Nullable
    final Map<String, String> tags;
    final OsmEntity osmEntity;
    @Nullable
    final String name;
    UUID uuid;

    public OSMNode(long id, OsmEntity osmEntity, @Nullable Map<String, String> tags) {
        this.id = id;
        this.tags = tags;
        this.osmEntity = osmEntity;
        this.name = tags!=null ? tags.get("name"): null;
        this.uuid = UUID.randomUUID();
    }

    public static OSMNode of(OsmEntity entity, Map<String, String> tags) {
        return new OSMNode(entity.getId(), entity, tags);
    }

    @Nullable
    public Map<String, String> getTags() {
        return tags;
    }

    public OsmEntity getOsmEntity() {
        return osmEntity;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "OSMNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public Optional<String> getTagOpt(String name) {
        if (tags == null)
            return Optional.empty();
        return Optional.ofNullable(tags.get(name));
    }

    public void setUuid(UUID existingUID) {
        uuid = existingUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OSMNode node = (OSMNode) o;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
