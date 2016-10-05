package io.github.gcandal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceManager {
    private Map<String, Service> services = new HashMap<>();
    private PriorityQueue<Service> topologicalOrder = new PriorityQueue<>(new ServiceTopologicalComparator());

    ServiceManager(String fileName) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(this::addServices);
        }
        if(!isAcyclic()) {
            throw new RuntimeException();
        }
        services.values().forEach(service -> topologicalOrder.add(service));
        initiateLatches();
    }

    private void initiateLatches() {
        services.values().forEach(Service::initiateLatches);
        services.values().forEach(
                parent -> parent.getDependencies().forEach(
                        child -> {
                            child.addParentLatch(parent.getStartLatch());
                            parent.addChildLatch(child.getStopLatch());
                        }
                )
        );
    }

    private void addServices(String line) {
        List<String> serviceIds = Arrays.asList(line.split(" "));
        String parentId = serviceIds.get(0);
        Integer nrChildren = serviceIds.size() - 1;
        List<String> childrenIds;
        if(nrChildren > 0){
            childrenIds = serviceIds.subList(1, nrChildren + 1);
        } else {
            childrenIds = new LinkedList<>();
        }
        addService(parentId, childrenIds);
    }

    private void addService(String parentId, List<String> childrenIds) {
        Service defaultService = new Service(parentId);
        Service source = services.getOrDefault(parentId, defaultService);
        Stream<Service> sinksStream = childrenIds.stream()
                .map(id -> services.getOrDefault(id, new Service(id)));
        List<Service> sinks = sinksStream.collect(Collectors.toList());
        source.addDependencies(sinks);
        services.putIfAbsent(parentId, source);
        sinks.forEach(service -> services.putIfAbsent(service.getId(), service));
    }

    private boolean isAcyclic() {
        List<Service> unexplored = new LinkedList<>();
        unexplored.addAll(services.values());
        while(!unexplored.isEmpty()) {
            Service toExplore = unexplored.remove(0);
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

    private boolean explore(Service node) {
        if(node.isMarkedTemp()) {
            return true;
        }
        node.markTemp();
        for(Service next: node.getDependencies()) {
            boolean hasCycle = explore(next);
            if(hasCycle) {
                return true;
            }
        }
        node.unmarkTemp();
        node.markPerm();
        return false;
    }

    public Service getService(String id) {
        return services.get(id);
    }

    public PriorityQueue<Service> getTopologicalOrder() {
        return topologicalOrder;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Service service : services.values()) {
            builder.append(service);
            builder.append(" (indegree = ");
            builder.append(service.getIndegree());
            builder.append(") (dependencies = ");
            builder.append(service.getDependencies());
            builder.append(")\n");
        }
        builder.append("Topological order: ");
        builder.append(topologicalOrder);
        builder.append("\n");
        return builder.toString();
    }

    private Set<Service> getSources() {
        return services.values().stream()
                .filter(service -> service.getIndegree() == 0)
                .collect(Collectors.toSet());
    }

    private Set<Service> getSinks() {
        return services.values().stream()
                .filter(service -> service.getDependencies().size() == 0)
                .collect(Collectors.toSet());
    }

    private void runAll() {
        getSources().forEach(Service::requestStart);
    }

    private void stopAll() {
        getSinks().forEach(Service::requestStop);
    }
}
