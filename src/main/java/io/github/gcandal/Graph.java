package io.github.gcandal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Graph {
    private Map<String, Node> nodes = new HashMap<>();
    private PriorityQueue<Node> topologicalOrder = new PriorityQueue<>(new NodeTopologicalComparator());

    Graph(String fileName) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(this::addNodes);
        }
        if(!isAcyclic()) {
            throw new RuntimeException();
        }
        nodes.values().forEach(node -> topologicalOrder.add(node));
        initiateLatches();
    }

    private void initiateLatches() {
        nodes.values().forEach(Node::initiateOwnLatch);
        nodes.values().forEach(
                parent -> parent.getDependencies().forEach(
                        child -> child.addParentLatch(parent.getOwnLatch())
                )
        );
    }

    private void addNodes(String line) {
        List<String> nodeIds = Arrays.asList(line.split(" "));
        String sourceId = nodeIds.get(0);
        Integer nrSinks = nodeIds.size() - 1;
        List<String> sinkIds;
        if(nrSinks > 0){
            sinkIds = nodeIds.subList(1, nrSinks + 1);
        } else {
            sinkIds = new LinkedList<>();
        }
        addNode(sourceId, sinkIds);
    }

    private void addNode(String sourceId, List<String> sinkIds) {
        Node defaultNode = new Node(sourceId);
        Node source = nodes.getOrDefault(sourceId, defaultNode);
        Stream<Node> sinksStream = sinkIds.stream()
                .map(id -> nodes.getOrDefault(id, new Node(id)));
        List<Node> sinks = sinksStream.collect(Collectors.toList());
        source.addDependencies(sinks);
        nodes.putIfAbsent(sourceId, source);
        sinks.forEach(node -> nodes.putIfAbsent(node.getId(), node));
    }

    private boolean isAcyclic() {
        List<Node> unexplored = new LinkedList<>();
        unexplored.addAll(nodes.values());
        while(!unexplored.isEmpty()) {
            Node toExplore = unexplored.remove(0);
            if(toExplore.isMarkedPerm()) {
                continue;
            }
            boolean hasCycle = explore(toExplore);
            if(hasCycle) {
                return false;
            }
        }
        return true;
    }

    private boolean explore(Node node) {
        if(node.isMarkedTemp()) {
            return true;
        }
        node.markTemp();
        for(Node next: node.getDependencies()) {
            boolean hasCycle = explore(next);
            if(hasCycle) {
                return true;
            }
        }
        node.unmarkTemp();
        node.markPerm();
        return false;
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public PriorityQueue<Node> getTopologicalOrder() {
        return topologicalOrder;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Node node: nodes.values()) {
            builder.append(node);
            builder.append(" (indegree = ");
            builder.append(node.getIndegree());
            builder.append(") (dependencies = ");
            builder.append(node.getDependencies());
            builder.append(")\n");
        }
        builder.append("Topological order: ");
        builder.append(topologicalOrder);
        builder.append("\n");
        return builder.toString();
    }

    private Set<Node> getSources() {
        return nodes.values().stream()
                .filter(node -> node.getIndegree() == 0)
                .collect(Collectors.toSet());
    }

    private void runAll() {
        getSources().forEach(Node::requestStart);
    }
}
