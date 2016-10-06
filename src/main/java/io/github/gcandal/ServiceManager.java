package io.github.gcandal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceManager implements Runnable {
    final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile boolean terminate = false;
    private static final Logger LOGGER = Logger.getLogger(ServiceManager.class.getName());
    private Map<String, Service> services = new HashMap<>();
    private PriorityQueue<Service> topologicalOrder = new PriorityQueue<>(new ServiceTopologicalComparator());

    ServiceManager(String fileName) throws IOException, RuntimeException {
        LOGGER.info("Initializing Service Manager...");
        LOGGER.info("Reading dependencies from " + fileName + "...");
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(this::addServices);
        }
        LOGGER.info("Checking for cycles in dependencies...");
        if(!isAcyclic()) {
            throw new RuntimeException("Dependencies read from " + fileName + " are cyclic.");
        }
        services.values().forEach(service -> topologicalOrder.add(service));
        LOGGER.info("Initiating latches...");
        initiateLatches();
    }

    private void initiateLatches() {
        services.values().forEach(Service::initiateLatches);
        services.values().forEach(
                parent -> parent.getDependencies()
                    .forEach(child -> child.addParentLatch(parent.getStartLatch()))
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
        Service defaultService = new SleepingService(parentId);
        Service source = services.getOrDefault(parentId, defaultService);
        Stream<Service> sinksStream = childrenIds.stream()
                .map(id -> services.getOrDefault(id, new SleepingService(id)));
        List<Service> sinks = sinksStream.collect(Collectors.toList());
        source.addDependencies(sinks);
        services.putIfAbsent(parentId, source);
        sinks.forEach(service -> services.putIfAbsent(service.getServiceId(), service));
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
        LOGGER.info("Starting all services...");
        getSources().forEach(Service::start);
    }

    private void runService(String serviceId) {
        LOGGER.info("Starting service" + serviceId + " ...");
        Service service = services.get(serviceId);
        if(service == null) {
            LOGGER.warning("Service " + serviceId + " doesn't exist.");
            return;
        }
        service.start();
    }

    private void stopService(String serviceId) {
        LOGGER.info("Stopping service" + serviceId + " ...");
        Service service = services.get(serviceId);
        if(service == null) {
            LOGGER.warning("Service " + serviceId + " doesn't exist.");
            return;
        }
        service.interrupt();
    }

    private void stopAll() {
        Set<Service> sinks = getSinks();
        LOGGER.info("Stopping services: " + sinks);
        sinks.forEach(Service::interrupt);
        LOGGER.info("All running services stopped");
    }

    private void terminate() {
        LOGGER.info("Waiting for running services before terminating...");
        for(Service service: services.values()) {
            try {
                service.join();
            } catch (InterruptedException e) {
                LOGGER.warning("Termination was forced before all services could be stopped");
                return;
            }
        }
        terminate = true;
        LOGGER.info("Terminated");
    }

    @Override
    public void run() {
        LOGGER.info("Listening to messages...");
        while(!terminate) {
            try {
                String message = queue.take();
                processMessage(message);
            } catch (InterruptedException e) {
                LOGGER.warning("Event loop interrupted by: " + e);
                terminate();
            }
        }
    }

    private void processMessage(String message) {
        String splittedMessage[] = message.split(" ", 2);
        String command = splittedMessage[0];
        String serviceId = splittedMessage.length > 1? splittedMessage[1] : "";
        switch (command) {
            case "START-ALL":
                runAll();
                break;
            case "STOP-ALL":
                stopAll();
                break;
            case "START-SERVICE":
                runService(serviceId);
                break;
            case "STOP-SERVICE":
                stopService(serviceId);
                break;
            case "EXIT":
                terminate();
                break;
            default:
                LOGGER.warning("Unknown command " + command + " in message " + message);
                break;
        }
    }
}
