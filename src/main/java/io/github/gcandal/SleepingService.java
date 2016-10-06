package io.github.gcandal;

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
                LOGGER.info("Service[" + id + "]: " + " Working...");
            }
            LOGGER.info("Service[" + id + "]: " + " Finished working.");
        } catch (InterruptedException e) {
            requestStop();
            LOGGER.info("Service[" + id + "]: " + " Got interrupted while working.");
        }
    }
}
