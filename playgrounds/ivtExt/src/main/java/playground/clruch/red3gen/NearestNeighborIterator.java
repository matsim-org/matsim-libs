package playground.clruch.red3gen;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 */
public class NearestNeighborIterator<Type> implements Iterator<Type>, Iterable<Type> {
  private final DistanceFunction distanceFunction;
  private final double[] searchPoint;
  private MinHeap<KdNode<Type>> pendingPaths;
  private IntervalHeap<Type> evaluatedPoints;
  private int pointsRemaining;
  private double lastDistanceReturned;

  protected NearestNeighborIterator(KdNode<Type> treeRoot, double[] searchPoint, int maxPointsReturned, DistanceFunction distanceFunction) {
    this.searchPoint = Arrays.copyOf(searchPoint, searchPoint.length);
    this.pointsRemaining = Math.min(maxPointsReturned, treeRoot.size());
    this.distanceFunction = distanceFunction;
    this.pendingPaths = new BinaryHeap.Min<KdNode<Type>>();
    this.pendingPaths.offer(0, treeRoot);
    this.evaluatedPoints = new IntervalHeap<Type>();
  }

  /* -------- INTERFACE IMPLEMENTATION -------- */
  @Override
  public boolean hasNext() {
    return pointsRemaining > 0;
  }

  @Override
  public Type next() {
    if (!hasNext()) {
      throw new IllegalStateException("NearestNeighborIterator has reached end!");
    }
    while (pendingPaths.size() > 0 && (evaluatedPoints.size() == 0 || (pendingPaths.getMinKey() < evaluatedPoints.getMinKey()))) {
      KdTree.nearestNeighborSearchStep(pendingPaths, evaluatedPoints, pointsRemaining, distanceFunction, searchPoint);
    }
    // Return the smallest distance point
    pointsRemaining--;
    lastDistanceReturned = evaluatedPoints.getMinKey();
    Type value = evaluatedPoints.getMin();
    evaluatedPoints.removeMin();
    return value;
  }

  public double distance() {
    return lastDistanceReturned;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Type> iterator() {
    return this;
  }
}
