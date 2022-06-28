package oldk.urk.geograph;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;


@ApiModel(description = "Связь")
public class GraphRelation {
    @ApiModelProperty(value = "GUID связи")
    public final UUID uid;
    @ApiModelProperty(value = "GUID узла-источника, откуда выходит данная связь")
    public final UUID from;
    @ApiModelProperty(value = "GUID узла-приемника, куда входит данная связь")
    public final UUID to;
    @ApiModelProperty(value = "Тип связи. Связь имеет строго один тип, набор типов связей возможных в системе определяется метаданными.")
    public final String relType;
    @Nullable
    @ApiModelProperty(value = "Прочие именованные свойства связи")
    public final Map<String, Object> props;

    public GraphRelation(UUID uid, UUID from, UUID to, String relType) {
        this(uid, from, to, relType, null);
    }

    public GraphRelation(UUID uid, UUID from, UUID to, String relType, @Nullable Map<String, Object> props) {
        this.uid = uid;
        this.from = from;
        this.to = to;
        this.relType = relType;
        this.props = props;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphRelation that = (GraphRelation) o;
        return uid.equals(that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}
