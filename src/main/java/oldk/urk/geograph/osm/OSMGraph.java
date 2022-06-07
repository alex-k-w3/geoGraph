package oldk.urk.geograph.osm;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OSMGraph {
    protected final Map<Long, OSMNode> nodeById;
    protected final List<Relation> edges;
    protected final Graph<OSMNode, DefaultEdge> graphInternal;


    public OSMGraph(Map<Long, OSMNode> nodeById, List<Relation> edges) {
        this.nodeById = nodeById;
        this.edges = edges;
        this.graphInternal = new DirectedAcyclicGraph<>(DefaultEdge.class);
        nodeById.values().forEach(graphInternal::addVertex);
        edges.forEach(r -> graphInternal.addEdge(nodeById.get(r.from), nodeById.get(r.to)));
    }

    public static OSMGraph fromNodesMap(Map<Long, OSMNode> nodesById, Function<OSMNode, Set<Pair<Long, String>>> toNodeMapper) {
        var rels = nodesById.values().stream()
                .flatMap(node -> toNodeMapper.apply(node).stream().map(p -> new Relation(node.id, p.getFirst(), p.getSecond())))
                .collect(Collectors.toList());
        return new OSMGraph(nodesById, rels);
    }


    public Collection<OSMNode> getNodes() {
        return nodeById.values();
    }

    public OSMNode getNode(long id) {
        return nodeById.get(id);
    }

    public long nodesCount() {
        return nodeById.size();
    }

    public Set<Long> isolatedNodes() {
        Set<Long> allNodes = new HashSet<>(nodeById.keySet());
        Set<Long> fromArrows = edges.stream()
                .map(r -> r.from)
                .collect(Collectors.toSet());
        Set<Long> toArrows = edges.stream()
                .map(r -> r.to)
                .collect(Collectors.toSet());

        allNodes.removeAll(fromArrows);
        allNodes.removeAll(toArrows);
        return allNodes;
    }

    public Set<Long> rootsOfTrees() {
        Set<Long> allNodes = new HashSet<>(nodeById.keySet());
        Set<Long> toArrows = edges.stream()
                .map(r -> r.to)
                .collect(Collectors.toSet());
        allNodes.removeAll(toArrows);
        return allNodes;
    }

    public Collection<Relation> getEdges() {
        return edges;
    }

    public boolean addRelation(OSMNode from, OSMNode to, String relType) {
        try {
            graphInternal.addEdge(from, to);
            var rel = new Relation(from.getId(), to.getId(), relType);
            edges.add(rel);
            return true;
        } catch (Exception x) {
            return false;
        }

    }

    public int edgesCount() {
        return edges.size();
    }

    public int connectedComponentsCount() {
        var con = new ConnectivityInspector<>(graphInternal);
        return con.connectedSets().size();
    }
}
