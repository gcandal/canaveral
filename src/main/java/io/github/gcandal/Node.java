package io.github.gcandal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Node  {
    private String id;
    private Set<Node> dependencyList = new HashSet<>();
    private int indegree = 0;
    private boolean markedPerm = false;
    private boolean markedTemp = false;
    private CountDownLatch ownLatch;
    private Set<CountDownLatch> parentLatches = new HashSet<>();

    public Node(String id) {
        this.id = id;
    }

    public void addDependencies(List<Node> dependencies) {
        dependencyList.addAll(dependencies);
        dependencies.forEach(Node::increaseIndegree);
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

    public Set<Node> getDependencies() {
        return dependencyList;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return getId();
    }

    public void initiateOwnLatch() {
        ownLatch = new CountDownLatch(dependencyList.size());
    }

    public void addParentLatch(CountDownLatch parentLatch) {
        parentLatches.add(parentLatch);
    }

    public Set<CountDownLatch> getParentLatches() {
        return parentLatches;
    }

    public CountDownLatch getOwnLatch() {
        return ownLatch;
    }

    public void requestStart() {
        try {
            ownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parentLatches.forEach(CountDownLatch::countDown);
    }
}
