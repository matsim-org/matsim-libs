package playground.clruch.simonton;

import java.util.*;

/**
 * @author Eric Simonton
 */
public class Cluster<ValueType> implements Iterable<Cluster.Point<ValueType>> {

    public final double[] center;

    private final int size;
    private final Distancer distancer;
    private final PriorityQueue<Point<ValueType>> points =
            new PriorityQueue<Point<ValueType>>();

    private final double[] testPoint;

    public Cluster(double[] center, int size, Distancer distancer) {

        this.center = center;
        this.size = size;
        this.distancer = distancer;
        testPoint = new double[center.length];
    }

    public Collection<ValueType> getValues() {

        Collection<ValueType> values = new ArrayList<ValueType>(points.size());
        for (Point<ValueType> node : points) {
            values.add(node.value);
        }
        return values;
    }

    public void consider(ValueType value, double[] location) {

        consider(new Point<ValueType>(value, location));
    }

    public void consider(Point<ValueType> p) {

        p.setDistanceToCenter(distancer, center);
        if (points.size() < size) {
            points.add(p);
        }
        else {
            if (p.distanceToCenter < points.peek().distanceToCenter) {
                points.poll();
                points.add(p);
            }
        }
    }

    public void trimTo(int size) {

        while (points.size() > size) {
            points.poll();
        }
    }

    public boolean isViable(double[] lBounds, double[] uBounds) {

        if (points.size() < size) {
            return true;
        }
        for (int i = lBounds.length; --i >= 0;) {
            testPoint[i] = bound(center[i], lBounds[i], uBounds[i]);
        }
        return points.peek().isFartherThan(testPoint, center, distancer);
    }
    static double bound(double value, double min, double max){
        if(min > value)
            return min;
        if(max < value)
            return max;
        return value;
    }

    public Iterator<Point<ValueType>> iterator() {

        return points.iterator();
    }

    public int size() {

        return points.size();
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof Cluster)) {
            return false;
        }

        Set mine = new HashSet(points);
        Set his = new HashSet(((Cluster) o).points);
        return mine.equals(his);
    }

    @Override
    public String toString() {

        return points.toString();
    }

    public static class Point<T> implements Comparable<Point<T>> {

        public final T value;
        public final double[] location;
        public double distanceToCenter;

        public Point(T value, double[] location) {

            this.value = value;
            this.location = location;
        }

        public void setDistanceToCenter(Distancer distancer, double[] center) {

            distanceToCenter = distancer.getDistance(center, location);
        }

        public boolean isFartherThan(
                double[] testPoint,
                double[] center,
                Distancer distancer) {

            return distanceToCenter > distancer.getDistance(testPoint, center);
        }

        public int compareTo(Point<T> other) {

            if (distanceToCenter > other.distanceToCenter) {
                return -1;
            }
            if (distanceToCenter < other.distanceToCenter) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {

            if (!(o instanceof Point)) {
                return false;
            }

            Point other = (Point) o;
            return value.equals(other.value)
                    && Arrays.equals(location, other.location);
        }

        @Override
        public int hashCode() {

            return Arrays.hashCode(location) ^ value.hashCode();
        }

        @Override
        public String toString() {

            return value.toString();
        }
    }
}