package io.github.gcandal;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Tests the {@link ServiceManager} class.
 */
public class ServiceManagerTest {
    /**
     * Checks general {@link ServiceManager} initialization,
     * mainly if the {@link Service}s are well connected
     * and possess correct references to each other.
     * @throws IOException When there was an error reading the file.
     */
    @Test
    public void testParents() throws IOException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        Service a = serviceManager.getService("a"),
            b = serviceManager.getService("b"),
            c = serviceManager.getService("c"),
            d = serviceManager.getService("d"),
            e = serviceManager.getService("e");

        Set<Service> expectedSources = new HashSet<>(),
                expectedSinks = new HashSet<>();
        expectedSources.add(d);
        expectedSources.add(e);
        expectedSinks.add(a);
        expectedSinks.add(e);

        assertThat(serviceManager.getSources(), is(expectedSources));
        assertThat(serviceManager.getSinks(), is(expectedSinks));

        assertEquals(2, a.getIndegree());
        assertEquals(1, b.getIndegree());
        assertEquals(1, c.getIndegree());
        assertEquals(0, d.getIndegree());
        assertEquals(0, e.getIndegree());
    }

    @Test
    public void testChildren() throws IOException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        Service a = serviceManager.getService("a"),
                b = serviceManager.getService("b"),
                c = serviceManager.getService("c"),
                d = serviceManager.getService("d"),
                e = serviceManager.getService("e");

        Set<Service> expectedDependencies = new HashSet<>();
        assertThat(a.getDependencies(), is(expectedDependencies));

        expectedDependencies.add(a);
        assertThat(b.getDependencies(), is(expectedDependencies));
        assertThat(c.getDependencies(), is(expectedDependencies));

        expectedDependencies.clear();
        expectedDependencies.add(b);
        expectedDependencies.add(c);
        assertThat(d.getDependencies(), is(expectedDependencies));

        expectedDependencies.clear();
        assertThat(e.getDependencies(), is(expectedDependencies));
    }

    /**
     * Checks if the {@link ServiceManager} detects dependency cicles.
     * @throws IOException When there was an error reading the file.
     */
    @Test(expected = RuntimeException.class)
    public void testCyclicServiceManager() throws IOException {
        new ServiceManager("cyclic_services.txt");
    }

    @Test
    public void testInvalidMessages() throws IOException, InterruptedException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        BlockingQueue<String> messageQueue = serviceManager.queue;
        messageQueue.put("RESUME-SERVICE non-existing");
        messageQueue.put("I'M NOT A VALID MESSAGE");
        messageQueue.put("ME NEITHER");
    }

    @Test
    public void testToString() throws IOException {
        ServiceManager serviceManager = new ServiceManager("services.txt");
        String option1 = "a (indegree = 2) (dependencies = [])\n" +
                "b (indegree = 1) (dependencies = [a])\n" +
                "c (indegree = 1) (dependencies = [a])\n" +
                "d (indegree = 0) (dependencies = [c, b])\n" +
                "e (indegree = 0) (dependencies = [])\n",
                option2 = "a (indegree = 2) (dependencies = [])\n" +
                    "b (indegree = 1) (dependencies = [a])\n" +
                    "c (indegree = 1) (dependencies = [a])\n" +
                    "d (indegree = 0) (dependencies = [b, c])\n" +
                    "e (indegree = 0) (dependencies = [])\n";
        assertThat(serviceManager.toString(), anyOf(equalTo(option1), equalTo(option2)));
    }
}
