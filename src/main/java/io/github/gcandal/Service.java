package io.github.gcandal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Service {
    private String id;
    private Set<Service> childrenList = new HashSet<>();
    private int indegree = 0;
    private boolean markedPerm = false;
    private boolean markedTemp = false;
    private CountDownLatch startLatch;
    private CountDownLatch stopLatch;
    private Set<CountDownLatch> parentLatches = new HashSet<>();
    private Set<CountDownLatch> childrenLatches = new HashSet<>();

    public Service(String id) {
        this.id = id;
    }

    public void addDependencies(List<Service> dependencies) {
        childrenList.addAll(dependencies);
        dependencies.forEach(Service::increaseIndegree);
    }

    public void increaseIndegree() {
        indegree += 1;
    }

    public int getIndegree() {
        return indegree;
    }

    public boolean isMarkedTemp() {
        return markedTemp;
    }

    public boolean isMarkedPerm() {
        return markedPerm;
    }

    public void markTemp() {
        markedTemp = true;
    }

    public void unmarkTemp() {
        markedTemp = false;
    }

    public void markPerm() {
        markedPerm = true;
    }

    public Set<Service> getDependencies() {
        return childrenList;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return getId();
    }

    public void initiateLatches() {
        startLatch = new CountDownLatch(childrenList.size());
        stopLatch = new CountDownLatch(indegree);
    }

    public void addParentLatch(CountDownLatch parentLatch) {
        parentLatches.add(parentLatch);
    }

    public void addChildLatch(CountDownLatch childLatch) {
        childrenLatches.add(childLatch);
    }

    public Set<CountDownLatch> getParentLatches() {
        return parentLatches;
    }

    public Set<CountDownLatch> getChildrenLatches() {
        return childrenLatches;
    }

    public CountDownLatch getStartLatch() {
        return startLatch;
    }

    public CountDownLatch getStopLatch() {
        return stopLatch;
    }

    public void requestStart() {
        childrenList.forEach(Service::requestStart);
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parentLatches.forEach(CountDownLatch::countDown);
    }

    public void requestStop() {
        childrenList.forEach(Service::requestStart);
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parentLatches.forEach(CountDownLatch::countDown);
    }
}
