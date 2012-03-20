package playground.tnicolai.matsim4opus.alpha;

import junit.framework.Assert;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
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
		
		
		double distance1 = NetworkUtil.getOrthogonalDistance(link1, new CoordImpl(10., 0.23456789));
		Assert.assertEquals(10.0, distance1);
		System.out.println(distance1);
		
		double distance2 = NetworkUtil.getOrthogonalDistance(link2, new CoordImpl(1500, 0));
		System.out.println(distance2);
		
		double distance3 = NetworkUtil.getOrthogonalDistance(link3,new CoordImpl(1111, 2222));
		Assert.assertEquals(222.0, distance3);
		System.out.println(distance3);
		
		double distance4 = NetworkUtil.getOrthogonalDistance(link3, new CoordImpl(9000, 2222));
		Assert.assertEquals(222.0, distance4);
		System.out.println(distance4);
		// the implemented distance calculator in Link Class don't return the right value
		double distance4_1 = link3.calcDistance(new CoordImpl(9000, 2222));
		// should be also 222, -> implement own calculation
		System.out.println(distance4_1);
		
		// the result should be 600m (100m distance point to link + 500m intersection to destination node)
		double distance5 = NetworkUtil.getDistance2Node(link1, new CoordImpl(100., 500.), link1.getToNode());
		Assert.assertEquals(600.0, distance5);
		System.out.println(distance5);
	}
}
