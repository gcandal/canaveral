package io.github.gcandal;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Tests the interaction between the {@link ServiceManager} and {@link Service} classes.
 */
public class SystemTest {
    /**
     * Tests the resiliance of the system when receiving senseless commands
     * such as stopping before starting or starting twice.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testDuplicateCommands() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        new Thread(serviceManager).start();
        messageQueue.put("STOP-ALL");
        messageQueue.put("START-ALL");
        messageQueue.put("START-ALL");
    }

    /**
     * Sees if interrupting a service while it's starting causes no problems.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testInterruptStart() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        Service a = serviceManager.getService("a");
        new Thread(serviceManager).start();

        assertEquals(false, a.isAlive());
        messageQueue.put("START-SERVICE a");
        messageQueue.put("STOP-SERVICE a");
        assertEquals(false, a.isAlive());
    }

    /**
     * Tests the 'START-ALL' and 'STOP-ALL' commands.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testStartStopSpecific() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        Service a = serviceManager.getService("a"),
                b = serviceManager.getService("b"),
                c = serviceManager.getService("c"),
                d = serviceManager.getService("d"),
                e = serviceManager.getService("e");
        new Thread(serviceManager).start();

        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());

        messageQueue.put("START-SERVICE b");
        Thread.sleep(1000);

        // a is a dependency of b, so it should be running
        assertEquals(true, a.isAlive());
        assertEquals(true, b.isAlive());

        messageQueue.put("START-SERVICE d");
        Thread.sleep(1000);

        // Dependencies of d should be running
        assertEquals(true, a.isAlive());
        assertEquals(true, b.isAlive());
        assertEquals(true, c.isAlive());
        assertEquals(true, d.isAlive());
        assertEquals(false, e.isAlive());


        messageQueue.put("STOP-SERVICE a");
        /*
         Must wait for the maximum time between SleepingService's stop
         flag reads (10s)
          */
        Thread.sleep(10000);

        // b, c and d ultimately depend on a,
        // so they should've stopped
        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());
    }

    /**
     * Tests the 'START-ALL' and 'STOP-ALL' commands.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testStartStopAll() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        Service a = serviceManager.getService("a"),
                b = serviceManager.getService("b"),
                c = serviceManager.getService("c"),
                d = serviceManager.getService("d"),
                e = serviceManager.getService("e");
        new Thread(serviceManager).start();

        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());

        messageQueue.put("START-ALL");
        Thread.sleep(1000);

        assertEquals(true, a.isAlive());
        assertEquals(true, b.isAlive());
        assertEquals(true, c.isAlive());
        assertEquals(true, d.isAlive());
        assertEquals(true, e.isAlive());


        messageQueue.put("STOP-ALL");
        /*
         Must wait for the maximum time between SleepingService's stop
         flag reads (10s)
          */
        Thread.sleep(10000);

        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());
    }

    /**
     * Tests the 'START-ALL' and 'STOP-ALL' commands.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testExit() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        Service a = serviceManager.getService("a"),
                b = serviceManager.getService("b"),
                c = serviceManager.getService("c"),
                d = serviceManager.getService("d"),
                e = serviceManager.getService("e");
        new Thread(serviceManager).start();

        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());

        messageQueue.put("START-ALL");
        Thread.sleep(1000);

        assertEquals(true, a.isAlive());
        assertEquals(true, b.isAlive());
        assertEquals(true, c.isAlive());
        assertEquals(true, d.isAlive());
        assertEquals(true, e.isAlive());


        messageQueue.put("EXIT");
        /*
         Must wait for the maximum time between SleepingService's stop
         flag reads (10s)
          */
        Thread.sleep(10000);

        assertEquals(false, a.isAlive());
        assertEquals(false, b.isAlive());
        assertEquals(false, c.isAlive());
        assertEquals(false, d.isAlive());
        assertEquals(false, e.isAlive());
    }
}