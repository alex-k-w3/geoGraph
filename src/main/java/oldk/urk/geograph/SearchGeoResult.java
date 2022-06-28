package oldk.urk.geograph;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Результаты поиска узла по координатам")
public class SearchGeoResult {

    @ApiModelProperty(value = "Расстояние от точки поиска (0 - означает, что точка попала внутрь данного объекта")
    public final double distance;

    @ApiModelProperty(value = "Найденный узел графа")
    public final GraphNode node;

    public SearchGeoResult(double distance, GraphNode node) {
        this.distance = distance;
        this.node = node;
    }
}
