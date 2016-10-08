package io.github.gcandal;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Tests the {@link ServiceManager} class.
 */
public class ServiceManagerTest
{
    /**
     * Checks general {@link ServiceManager} initialization,
     * mainly if the {@link Service}s are well connected
     * and possess correct references to each other.
     * @throws IOException When there was an error reading the file.
     */
    @Test
    public void testInitServiceManager() throws IOException {
        ServiceManager serviceManager = new ServiceManager("services.txt");

        Service a = serviceManager.getService("a"),
            b = serviceManager.getService("b"),
            c = serviceManager.getService("c"),
            d = serviceManager.getService("d"),
            e = serviceManager.getService("e");

        /*
        Check if the number of parents
        is properly set
        */
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

        // Check if the nodes are connected correctly
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

        /*
        Check if the initialization latches
        are shared properly among parent-child pairs
         */
        assertThat(a.getParentLatches(), hasItems(b.getStartLatch(), c.getStartLatch()));
        assertThat(b.getParentLatches(), hasItems(d.getStartLatch()));
        assertThat(c.getParentLatches(), hasItems(d.getStartLatch()));
        assertEquals(0, d.getParentLatches().size());
        assertEquals(0, e.getParentLatches().size());
    }

    /**
     * Checks if the {@link ServiceManager} detects dependency cicles.
     * @throws IOException When there was an error reading the file.
     */
    @Test(expected = RuntimeException.class)
    public void testCyclicServiceManager() throws IOException {
        new ServiceManager("cyclic_services.txt");
    }
}
