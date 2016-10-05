package io.github.gcandal;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.*;


public class ServiceManagerTest
{
    @Test
    public void testServiceManager() throws IOException {
        ServiceManager serviceManager = new ServiceManager("services.txt");

        Service a = serviceManager.getService("a"),
            b = serviceManager.getService("b"),
            c = serviceManager.getService("c"),
            d = serviceManager.getService("d"),
            e = serviceManager.getService("e");

        assertEquals(2, a.getIndegree());
        assertEquals(1, b.getIndegree());
        assertEquals(1, c.getIndegree());
        assertEquals(0, d.getIndegree());
        assertEquals(0, e.getIndegree());

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

        List<Service> topologicalOrder = new LinkedList<>(serviceManager.getTopologicalOrder());
        int aOrder = topologicalOrder.indexOf(a),
            bOrder = topologicalOrder.indexOf(b),
            cOrder = topologicalOrder.indexOf(c),
            dOrder = topologicalOrder.indexOf(d),
            eOrder = topologicalOrder.indexOf(e);

        assertTrue(aOrder < bOrder);
        assertTrue(bOrder == cOrder - 1 || bOrder == cOrder + 1);
        assertTrue(bOrder < dOrder);
        assertTrue(dOrder == eOrder - 1 || dOrder == eOrder + 1);

        assertThat(a.getParentLatches(), hasItems(b.getStartLatch(), c.getStartLatch()));
        assertEquals(0, a.getChildrenLatches().size());

        assertThat(b.getParentLatches(), hasItems(d.getStartLatch()));
        assertThat(b.getChildrenLatches(), hasItems(a.getStopLatch()));

        assertThat(c.getParentLatches(), hasItems(d.getStartLatch()));
        assertThat(c.getChildrenLatches(), hasItems(a.getStopLatch()));

        assertEquals(0, d.getParentLatches().size());
        assertThat(d.getChildrenLatches(), hasItems(b.getStopLatch(), c.getStopLatch()));

        assertEquals(0, e.getParentLatches().size());
        assertEquals(0, e.getChildrenLatches().size());
    }

    @Test(expected = RuntimeException.class)
    public void testCyclicServiceManager() throws IOException {
        new ServiceManager("cyclic_services.txt");
    }
}
