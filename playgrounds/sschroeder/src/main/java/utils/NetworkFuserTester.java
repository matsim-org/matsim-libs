package utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class NetworkFuserTester {
	public static void main(String[] args) {
		NetworkImpl network = NetworkImpl.createNetwork();
		Node n1 = network.createAndAddNode(makeId("n1"), makeCoord(0,0));
		Node n2 = network.createAndAddNode(makeId("n2"), makeCoord(1,0));
		Node n3 = network.createAndAddNode(makeId("n3"), makeCoord(2,0));
		Node n4 = network.createAndAddNode(makeId("n4"), makeCoord(3,0));
		Node n5 = network.createAndAddNode(makeId("n5"), makeCoord(4,0));
		
		network.createAndAddLink(makeId("l1"),n1,n2,100.0,200,100,2);
		network.createAndAddLink(makeId("l2"),n2,n1,100.0,200,100,2);
		network.createAndAddLink(makeId("l3"),n2,n3,100.0,200,100,2);
		network.createAndAddLink(makeId("l4"),n3,n2,100.0,200,100,2);
		printNetwork(network);
		NetworkFuser fuser = new NetworkFuser(network);
		fuser.setLinkMerger(new LinkMerger());
		fuser.setMergingConstraint(new MergingConstraint());
		fuser.fuse();
		printNetwork(network);
		
		//1->2->3
		//1<-2<-3
		
	}
	
	private static void printNetwork(Network n){
		for(Link l : n.getLinks().values()){
			System.out.println(l);
		}
		System.out.println();
	}

	private static Coord makeCoord(int i, int j) {
		return new CoordImpl(i,j);
	}

	private static Id makeId(String string) {
		return new IdImpl(string);
	}
}
