package playground.clruch.red3gen;

public class Test {
  public static void main(String[] args) {
    KdTree<String> myKdTree = new KdTree<>(2);
    for (int c = 0; c < 10; ++c) {
      myKdTree.addPoint(new double[] { .2, .2 }, "n" + c);
      myKdTree.addPoint(new double[] { .2, .2 }, "n" + c);
      myKdTree.addPoint(new double[] { .2+c*.1, .2 }, "m" + c);
    }
    myKdTree.addPoint(new double[] { .312, .2 }, "other");
    // myKdTree.addPoint(new double[] { .0, .1 }, "close");
    DistanceFunction dist = new SquareEuclideanDistanceFunction();
    double[] searchPoint = new double[] { 0, 0 };
    {
      MaxHeap<String> myMaxHeap = myKdTree.findNearestNeighbors(searchPoint, 2, dist);
      int size = myMaxHeap.size();
      for (int c = 0; c < size; ++c) {
        System.out.println(myMaxHeap.getMax());
        System.out.println(myMaxHeap.getMaxKey());
        myMaxHeap.removeMax();
      }
    }
    System.out.println("---");
    {
      System.out.println(myKdTree.size());
      NearestNeighborIterator<String> myNearestNeighborIterator = myKdTree.getNearestNeighborIterator(searchPoint, myKdTree.size(), dist);
      for (String myString : myNearestNeighborIterator) {
        System.out.println(myString);
      }
    }
  }
}
