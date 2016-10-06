package io.github.gcandal;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class App
{
    private static final Logger LOGGER = Logger.getLogger(ServiceManager.class.getName());

    public static void main(String[] args) {
        String filename = "services.txt";
        ServiceManager serviceManager = null;
        try {
            serviceManager = new ServiceManager(filename);
        } catch (IOException e) {
            LOGGER.severe("Couldn't read file " + filename + " due to: " + e);
        } catch (RuntimeException e) {
            LOGGER.severe("Something went wrong initializing serviceManager: " + e);
        }
        if(serviceManager == null) {
            return;
        }
        BlockingQueue<String> queue = serviceManager.queue;
        new Thread(serviceManager).start();
        try {
            queue.put("START-ALL");
            Thread.sleep(1000);
            queue.put("STOP-SERVICE a");
            queue.put("EXIT");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
