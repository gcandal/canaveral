package io.github.gcandal;

import java.util.concurrent.CountDownLatch;

public class Service {
    private CountDownLatch ownLatch;
    public Service(CountDownLatch ownLatch) {
        this.ownLatch = ownLatch;
    }
}
