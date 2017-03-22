package playground.clruch.red3gen;

/**
 *
 */
public class KdTree<Type> extends KdNode<Type> {
  public KdTree(int dimensions) {
    this(dimensions, 24);
  }

  public KdTree(int dimensions, int bucketCapacity) {
    super(dimensions, bucketCapacity);
  }

  public NearestNeighborIterator<Type> getNearestNeighborIterator(double[] searchPoint, int maxPointsReturned, DistanceFunction distanceFunction) {
    return new NearestNeighborIterator<Type>(this, searchPoint, maxPointsReturned, distanceFunction);
  }

  public MaxHeap<Type> findNearestNeighbors(double[] searchPoint, int maxPointsReturned, DistanceFunction distanceFunction) {
    BinaryHeap.Min<KdNode<Type>> pendingPaths = new BinaryHeap.Min<KdNode<Type>>();
    BinaryHeap.Max<Type> evaluatedPoints = new BinaryHeap.Max<Type>();
    int pointsRemaining = Math.min(maxPointsReturned, size());
    pendingPaths.offer(0, this);
    while (pendingPaths.size() > 0 && (evaluatedPoints.size() < pointsRemaining || (pendingPaths.getMinKey() < evaluatedPoints.getMaxKey()))) {
      nearestNeighborSearchStep(pendingPaths, evaluatedPoints, pointsRemaining, distanceFunction, searchPoint);
    }
    return evaluatedPoints;
  }

  @SuppressWarnings("unchecked")
  protected static <T> void nearestNeighborSearchStep(MinHeap<KdNode<T>> pendingPaths, MaxHeap<T> evaluatedPoints, int desiredPoints,
      DistanceFunction distanceFunction, double[] searchPoint) {
    // If there are pending paths possibly closer than the nearest evaluated point, check it out
    KdNode<T> cursor = pendingPaths.getMin();
    pendingPaths.removeMin();
    // Descend the tree, recording paths not taken
    while (!cursor.isLeaf()) {
      KdNode<T> pathNotTaken;
      if (searchPoint[cursor.splitDimension] > cursor.splitValue) {
        pathNotTaken = cursor.left;
        cursor = cursor.right;
      } else {
        pathNotTaken = cursor.right;
        cursor = cursor.left;
      }
      double otherDistance = distanceFunction.distanceToRect(searchPoint, pathNotTaken.minBound, pathNotTaken.maxBound);
      // Only add a path if we either need more points or it's closer than furthest point on list so far
      if (evaluatedPoints.size() < desiredPoints || otherDistance <= evaluatedPoints.getMaxKey()) {
        pendingPaths.offer(otherDistance, pathNotTaken);
      }
    }
    if (cursor.singlePoint) {
      double nodeDistance = distanceFunction.distance(cursor.points[0], searchPoint);
      // Only add a point if either need more points or it's closer than furthest on list so far
      if (evaluatedPoints.size() < desiredPoints || nodeDistance <= evaluatedPoints.getMaxKey()) {
        for (int i = 0; i < cursor.size(); i++) {
          T value = (T) cursor.data[i];
          // If we don't need any more, replace max
          if (evaluatedPoints.size() == desiredPoints) {
            evaluatedPoints.replaceMax(nodeDistance, value);
          } else {
            evaluatedPoints.offer(nodeDistance, value);
          }
        }
      }
    } else {
      // Add the points at the cursor
      for (int i = 0; i < cursor.size(); i++) {
        double[] point = cursor.points[i];
        T value = (T) cursor.data[i];
        double distance = distanceFunction.distance(point, searchPoint);
        // Only add a point if either need more points or it's closer than furthest on list so far
        if (evaluatedPoints.size() < desiredPoints) {
          evaluatedPoints.offer(distance, value);
        } else if (distance < evaluatedPoints.getMaxKey()) {
          evaluatedPoints.replaceMax(distance, value);
        }
      }
    }
  }
}
