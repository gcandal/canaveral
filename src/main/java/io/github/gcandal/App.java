package io.github.gcandal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

/**
 * Entry point: initiates a {@link ServiceManager} from
 * a file whose path is passed via argument and
 * subsequently redirects messages from STDIN
 * to it.
 */
public class App {
    public static void main(String[] args) {
        String filename = "services.txt";
        ServiceManager serviceManager = null;

        try {
            serviceManager = new ServiceManager(filename);
        } catch (IOException e) {
            System.err.println("Couldn't read file " + filename + " due to: " + e);
        } catch (RuntimeException e) {
            System.err.println("Something went wrong initializing serviceManager: " + e);
        }

        if(serviceManager == null) {
            return;
        }

        BlockingQueue<String> queue = serviceManager.queue;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        new Thread(serviceManager).start();

        try {
            while (true) {
                final String line = reader.readLine();

                if (line == null || line.equals("EXIT")) {
                    queue.put("EXIT");
                    break;
                }

                queue.put(line);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
