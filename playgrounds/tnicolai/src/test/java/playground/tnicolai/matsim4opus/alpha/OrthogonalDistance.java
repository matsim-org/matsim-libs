package playground.tnicolai.matsim4opus.alpha;

import junit.framework.Assert;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

public class OrthogonalDistance {
	
	public static void main(String args[]){
		
		/* create a sample network:
		 *
		 *        (3)---3---(4)
		 *       /         /
		 *     2          /
		 *   /           /
		 * (2)          4
		 *  |          /
		 *  1         /
		 *  |        /
		 * (1)    (5)
		 *
		 * The network contains an exactly horizontal, an exactly vertical, an exactly diagonal
		 * and another link with no special slope to also test possible special cases.
		 */
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(0, 1000));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1000, 2000));
		Node node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(2000, 2000));
		Node node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(1000, 0));
		LinkImpl link1 = (LinkImpl) network.createAndAddLink(new IdImpl("1"), node1, node2, 1000, 1, 3600, 1);
		LinkImpl link2 = (LinkImpl) network.createAndAddLink(new IdImpl("2"), node2, node3, 1500, 1, 3600, 1);
		LinkImpl link3 = (LinkImpl) network.createAndAddLink(new IdImpl("3"), node3, node4, 1000, 1, 3600, 1);
		LinkImpl link4 = (LinkImpl) network.createAndAddLink(new IdImpl("4"), node4, node5, 2800, 1, 3600, 1);
		
		// the method getOrthogonalDistance assumes that a link has unlimited length -> it gives just the distance between a point and a line
		// the method getDistance2Node also considers the distance between the intersection of the projection from a point to a line to the network node 
		
		double distance1 = NetworkUtil.getOrthogonalDistance2NearestLink(link1, new CoordImpl(10., 0.23456789));
		Assert.assertEquals(10.0, distance1);
		System.out.println(distance1 + " distance1");
		
		double distance1_1 = NetworkUtil.getOrthogonalDistance2NearestLink(link1, new CoordImpl(10., -10.));
		Assert.assertEquals(10.0, distance1_1);
		System.out.println(distance1_1 + " distance1_1");
		
		double distance1_2 = NetworkUtil.getDistance2Node(link1, new CoordImpl(10., -10.), link1.getToNode()); // distance point2link=10 + distance intersection2Node=1010
		Assert.assertEquals(1020.0, distance1_2);
		System.out.println(distance1_2 + " distance1_2");
//		double distance1_3 = NetworkUtil.getDistance2NodeV2(link1, new CoordImpl(10., -10.), link1.getToNode()); // distance point2link=10 + distance intersection2Node=1010
//		Assert.assertEquals(1020.0, distance1_3);
//		System.out.println(distance1_3 + " distance1_3");
//		Assert.assertEquals(distance1_2, distance1_3);
		
		double distance2 = NetworkUtil.getOrthogonalDistance2NearestLink(link2, new CoordImpl(1500, 0));
		System.out.println(distance2);
		
		double distance2_1 = NetworkUtil.getDistance2Node(link2, new CoordImpl(1500, 0), link2.getToNode());
		System.out.println(distance2_1 + " distance2_1");
//		double distance2_2 = NetworkUtil.getDistance2NodeV2(link2, new CoordImpl(1500, 0), link2.getToNode());
//		System.out.println(distance2_2 + " distance2_2");
//		Assert.assertEquals(distance2_1, distance2_2);
		
		double distance3 = NetworkUtil.getOrthogonalDistance2NearestLink(link3,new CoordImpl(1111, 2222));
		Assert.assertEquals(222.0, distance3);
		System.out.println(distance3 + " distance3");
		double distance4 = NetworkUtil.getOrthogonalDistance2NearestLink(link3, new CoordImpl(9000, 2222));
		Assert.assertEquals(222.0, distance4);
		System.out.println(distance4 + " distance4");
		Assert.assertEquals(distance3, distance4);
		
		// the result should be 600m (100m distance point to link + 500m intersection to destination node)
		double distance5 = NetworkUtil.getDistance2Node(link1, new CoordImpl(100., 500.), link1.getToNode());
		Assert.assertEquals(600.0, distance5);
		System.out.println(distance5 + " distance5");
//		double distance5_1 = NetworkUtil.getDistance2NodeV2(link1, new CoordImpl(100., 500.), link1.getToNode());
//		Assert.assertEquals(600.0, distance5_1);
//		System.out.println(distance5_1 + " distance5_1");
//		Assert.assertEquals(distance5, distance5_1);
		
		double distance6 = NetworkUtil.getDistance2Node(link1, new CoordImpl(-150., -500.), link1.getToNode());
		Assert.assertEquals(1650.0, distance6);
		System.out.println(distance6 + " distance6");
		
		// Speed test getDistance2Node vs getDistance2NodeV2
		double start = System.currentTimeMillis();
		for(int i = 0; i <  1000000000; i++)
			NetworkUtil.getDistance2Node(link1, new CoordImpl(10., -10.), link1.getToNode());
		System.out.println("NetworkUtil.getDistance2Node took: " + (System.currentTimeMillis() - start) / 1000 + " sec");
		start = System.currentTimeMillis();
		for(int i = 0; i <  1000000000; i++)
			NetworkUtil.getDistance2NodeV2(link1, new CoordImpl(10., -10.), link1.getToNode());
		System.out.println("NetworkUtil.getDistance2NodeV2 took: " + (System.currentTimeMillis() - start) / 1000 + " sec");
	}
}
