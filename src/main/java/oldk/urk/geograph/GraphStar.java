package oldk.urk.geograph;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Звезда - узел и все входящие и исходящие дуги
 */
@ApiModel(description = "Звезда - узел и все входящие и исходящие дуги")
public class GraphStar {
    @ApiModelProperty(value = "Центральный узел звезды")
    public final GraphNode node;
    @Nullable

    @ApiModelProperty(value = "Связи узла", dataType = "[Loldk.urk.geograph.GraphRelation;")
    public final Set<GraphRelation> relations;

    public GraphStar(GraphNode node) {
        this(node, null);
    }

    public GraphStar(GraphNode node, @Nullable Set<GraphRelation> relations) {
        this.node = node;
        this.relations = relations!=null? Collections.unmodifiableSet(relations): null;
    }


}
