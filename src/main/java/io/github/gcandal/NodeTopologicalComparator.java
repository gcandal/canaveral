package io.github.gcandal;

import java.util.Comparator;

public class NodeTopologicalComparator implements Comparator<Node> {

    @Override
    public int compare(Node x, Node y) {
        if(x == null && y == null) {
            return 0;
        }
        if(x == null) {
            return -1;
        }
        if(y == null) {
            return 1;
        }
        if (x.getIndegree() > y.getIndegree())
        {
            return -1;
        }
        if (x.getIndegree() < y.getIndegree())
        {
            return 1;
        }
        return 0;
    }
}
