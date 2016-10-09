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
        messageQueue.put("RESUME-ALL");
        messageQueue.put("RESUME-ALL");
    }

    /**
     * Sees if stopping a service while it's starting causes no problems.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testInterruptStart() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        new Thread(serviceManager).start();

        messageQueue.put("RESUME-SERVICE d");
        messageQueue.put("STOP-SERVICE d");
    }

    /**
     * Sees if stopping timeout is working properly.
     * @throws IOException When there was an error reading the file.
     * @throws InterruptedException When the test was interrupted during sleeps.
     */
    @Test
    public void testTimeout() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        Service a = serviceManager.getService("a"),
            b = serviceManager.getService("b");
        new Thread(serviceManager).start();

        b.isBad = true;
        a.setTimeout(1);

        messageQueue.put("RESUME-SERVICE b");
        messageQueue.put("STOP-SERVICE a");
        waitPropagation();

        assertEquals(Service.ServiceState.WAITING_RUN, a.state);
        assertEquals(Service.ServiceState.WAITING_STOP, b.state);
    }

    /**
     * Tests the 'RESUME-ALL' and 'STOP-ALL' commands.
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

        messageQueue.put("RESUME-SERVICE b");
        waitPropagation();

        // a is a dependency of b, so it should be running
        assertEquals(Service.ServiceState.RUNNING, a.state);
        assertEquals(Service.ServiceState.RUNNING, b.state);

        messageQueue.put("RESUME-SERVICE d");
        waitPropagation();

        // Dependencies of d should be running
        assertEquals(Service.ServiceState.RUNNING, a.state);
        assertEquals(Service.ServiceState.RUNNING, b.state);
        assertEquals(Service.ServiceState.RUNNING, c.state);
        assertEquals(Service.ServiceState.RUNNING, d.state);
        assertEquals(Service.ServiceState.WAITING_RUN, e.state);


        messageQueue.put("STOP-SERVICE a");
        waitPropagation();

        // b, c and d ultimately depend on a,
        // so they should've stopped
        assertEquals(Service.ServiceState.WAITING_RUN, a.state);
        assertEquals(Service.ServiceState.WAITING_RUN, b.state);
        assertEquals(Service.ServiceState.WAITING_RUN, c.state);
        assertEquals(Service.ServiceState.WAITING_RUN, d.state);
        assertEquals(Service.ServiceState.WAITING_RUN, e.state);
    }

    /**
     * Tests the 'RESUME-ALL' and 'STOP-ALL' commands.
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

        messageQueue.put("RESUME-ALL");
        waitPropagation();

        assertEquals(Service.ServiceState.RUNNING, a.state);
        assertEquals(Service.ServiceState.RUNNING, b.state);
        assertEquals(Service.ServiceState.RUNNING, c.state);
        assertEquals(Service.ServiceState.RUNNING, d.state);
        assertEquals(Service.ServiceState.RUNNING, e.state);


        messageQueue.put("STOP-ALL");
        waitPropagation();

        assertEquals(Service.ServiceState.WAITING_RUN, a.state);
        assertEquals(Service.ServiceState.WAITING_RUN, b.state);
        assertEquals(Service.ServiceState.WAITING_RUN, c.state);
        assertEquals(Service.ServiceState.WAITING_RUN, d.state);
        assertEquals(Service.ServiceState.WAITING_RUN, e.state);
    }

    /**
     * Tests the 'RESUME-ALL' and 'STOP-ALL' commands.
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

        messageQueue.put("RESUME-ALL");
        waitPropagation();

        assertEquals(Service.ServiceState.RUNNING, a.state);
        assertEquals(Service.ServiceState.RUNNING, b.state);
        assertEquals(Service.ServiceState.RUNNING, c.state);
        assertEquals(Service.ServiceState.RUNNING, d.state);
        assertEquals(Service.ServiceState.RUNNING, e.state);

        messageQueue.put("EXIT");
        waitPropagation();

        assertEquals(Service.ServiceState.TERMINATED, a.state);
        assertEquals(Service.ServiceState.TERMINATED, b.state);
        assertEquals(Service.ServiceState.TERMINATED, c.state);
        assertEquals(Service.ServiceState.TERMINATED, d.state);
        assertEquals(Service.ServiceState.TERMINATED, e.state);
    }

    private void waitPropagation() throws InterruptedException {
        Thread.sleep(2000);
    }
}