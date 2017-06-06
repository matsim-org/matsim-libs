package playground.clruch.simonton;

/**
 * Created by Claudio on 3/23/2017.
 */
public class Test {
    public static void main(String[]args){
        System.out.println("Starting to test.");

        MyTree<String> testTree = new MyTree<String>(2,10, 10000.0,100);
        testTree.add(new double[]{1.0,1.0},"d1");
        testTree.add(new double[]{1.0,0.0},"d2");
        testTree.add(new double[]{0.0,1.0},"d3");
        testTree.add(new double[]{1.0,1.0},"d4");
        testTree.add(new double[]{0.1,0.1},"d5");
        testTree.add(new double[]{6.0,7.0},"d6");

        Distancer distancer = new Distancer() {
            @Override
            public double getDistance(double[] d1, double[] d2) {
                return Math.hypot(d1[0]-d2[0],d1[1]-d2[1]);
            }
        };
        Cluster<String> myCluster =  testTree.buildCluster(new double[]{0.0,0.0},4,distancer);


        System.out.println(myCluster.toString());


    }
}
