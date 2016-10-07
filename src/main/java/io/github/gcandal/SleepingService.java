package io.github.gcandal;

/**
 * Concrete {@link Service} that prints to STDOUT
 * in random intervals of 0 to 10 seconds.
 */
class SleepingService extends Service {
    SleepingService(String id) {
        super();
        this.id = id;
    }

    @Override
    void doWork() {
        try {
            while(!terminate) {
                Thread.sleep((long)(Math.random() * 10000));
                log("Working...");
            }
            log("Finished working.");
        } catch (InterruptedException e) {
            log("Got interrupted while working.");
            requestStop();
        }
    }
}
