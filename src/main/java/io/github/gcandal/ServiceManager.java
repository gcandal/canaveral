package io.github.gcandal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wires dependent {@link Service}s together
 * and acts as a command dispatcher to start
 * and stop them.
 */
class ServiceManager implements Runnable {
    /**
     * Queue to be used to pass commands
     * to this manager.
     */
    final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    /**
     * Marks that the manager should stop
     * as soon as possible.
     */
    private volatile boolean terminate = false;
    /**
     * Mapping of Service IDs to their reference.
     */
    private Map<String, Service> services = new HashMap<>();

    /**
     * Reads a file where Service dependencies are specified,
     * with the format of 'ServiceA (ServiceB)*', meaning that
     * 'ServiceA' depends on 'ServiceB'.
     * @param filePath The dependency filepath.
     * @throws IOException When there was a problem reading the dependency file.
     * @throws RuntimeException When the dependency graph is cyclic.
     */
    ServiceManager(String filePath) throws IOException, RuntimeException {
        log("Initializing Service Manager...");
        log("Reading dependencies from " + filePath + "...");
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(this::addServices);
        }
        log("Checking for cycles in dependencies...");
        if(!isAcyclic()) {
            throw new RuntimeException("Dependencies read from " + filePath + " are cyclic.");
        }
        log("Initiating latches...");
        initiateLatches();
    }

    /**
     * Initiate all {@link Service#startLatch} and pass
     * the references from parents to children.
     */
    private void initiateLatches() {
        services.values().forEach(Service::initiateStartLatch);
        services.values().forEach(
                parent -> parent.getDependencies()
                    .forEach(child -> child.addParentLatch(parent.getStartLatch()))
        );
    }

    /**
     * Parses a dependency file line.
     * @param line A dependency file line.
     */
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

    /**
     * Instantiates several {@link Service} and sets the dependencies
     * between them.
     * @param parentId The ID of the service that depends on childrenIDs
     * @param childrenIds The IDs of the services which parentId depends on.
     */
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

    /**
     * Checks if the graph is acyclic.
     * @return True if the graph is acyclic.
     */
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

    /**
     * Checks if a node is part of a cycle.
     * @param node Node to be explored.
     * @return True if the node was already seen in the current exploration.
     */
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

    /**
     * Returns {@link Service} with ID 'id'.
     * @param id ID of the {@link Service}.
     * @return {@link Service} with ID 'id'.
     */
    Service getService(String id) {
        return services.get(id);
    }

    /**
     * Returns the string representation of this {@link ServiceManager}.
     * @return The string representation of this {@link ServiceManager}.
     */
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
        return builder.toString();
    }

    /**
     * Returns {@link Service}s that no other {@link Service}
     * depends on.
     * @return {@link Service}s that no other {@link Service}
     * depends on.
     */
    private Set<Service> getSources() {
        return services.values().stream()
                .filter(service -> service.getIndegree() == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Returns {@link Service}s that have no dependencies.
     * @return {@link Service}s that have no dependencies.
     */
    private Set<Service> getSinks() {
        return services.values().stream()
                .filter(service -> service.getDependencies().size() == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Start all {@link Service}s.
     */
    private void runAll() {
        log("Starting all services...");
        getSources().forEach(Service::tryStart);
    }

    /**
     * Starts a {@link Service} with a given ID.
     * @param serviceId The ID of the {@link Service} being started.
     */
    private void runService(String serviceId) {
        log("Starting service" + serviceId + " ...");
        Service service = services.get(serviceId);
        if(service == null) {
            log("Service " + serviceId + " doesn't exist.");
            return;
        }
        service.start();
    }

    /**
     * Request all {@link Service}s to stop.
     */
    private void stopAll() {
        Set<Service> sinks = getSinks();
        log("Stopping services: " + sinks);
        sinks.forEach(Service::interrupt);
        log("All running services stopped");
    }

    /**
     * Requests the stopping of a {@link Service} with a given ID.
     * @param serviceId The ID of the {@link Service} being stopped.
     */
    private void stopService(String serviceId) {
        log("Stopping service" + serviceId + " ...");
        Service service = services.get(serviceId);
        if(service == null) {
            log("Service " + serviceId + " doesn't exist.");
            return;
        }
        service.interrupt();
    }

    /**
     * Terminates the {@link ServiceManager} as soon as possible.
     * This means requesting all {@link Service}s to stop and wait
     * for that to happen before terminating itself.
     */
    private void terminate() {
        stopAll();
        log("Waiting for running services before terminating...");
        for(Service service: services.values()) {
            try {
                service.join();
            } catch (InterruptedException e) {
                log("Termination was forced before all services could be stopped");
                return;
            }
        }
        terminate = true;
        log("Terminated");
    }

    /**
     * While the {@link ServiceManager} is running,
     * reads messages from the {@link ServiceManager#queue}
     * and acts upon them.
     */
    @Override
    public void run() {
        log("Listening to messages...");
        while(!terminate) {
            try {
                String message = queue.take();
                processMessage(message);
            } catch (InterruptedException e) {
                log("Event loop interrupted by: " + e);
                terminate();
            }
        }
    }

    /**
     * Mapping from messages received in the {@link ServiceManager#queue}
     * and actions to take.
     * @param message The message being received.
     */
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
                log("Unknown command " + command + " in message " + message);
                break;
        }
    }

    /**
     * Print a message prepended by the Thread name.
     * @param message The message being printed.
     */
    private void log(String message) {
        System.out.println("ServiceManager-" + Thread.currentThread().getName() + ": " + message);
    }
}
