package io.github.gcandal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Abstract Service class providing the
 * basic dependency functionality, meant
 * to be extended by classes that
 * implement work to be done
 */
abstract class Service extends Thread {
    /**
     * Services which this service depends on.
     */
    private Set<Service> dependencies = new HashSet<>();
    /**
     * Services which depend on this service.
     */
    private Set<Service> parents = new HashSet<>();
    /**
     * Services which this service depends
     * on and are already running. A service
     * must only start when this set matches
     * {@link Service#dependencies}.
     */
    private Set<Service> runningDependencies = new CopyOnWriteArraySet<>();
    /**
     * Services which depend on this service
     * and are running, meaning that this service
     * should only stop after they also do.
     */
    private Set<Service> runningParents = new CopyOnWriteArraySet<>();
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
     * The {@link Service} should stop
     * doing work.
     */
    volatile boolean stop = true;
    /**
     * The {@link Service} thread should stop.
     */
    private volatile boolean terminate = false;
    /**
     * The time in milliseconds to wait for parents to finish.
     */
    private long timeout = 0;
    /**
     * Identification name of the service.
     */
    String id;
    /**
     * Flag used for testing. Makes the Service ignore the
     * {@link Service#stop} flag.
     */
    boolean isBad = false;
    /**
     * The Service current state.
     */
    volatile ServiceState state = ServiceState.CREATED;

    /**
     * Possible states of a service.
     */
    enum ServiceState {
        CREATED, WAITING_RUN, RUNNING, WAITING_STOP, TERMINATED
    }

    Service() {
        this.setUncaughtExceptionHandler(new ServiceExceptionHandler());
    }

    /**
     * Sets {@link Service#dependencies}.
     * @param dependencies Services that this service depends on
     */
    final void addDependencies(List<Service> dependencies) {
        this.dependencies.addAll(dependencies);
        dependencies.forEach(dependency -> dependency.addParent(this));
    }

    /**
     * Registers a service has one that depends on this
     * one.
     * @param parent Service which depends on this one.
     */
    final void addParent(Service parent) {
        parents.add(parent);
    }

    /**
     * Returns the number of services which depend
     * upon this one.
     * @return The number of services which depend
     * upon this one.
     */
    final int getIndegree() {
        return parents.size();
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
     * Sets the time in milliseconds to wait for parents to finish.
     * @param timeout The new timeout value in milliseconds.
     */
    void setTimeout(long timeout) {
        if(timeout < 0) {
            throw new RuntimeException(buildMessage("Timeout value must be positive, not " + timeout));
        }
        this.timeout = timeout;
    }

    /**
     * Sets the values of the control flags.
     * @param stop New value of the {@link Service#stop} flag.
     * @param terminate New value of the {@link Service#terminate} flag.
     */
    private void setStop(boolean stop, boolean terminate) {
        this.stop = stop;
        this.terminate = terminate;
        synchronized (this) {
            notify();
        }
    }

    /**
     * Request this service to resume
     * doing work.
     */
    void requestResume() {
        log("Resuming");
        waitDependencies();
        setStop(false, terminate);
        state = ServiceState.RUNNING;
        log("Resumed.");
    }

    /**
     * Used by a parent service to request a dependency
     * to start doing work.
     * @param parent service which depends on this one.
     */
    private void requestResume(Service parent) {
        runningParents.add(parent);
        requestResume();
    }

    /**
     * Try to gracefully stop this service. This means
     * waiting for all of his parents (the services
     * that are dependent on it) stop running; when
     * that happens, indicate that the work should
     * stop executing by setting the {@link Service#stop}
     * flag.
     */
    private void requestStop(boolean terminate) {
        if(stop) {
            return;
        }
        log("Stopping");
        log("Waiting for parents to stop: " + runningParents);
        state = ServiceState.WAITING_STOP;
        runningParents.forEach(parent -> parent.requestStop(terminate));
        long timeoutExpires = System.currentTimeMillis() + timeout;
        try {
            synchronized (this) {
                while(runningParents.size() > 0) {
                    wait(timeout);
                    if (System.currentTimeMillis() >= timeoutExpires) {
                        log("Timeout while waiting for parents " + runningParents + " to stop.");
                        break;
                    } else {
                        log("Waiting for parents to stop: " + runningParents);
                    }
                }
            }
        } catch (InterruptedException e) {
            log("Timeout while waiting for parents " + runningParents + " to stop.");
            return;
        }
        setStop(true, terminate);
        log("Is able to stop");
    }

    /**
     * Try to gracefully terminate this service. This means
     * initiating the {@link Service#requestStop(boolean)}
     * protocol.
     */
    void requestStop() {
        requestStop(false);
    }

    /**
     * Try to gracefully terminate this service. This means
     * initiating the {@link Service#requestStop(boolean)}
     * protocol and setting the {@link Service#terminate}
     * flag.
     */
    void requestTerminate() {
        log("Trying to terminate...");
        requestStop(true);
    }

    /**
     * Notify this service that one of its
     * dependencies has started, indicating
     * that it could possibly start running.
     * @param dependency A dependency of this service.
     */
    private void notifyResumed(Service dependency) {
        synchronized (this) {
            runningDependencies.add(dependency);
            notify();
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
        synchronized (this) {
            runningParents.remove(parent);
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
        dependencies.forEach(dependency -> dependency.requestResume(this));
        try {
            synchronized (this) {
                while(!dependencies.equals(runningDependencies)) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            log("Interrupted while trying to start");
            requestStop();
        }
        parents.forEach(parent -> parent.notifyResumed(this));
    }

    /**
     * Service's work loop.
     */
    @Override
    public void run() {
        log("Waiting for resuming...");
        state = ServiceState.WAITING_RUN;

        try {
            synchronized (this) {
                while(stop) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            log("Got interrupted while stopped.");}

        try {
            doWork();
        } catch (InterruptedException e) {
            log("Got interrupted while working.");
        }

        dependencies.forEach(children -> children.notifyStopped(this));

        if(terminate) {
            log("Terminated");
            state = ServiceState.TERMINATED;
        } else {
            run();
        }
    }

    /**
     * Entry point for different Service implementations.
     * This function should either be non-blocking or
     * periodically check the {@link Service#stop} flag.
     * @throws InterruptedException When the Service is interrupted while working.
     */
    abstract void doWork() throws InterruptedException;

    /**
     * Print a message prepended by the Service's ID
     * and Thread name.
     * @param message The message being printed.
     */
    void log(String message) {
        System.out.println(buildMessage(message));
    }

    private String buildMessage(String message) {
        return "Service[" + id + "]-" + getName() + ": " + message;
    }
}
