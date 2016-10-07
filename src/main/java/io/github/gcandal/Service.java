package io.github.gcandal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

/**
 * Abstract Service class providing the
 * basic dependency functionality, meant
 * to be extended by classes that
 * implement work to be done
 */
abstract class Service extends Thread {
    private Set<Service> dependencies = new HashSet<>();
    /**
     * Services which depend on this service
     * and are running, meaning that this service
     * should only stop after they also do.
     */
    private Set<Service> runningParents = new CopyOnWriteArraySet<>();
    /**
     * Number of services who have this
     * one as dependency
     */
    private int indegree = 0;
    /**
     * Auxiliary variable sed by the DFS
     * topological sort algorithm
     * (see {@link ServiceManager#isAcyclic()}).
     */
    private boolean markedPerm = false;
    /**
     * Auxiliary variable sed by the DFS
     * topological sort algorithm
     * (see {@link ServiceManager#isAcyclic()}).
     */
    private boolean markedTemp = false;
    /**
     * Latch that controls if all the
     * dependencies have started.
     */
    private CountDownLatch startLatch;
    /**
     * References to the latches of the
     * services that depend on this one,
     * so they can be notified once this
     * service is running.
     */
    private Set<CountDownLatch> parentLatches = new HashSet<>();
    /**
     * Marks that the work being done
     * should be interrupted.
     */
    volatile boolean terminate = false;
    /**
     * Identification name of the service.
     */
    String id;

    Service() {
        this.setUncaughtExceptionHandler(new ServiceExceptionHandler());
    }

    /**
     * Sets {@link Service#dependencies}.
     * @param dependencies Services that this service depends on
     */
    final void addDependencies(List<Service> dependencies) {
        this.dependencies.addAll(dependencies);
        dependencies.forEach(Service::increaseIndegree);
    }

    /**
     * Increments {@link Service#indegree}.
     */
    private void increaseIndegree() {
        indegree += 1;
    }

    /**
     * Returns {@link Service#indegree}.
     * @return {@link Service#indegree}
     */
    final int getIndegree() {
        return indegree;
    }

    /**
     * Returns {@link Service#markedTemp}.
     * @return {@link Service#markedTemp}
     */
    final boolean isMarkedTemp() {
        return markedTemp;
    }


    /**
     * Sets {@link Service#markedTemp} to true.
     */
    final void markTemp() {
        markedTemp = true;
    }

    /**
     * Sets {@link Service#markedTemp} to false.
     */
    final void unmarkTemp() {
        markedTemp = false;
    }

    /**
     * Returns {@link Service#markedPerm}.
     * @return {@link Service#markedPerm}
     */
    final boolean isMarkedPerm() {
        return markedPerm;
    }

    /**
     * Sets {@link Service#markedPerm} to true.
     */
    final void markPerm() {
        markedPerm = true;
    }

    /**
     * Returns {@link Service#dependencies}.
     * @return {@link Service#dependencies}
     */
    final Set<Service> getDependencies() {
        return dependencies;
    }

    /**
     *
     * @return {@link Service#id}
     */
    final String getServiceId() {
        return id;
    }

    /**
     * The string representation of the Service.
     * @return The string representation of the Service.
     */
    public String toString() {
        return getServiceId();
    }

    /**
     * When the dependencies are all defined,
     * initiates the {@link Service#startLatch}
     * with the proper count.
     */
    final void initiateStartLatch() {
        startLatch = new CountDownLatch(dependencies.size());
    }

    /**
     * Adds a latch of a Service that depends on this one.
     * @param parentLatch Latch of a Service that depends on this one.
     */
    final void addParentLatch(CountDownLatch parentLatch) {
        parentLatches.add(parentLatch);
    }

    /**
     * Returns latches of Services that depend on this one.
     * @return Latches of Services that depend on this one.
     */
    final Set<CountDownLatch> getParentLatches() {
        return parentLatches;
    }

    /**
     * Returns this service's {@link Service#startLatch}.
     * @return This service's {@link Service#startLatch}.
     */
    final CountDownLatch getStartLatch() {
        return startLatch;
    }

    /**
     * Try to gracefully stop this service. This means
     * waiting for all of his parents (the services
     * that are dependent on it) stop running; when
     * that happens, indicate that the work should
     * stop executing by setting the {@link Service#terminate}
     * flag.
     */
    void requestStop() {
        log("Stopping");
        log("Waiting for parents to stop: " + runningParents);
        runningParents.forEach(Service::requestStop);
        try {
            while(runningParents.size() > 0) {
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            log("Interrupted while trying to stop, retrying...");
            requestStop();
            return;
        }
        terminate = true;
        log("Stopped");
        dependencies.forEach(children -> children.notifyStopped(this));
    }

    /**
     * Start the service if it is not running
     * already.
     * @param parent A service which depends on the one being started.
     */
    private void tryStart(Service parent) {
        runningParents.add(parent);
        synchronized (this) {
            if(!isAlive()) {
                terminate = false;
                start();
            }
        }
    }

    /**
     * Start the service if it is not running
     * already.
     */
    void tryStart() {
        synchronized (this) {
            if(!isAlive()) {
                start();
            }
        }
    }

    /**
     * Notify this service that one of its
     * parents has stopped, indicating
     * that there is one less running service that
     * depends upon this one.
     * @param parent The service depending on this one which has stopped.
     */
    private void notifyStopped(Service parent) {
        runningParents.remove(parent);
        synchronized (this) {
            notify();
        }
    }

    /**
     * Wait for the dependencies to start running. If an
     * interrupt is received while waiting, initiate the
     * stopping procedure.
     */
    private void waitDependencies() {
        log("Waiting for dependencies to start: " + dependencies);
        dependencies.forEach(service -> service.tryStart(this));
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            log("Interrupted while trying to start");
            requestStop();
        }
        parentLatches.forEach(CountDownLatch::countDown);
    }

    /**
     * Service's initiation procedure.
     */
    @Override
    public void run() {
        log("Starting");
        waitDependencies();
        log("Started");
        doWork();
    }

    /**
     * Entry point for different Service implementations.
     * This function should either be non-blocking or
     * periodically check the {@link Service#terminate} flag.
     */
    abstract void doWork();

    /**
     * Print a message prepended by the Service's ID
     * and Thread name.
     * @param message The message being printed.
     */
    void log(String message) {
        System.out.println("Service[" + id + "]-" + getName() + ": " + message);
    }
}
