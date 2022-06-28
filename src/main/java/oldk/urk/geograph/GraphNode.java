package oldk.urk.geograph;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApiModel(description = "Узел графа")
public class GraphNode {

    @ApiModelProperty(value = "GUID узла графа")
    public final UUID uid;
    @ApiModelProperty(value = "Типы данного узла. Узел может иметь несколько типов. Список типов содержится в метадарнных системы, " +
            "в данный момент узел может иметь только один захардкоженный тип: Place - место", example = "Place")
    public final List<String> types;
    @ApiModelProperty(value = "Свойства данного узла в виде пар: имя-значение")
    public final Map<String, Object> props;

    public GraphNode(UUID uid, List<String> types) {
        this(uid, types, Collections.emptyMap());
    }

    @JsonCreator
    public GraphNode(@JsonProperty("uid") UUID uid, @JsonProperty("types") List<String> types, @JsonProperty("props") Map<String, Object> props) {
        this.uid = uid;
        this.types = types;
        this.props = Collections.unmodifiableMap(props);
    }
}
