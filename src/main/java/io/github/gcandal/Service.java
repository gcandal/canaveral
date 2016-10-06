package io.github.gcandal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

abstract class Service extends Thread {
    private Set<Service> children = new HashSet<>();
    private Set<Service> runningParents = new CopyOnWriteArraySet<>();
    private int indegree = 0;
    private boolean markedPerm = false;
    private boolean markedTemp = false;
    private CountDownLatch startLatch;
    private Set<CountDownLatch> parentLatches = new HashSet<>();
    Logger LOGGER = Logger.getLogger(ServiceManager.class.getName());
    volatile boolean terminate = false;
    String id;

    Service() {
        this.setUncaughtExceptionHandler(new ServiceExceptionHandler());
    }

    final void addDependencies(List<Service> dependencies) {
        children.addAll(dependencies);
        dependencies.forEach(Service::increaseIndegree);
    }

    private void increaseIndegree() {
        indegree += 1;
    }

    final int getIndegree() {
        return indegree;
    }

    final boolean isMarkedTemp() {
        return markedTemp;
    }

    final boolean isMarkedPerm() {
        return markedPerm;
    }

    final void markTemp() {
        markedTemp = true;
    }

    final void unmarkTemp() {
        markedTemp = false;
    }

    final void markPerm() {
        markedPerm = true;
    }

    final Set<Service> getDependencies() {
        return children;
    }

    final String getServiceId() {
        return id;
    }

    public String toString() {
        return getServiceId();
    }

    final void initiateLatches() {
        startLatch = new CountDownLatch(children.size());
    }

    final void addParentLatch(CountDownLatch parentLatch) {
        parentLatches.add(parentLatch);
    }

    final Set<CountDownLatch> getParentLatches() {
        return parentLatches;
    }

    final CountDownLatch getStartLatch() {
        return startLatch;
    }

    void requestStop() {
        LOGGER.info("Service[" + id + "]: " + "Stopping");
        LOGGER.info("Service[" + id + "]: " + "Waiting for parents to stop: " + runningParents);
        terminate = true;
        runningParents.forEach(Service::requestStop);
        try {
            while(runningParents.size() > 0) {
                synchronized (this) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Service[" + id + "]: " + "Interrupted while trying to stop, retrying...");
            requestStop();
            return;
        }
        LOGGER.info("Service[" + id + "]: " + "Stopped");
        children.forEach(children -> children.notifyStopped(this));
    }

    private void tryStart(Service parent) {
        runningParents.add(parent);
        synchronized (this) {
            if(!isAlive()) {
                start();
            }
        }
    }

    private void notifyStopped(Service parent) {
        runningParents.remove(parent);
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void run() {
        LOGGER.info("Service[" + id + "]: " + "Starting");
        LOGGER.info("Service[" + id + "]: " + "Waiting for children to start: " + children);
        children.forEach(service -> service.tryStart(this));
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            LOGGER.info("Service[" + id + "]: " + "Interrupted while trying to start");
            requestStop();
            return;
        }
        LOGGER.info("Service[" + id + "]: " + "Started");
        parentLatches.forEach(CountDownLatch::countDown);

        doWork();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    abstract void doWork();
}
