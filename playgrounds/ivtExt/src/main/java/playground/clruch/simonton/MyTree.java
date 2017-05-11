package playground.clruch.simonton;

import playground.clruch.simonton.Cluster.Point;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Eric Simonton
 */
public class MyTree<ValueType> {

    private final Node root;
    private final int maxDensity;
    private final double maxCoordinate;

    private int size;

    private int numRemoved;
    private final double[] lBounds;
    private final double[] uBounds;

    public MyTree(
            int dimensions,
            int maxDensity,
            double maxCoordinate,
            int maxDepth) {

        this.maxDensity = maxDensity;
        this.maxCoordinate = maxCoordinate;
        root = new Node(maxDepth);
        lBounds = new double[dimensions];
        uBounds = new double[dimensions];
    }

    public void add(double[] location, ValueType value) {

        add(new Point<ValueType>(value, location));
    }

    public Point<ValueType> add(Point<ValueType> point) {

        resetBounds();
        Point<ValueType> removed = root.add(point);
        if (removed == null) {
            ++size;
        } else {
            // System.out.printf("Removed %s (#%d)\n", Arrays
            // .toString(removed.location), ++numRemoved);
        }
        return removed;
    }

    public Cluster<ValueType> buildCluster(
            double[] center,
            int size,
            Distancer distancer) {

        resetBounds();
        Cluster<ValueType> cluster =
                new Cluster<ValueType>(center, size, distancer);
        root.addToCluster(cluster);
        return cluster;
    }

    public int size() {

        return size;
    }

    private void resetBounds() {

        for (int i = lBounds.length; --i >= 0; ) {
            lBounds[i] = 0;
            uBounds[i] = maxCoordinate;
        }
    }

    private class Node {

        private final int maxDepth;

        private boolean internal = false;
        ;

        private Node lChild;
        private Node rChild;
        private Queue<Point<ValueType>> data =
                new LinkedList<Point<ValueType>>();

        public Node(final int maxDepth) {

            this.maxDepth = maxDepth;
        }

        public Point<ValueType> add(Point<ValueType> point) {

            if (internal) {
                double[] location = point.location;
                int dimension = maxDepth % location.length;
                double median = (lBounds[dimension] + uBounds[dimension]) / 2;
                if (location[dimension] < median) {
                    uBounds[dimension] = median;
                    if (lChild == null) {
                        lChild = new Node(maxDepth - 1);
                    }
                    return lChild.add(point);
                }
                lBounds[dimension] = median;
                if (rChild == null) {
                    rChild = new Node(maxDepth - 1);
                }
                return rChild.add(point);
            }

            if (data.size() < maxDensity) {
                data.add(point);
                return null;
            }

            if (maxDepth == 1) {
                data.add(point);
                return data.poll();
            }

            int dimension = maxDepth % lBounds.length;
            double median = (lBounds[dimension] + uBounds[dimension]) / 2;
            for (Point<ValueType> p : data) {
                if (p.location[dimension] < median) {
                    if (lChild == null) {
                        lChild = new Node(maxDepth - 1);
                    }
                    lChild.data.add(p);
                } else {
                    if (rChild == null) {
                        rChild = new Node(maxDepth - 1);
                    }
                    rChild.data.add(p);
                }
            }
            data = null;
            internal = true;
            return add(point);
        }

        public void addToCluster(Cluster<ValueType> cluster) {

            if (internal) {
                int dimension = maxDepth % lBounds.length;
                double median = (lBounds[dimension] + uBounds[dimension]) / 2;
                boolean lFirst = cluster.center[dimension] < median;
                addChildToCluster(cluster, median, lFirst);
                addChildToCluster(cluster, median, !lFirst);
            } else {
                for (Point<ValueType> p : data) {
                    cluster.consider(p);
                }
            }
        }

        private void addChildToCluster(
                Cluster<ValueType> cluster,
                double median,
                boolean left) {

            int dimension = maxDepth % lBounds.length;
            if (left) {
                if (lChild == null) {
                    return;
                }
                double orig = uBounds[dimension];
                uBounds[dimension] = median;
                if (cluster.isViable(lBounds, uBounds)) {
                    lChild.addToCluster(cluster);
                }
                uBounds[dimension] = orig;
            } else {
                if (rChild == null) {
                    return;
                }
                double orig = lBounds[dimension];
                lBounds[dimension] = median;
                if (cluster.isViable(lBounds, uBounds)) {
                    rChild.addToCluster(cluster);
                }
                lBounds[dimension] = orig;
            }
        }
    }
}
