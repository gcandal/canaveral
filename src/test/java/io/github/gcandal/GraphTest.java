package io.github.gcandal;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.*;


public class GraphTest
{
    @Test
    public void testGraph() throws IOException {
        Graph graph = new Graph("services.txt");

        Node a = graph.getNode("a"),
            b = graph.getNode("b"),
            c = graph.getNode("c"),
            d = graph.getNode("d"),
            e = graph.getNode("e");

        assertEquals(2, a.getIndegree());
        assertEquals(1, b.getIndegree());
        assertEquals(1, c.getIndegree());
        assertEquals(0, d.getIndegree());
        assertEquals(0, e.getIndegree());

        Set<Node> expectedDependencies = new HashSet<>();
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

        List<Node> topologicalOrder = new LinkedList<>(graph.getTopologicalOrder());
        int aOrder = topologicalOrder.indexOf(a),
            bOrder = topologicalOrder.indexOf(b),
            cOrder = topologicalOrder.indexOf(c),
            dOrder = topologicalOrder.indexOf(d),
            eOrder = topologicalOrder.indexOf(e);

        assertTrue(aOrder < bOrder);
        assertTrue(bOrder == cOrder - 1 || bOrder == cOrder + 1);
        assertTrue(bOrder < dOrder);
        assertTrue(dOrder == eOrder - 1 || dOrder == eOrder + 1);

        assertThat(a.getParentLatches(), hasItems(b.getOwnLatch(), c.getOwnLatch()));
        assertThat(b.getParentLatches(), hasItems(d.getOwnLatch()));
        assertThat(c.getParentLatches(), hasItems(d.getOwnLatch()));
        assertEquals(0, d.getParentLatches().size());
        assertEquals(0, e.getParentLatches().size());
    }

    @Test(expected = RuntimeException.class)
    public void testCyclicGraph() throws IOException {
        new Graph("cyclic_services.txt");
    }
}
