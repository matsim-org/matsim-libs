package playground.tnicolai.matsim4opus.alpha;

import junit.framework.Assert;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

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
		
		
		double dist1 = link1.calcDistance(new CoordImpl(10., 0.23456789));
		Assert.assertEquals(10.0, dist1);
		System.out.println(dist1);
		
		double dist2 = link3.calcDistance(new CoordImpl(1111, 2222));
		Assert.assertEquals(222.0, dist2);
		System.out.println(dist2);
		
		double dist3 = link3.calcDistance(new CoordImpl(9000, 2222));
		// should be also 222, -> implement own calculation
		System.out.println(dist3);
	}

}
