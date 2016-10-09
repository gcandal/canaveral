package io.github.gcandal;

/**
 * Concrete {@link Service} that prints to STDOUT
 * in random intervals of 0 to 10 seconds.
 */
class SleepingService extends Service {
    SleepingService(String id) {
        super();
        this.id = id;
        setTimeout(1500);
    }

    @Override
    void doWork() throws InterruptedException {
        while(!stop || isBad) {
            Thread.sleep((long)(Math.random() * 1000));
            log("Working...");
        }
        log("Stopped working.");
    }
}
