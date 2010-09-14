package playground.rost.eaflow.TestNetworks;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

public class TestNetwork {
	public static NetworkWithDemands getSimple1SourceForwardNetwork()
	{
		//create network
		NetworkImpl network = NetworkImpl.createNetwork();
		network.createAndAddNode(new IdImpl("source"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("between node"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,0));
		
		
		network.createAndAddLink(new IdImpl("s -> bN"),
							network.getNodes().get(new IdImpl("source")),
							network.getNodes().get(new IdImpl("between node")),
							8,
							1, // freespeed
							10, // cap
							1);
		

		network.createAndAddLink(new IdImpl("bN -> sink"),
							network.getNodes().get(new IdImpl("between node")),
							network.getNodes().get(new IdImpl("superSink")),
							4,
							1, // freespeed
							20, // cap
							1);
		
		Node sink = network.getNodes().get(new IdImpl("superSink"));
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source = network.getNodes().get(new IdImpl("source"));
		nWD.addDemand(source, 60);
		return nWD;
	}
	
	public static NetworkWithDemands getSimple2SourceForwardNetwork()
	{
		//create network
		NetworkImpl network = NetworkImpl.createNetwork();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("between node"), new CoordImpl(1,1));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		
		network.createAndAddLink(new IdImpl("s1 -> bN"),
				network.getNodes().get(new IdImpl("source1")),
				network.getNodes().get(new IdImpl("between node")),
				16,
				2, // freespeed
				10, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bN"),
				network.getNodes().get(new IdImpl("source2")),
				network.getNodes().get(new IdImpl("between node")),
				2,
				2, // freespeed
				10, // cap
				1);
		

		network.createAndAddLink(new IdImpl("bN -> sink"),
							network.getNodes().get(new IdImpl("between node")),
							network.getNodes().get(new IdImpl("superSink")),
							8,
							2, // freespeed
							20, // cap
							1);
		
		Node sink = network.getNodes().get(new IdImpl("superSink"));
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNodes().get(new IdImpl("source1"));
		nWD.addDemand(source1, 60);
		Node source2 = network.getNodes().get(new IdImpl("source2"));
		nWD.addDemand(source2, 60);
		return nWD;
	}
	
	
	public static NetworkWithDemands get2SourceNetworkWithBackwardEdgeUse()
	{
		//create network
		NetworkImpl network = NetworkImpl.createNetwork();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("bAbove"), new CoordImpl(1,2));
		network.createAndAddNode(new IdImpl("bBeneath"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		
		network.createAndAddLink(new IdImpl("s1 -> bAbove"),
				network.getNodes().get(new IdImpl("source1")),
				network.getNodes().get(new IdImpl("bAbove")),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bBeneath"),
				network.getNodes().get(new IdImpl("source2")),
				network.getNodes().get(new IdImpl("bBeneath")),
				3,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> bBeneath"),
				network.getNodes().get(new IdImpl("bAbove")),
				network.getNodes().get(new IdImpl("bBeneath")),
				2,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> sink"),
							network.getNodes().get(new IdImpl("bAbove")),
							network.getNodes().get(new IdImpl("superSink")),
							5,
							1, // freespeed
							2, // cap
							1);

		network.createAndAddLink(new IdImpl("bBeneath -> sink"),
				network.getNodes().get(new IdImpl("bBeneath")),
				network.getNodes().get(new IdImpl("superSink")),
				1,
				1, // freespeed
				2, // cap
				1);
		
		Node sink = network.getNodes().get(new IdImpl("superSink"));
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNodes().get(new IdImpl("source1"));
		nWD.addDemand(source1, 100);
		Node source2 = network.getNodes().get(new IdImpl("source2"));
		nWD.addDemand(source2, 100);
		return nWD;
	}
	
	public static NetworkWithDemands getCrazyNetwork()
	{
		//create network
		NetworkImpl network = NetworkImpl.createNetwork();
		network.createAndAddNode(new IdImpl("source1"), new CoordImpl(0,2));
		network.createAndAddNode(new IdImpl("source2"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("source3"), new CoordImpl(0,0));
		network.createAndAddNode(new IdImpl("bAbove"), new CoordImpl(1,2));
		network.createAndAddNode(new IdImpl("bBeneath"), new CoordImpl(1,0));
		network.createAndAddNode(new IdImpl("superSink"), new CoordImpl(2,1));
		
		network.createAndAddLink(new IdImpl("s1 -> s2"),
				network.getNodes().get(new IdImpl("source1")),
				network.getNodes().get(new IdImpl("source2")),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> s1"),
				network.getNodes().get(new IdImpl("source2")),
				network.getNodes().get(new IdImpl("source1")),
				1,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s1 -> bAbove"),
				network.getNodes().get(new IdImpl("source1")),
				network.getNodes().get(new IdImpl("bAbove")),
				100,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s2 -> bBeneath"),
				network.getNodes().get(new IdImpl("source2")),
				network.getNodes().get(new IdImpl("bBeneath")),
				300,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("s3 -> bAbove"),
				network.getNodes().get(new IdImpl("source3")),
				network.getNodes().get(new IdImpl("bAbove")),
				3,
				1, // freespeed
				2, // cap
				1);
		
		
		
		network.createAndAddLink(new IdImpl("bAbove -> bBeneath"),
				network.getNodes().get(new IdImpl("bAbove")),
				network.getNodes().get(new IdImpl("bBeneath")),
				2,
				1, // freespeed
				2, // cap
				1);
		
		network.createAndAddLink(new IdImpl("bAbove -> sink"),
							network.getNodes().get(new IdImpl("bAbove")),
							network.getNodes().get(new IdImpl("superSink")),
							5,
							1, // freespeed
							2, // cap
							1);

		network.createAndAddLink(new IdImpl("bBeneath -> sink"),
				network.getNodes().get(new IdImpl("bBeneath")),
				network.getNodes().get(new IdImpl("superSink")),
				1,
				1, // freespeed
				2, // cap
				1);
		
		Node sink = network.getNodes().get(new IdImpl("superSink"));
		NetworkWithDemands nWD = new NetworkWithDemands(network, sink);
		
		Node source1 = network.getNodes().get(new IdImpl("source1"));
		nWD.addDemand(source1, 10000);
		Node source2 = network.getNodes().get(new IdImpl("source2"));
		nWD.addDemand(source2, 10000);
		Node source3 = network.getNodes().get(new IdImpl("source3"));
		nWD.addDemand(source3, 10);
		return nWD;
	}
}
