package playground.rost.eaflow.TestNetworks;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

public class TestNetwork {
	public static NetworkWithDemands getSimple1SourceForwardNetwork()
	{
		//create network
		NetworkLayer network = new NetworkLayer();
		network.createAndAddNode(new IdImpl("source"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("between node"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,0));
		
		
		network.createAndAddLink(new IdImpl("s -> bN"),
							network.getNode("source"),
							network.getNode("between node"),
							8,
							1, // freespeed
							10, // cap
							1);
		

		network.createAndAddLink(new IdImpl("bN -> sink"),
							network.getNode("between node"),
							network.getNode("superSink"),
							4,
							1, // freespeed
							20, // cap
							1);
		
		Node sink = network.getNode("superSink");
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source = network.getNode("source");
		nWD.addDemand(source, 60);
		return nWD;
	}
	
	public static NetworkWithDemands getSimple2SourceForwardNetwork()
	{
		//create network
		NetworkLayer network = new NetworkLayer();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("between node"), new CoordImpl(1,1));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		
		network.createAndAddLink(new IdImpl("s1 -> bN"),
				network.getNode("source1"),
				network.getNode("between node"),
				16,
				2, // freespeed
				10, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bN"),
				network.getNode("source2"),
				network.getNode("between node"),
				2,
				2, // freespeed
				10, // cap
				1);
		

		network.createAndAddLink(new IdImpl("bN -> sink"),
							network.getNode("between node"),
							network.getNode("superSink"),
							8,
							2, // freespeed
							20, // cap
							1);
		
		Node sink = network.getNode("superSink");
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNode("source1");
		nWD.addDemand(source1, 60);
		Node source2 = network.getNode("source2");
		nWD.addDemand(source2, 60);
		return nWD;
	}
	
	
	public static NetworkWithDemands get2SourceNetworkWithBackwardEdgeUse()
	{
		//create network
		NetworkLayer network = new NetworkLayer();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("bAbove"), new CoordImpl(1,2));
		network.createAndAddNode(new IdImpl("bBeneath"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		
		network.createAndAddLink(new IdImpl("s1 -> bAbove"),
				network.getNode("source1"),
				network.getNode("bAbove"),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bBeneath"),
				network.getNode("source2"),
				network.getNode("bBeneath"),
				3,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> bBeneath"),
				network.getNode("bAbove"),
				network.getNode("bBeneath"),
				2,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> sink"),
							network.getNode("bAbove"),
							network.getNode("superSink"),
							5,
							1, // freespeed
							2, // cap
							1);

		network.createAndAddLink(new IdImpl("bBeneath -> sink"),
				network.getNode("bBeneath"),
				network.getNode("superSink"),
				1,
				1, // freespeed
				2, // cap
				1);
		
		Node sink = network.getNode("superSink");
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNode("source1");
		nWD.addDemand(source1, 100);
		Node source2 = network.getNode("source2");
		nWD.addDemand(source2, 100);
		return nWD;
	}
	
	public static NetworkWithDemands getCrazyNetwork()
	{
		//create network
		NetworkLayer network = new NetworkLayer();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("source3"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("bAbove"), new CoordImpl(1,2));
		network.createAndAddNode(new IdImpl("bBeneath"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		network.createAndAddLink(new IdImpl("s1 -> s2"),
				network.getNode("source1"),
				network.getNode("source2"),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> s1"),
				network.getNode("source2"),
				network.getNode("source1"),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s1 -> bAbove"),
				network.getNode("source1"),
				network.getNode("bAbove"),
				100,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bBeneath"),
				network.getNode("source2"),
				network.getNode("bBeneath"),
				300,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s3 -> bAbove"),
				network.getNode("source3"),
				network.getNode("bAbove"),
				3,
				1, // freespeed
				2, // cap
				1);
		
		
		
		network.createAndAddLink(new IdImpl("bAbove -> bBeneath"),
				network.getNode("bAbove"),
				network.getNode("bBeneath"),
				2,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> sink"),
							network.getNode("bAbove"),
							network.getNode("superSink"),
							5,
							1, // freespeed
							2, // cap
							1);

		network.createAndAddLink(new IdImpl("bBeneath -> sink"),
				network.getNode("bBeneath"),
				network.getNode("superSink"),
				1,
				1, // freespeed
				2, // cap
				1);
		
		Node sink = network.getNode("superSink");
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNode("source1");
		nWD.addDemand(source1, 10000);
		Node source2 = network.getNode("source2");
		nWD.addDemand(source2, 10000);
		Node source3 = network.getNode("source3");
		nWD.addDemand(source3, 10);
		return nWD;
	}
}
