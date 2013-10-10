package josmMatsimPlugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * writes out a network-xml from a NetworkLayer
 * @author nkuehnel
 * 
 */
public class Network2Network extends AbstractJosm2Network
{

	protected Network2Network(Network network,
			CoordinateTransformation transformation, boolean keepPaths)
	{
		super(network, transformation);
		this.keepPaths=keepPaths;
	}

	@Override
	public void writeNodes(Collection<Node> nodes)
	{
		for (Node n : sortById(nodes))
		{
			visit(n);
		}
		
	}

	@Override
	protected void writeWays(Collection<Way> ways)
	{
		for (Way w : sortById(ways))
		{
			visit(w);
		}
	}

	@Override
	protected boolean shouldWrite(OsmPrimitive osm)
	{
		return true;
	}

	@Override
	public void convert()
	{
		if (this.network instanceof NetworkImpl)
		{
			((NetworkImpl) this.network).setCapacityPeriod(3600);
		}

		Iterator<Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		while (it.hasNext())
		{	
			Entry<Long, OsmWay> entry = it.next();
			for (Long nodeId : entry.getValue().nodes)
			{
				if (this.nodes.get(nodeId) == null)
				{
					it.remove();
					break;
				}
			}
		}

		// save the highest existing id to avoid overlapping when assigning new Ids to manually added nodes
		long maxNodeId=0;
		for (OsmNode node: nodes.values())
		{
			if(node.id>maxNodeId)
				maxNodeId=node.id;
		}
		
		// check which nodes are used
		for (OsmWay way : this.ways.values())
		{
				String idFrom=way.tags.get("from");
				if (idFrom!=null && nodes.containsKey(Long.parseLong(idFrom)))
					nodes.get(Long.parseLong(idFrom)).used=true;
				String idTo=way.tags.get("to");
				if (idTo!=null && nodes.containsKey(Long.parseLong(idTo)))
					nodes.get(Long.parseLong(idTo)).used=true;
				for (Long node: way.nodes)
				{
					if (node <0)
					{
						nodes.get(node).used=true;
						nodes.get(node).id=(maxNodeId+1);
						maxNodeId++;
					}
				}
		}
		
		

		if (!this.keepPaths)
		{
			// marked nodes as unused where only one way leads through
			for (OsmNode node : this.nodes.values())
			{
				if ((node.ways == 1) && (!this.keepPaths))
				{
					node.used = false;
				}
			}
		}

		// create the required nodes
		for (OsmNode node : this.nodes.values())
		{
			if (node.used)
			{
				org.matsim.api.core.v01.network.Node nn = this.network
						.getFactory().createNode(new IdImpl(node.id),
								node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		for (OsmWay way : this.ways.values())
		{
			OsmNode fromNode = this.nodes.get(way.nodes.get(0));
			double length = 0.0;
			OsmNode lastToNode = fromNode;
			if (fromNode.used)
			{
				for (int i = 1, n = way.nodes.size(); i < n; i++)
				{
					OsmNode toNode = this.nodes.get(way.nodes.get(i));
					if (toNode != lastToNode)
					{
						length += CoordUtils.calcDistance(lastToNode.coord,
								toNode.coord);
						if (toNode.used)
						{
							createLink(this.network, way, fromNode,	toNode, length);
							fromNode = toNode;
							length = 0.0;
						}
						lastToNode = toNode;
					}
				}
			}
		}
		// free up memory
		this.nodes.clear();
		this.ways.clear();
	}

	
	
		@Override
		public void createLink(Network network, OsmWay way, OsmNode fromNode,
				OsmNode toNode, double length)
		{
			long id = 0;
			String origId = "new Link";
			double nofLanes = 0;
			double freespeed = 0;
			boolean oneway = false;
			boolean onewayReverse = false;
			double capacity = 0;
			
			if(way.tags.get(TAG_ID)!=null)
				id=Long.parseLong(way.tags.get(TAG_ID));
			if(way.tags.get(TAG_LENGTH)!=null)
				length = Double.parseDouble(way.tags.get(TAG_LENGTH));
			if(way.tags.get(TAG_PERMLANES)!=null)
				nofLanes = Double.parseDouble(way.tags.get(TAG_PERMLANES));
			if(way.tags.get(TAG_CAPACITY)!=null)
				capacity = Double.parseDouble(way.tags.get(TAG_CAPACITY));
			if(way.tags.get(TAG_FREESPEED)!=null)
				freespeed = Double.parseDouble(way.tags.get(TAG_FREESPEED));
			if(way.tags.get(TAG_ONEWAY)!=null)
			{
				if (way.tags.get(TAG_ONEWAY).equals("1") || way.tags.get(TAG_ONEWAY).equals("true"))
					oneway = true;
				else
					oneway = false;
			}
			if (way.tags.get(TAG_ORIGID)!=null)
				origId=way.tags.get(TAG_ORIGID);
			
			
			// only create link, if both nodes were found, node could be null, since
			// nodes outside a layer were dropped
			Id fromId = new IdImpl(fromNode.id);
			Id toId = new IdImpl(toNode.id);
			
			id=checkId(id);
			
			if (network.getNodes().get(fromId) != null
					&& network.getNodes().get(toId) != null)
			{
				if (!onewayReverse)
				{
					Link l = network.getFactory().createLink(new IdImpl(id),
							network.getNodes().get(fromId),
							network.getNodes().get(toId));
					l.setLength(length);
					l.setFreespeed(freespeed);
					l.setCapacity(capacity);
					l.setNumberOfLanes(nofLanes);
					if (l instanceof LinkImpl)
					{
						((LinkImpl) l).setOrigId(origId);
					}
					network.addLink(l);
				}
				id=checkId(id);
				if (!oneway)
				{
					Link l = network.getFactory().createLink(new IdImpl(id),
							network.getNodes().get(toId),
							network.getNodes().get(fromId));
					l.setLength(length);
					l.setFreespeed(freespeed);
					l.setCapacity(capacity);
					l.setNumberOfLanes(nofLanes);
					if (l instanceof LinkImpl)
					{
						((LinkImpl) l).setOrigId(origId);
					}
					network.addLink(l);
				}
			}
		}

		private long checkId(long id)
		{
			Id tmp= new IdImpl(id);
			if (network.getLinks().containsKey(tmp))
			{
				for (Id x :network.getLinks().keySet())
				{
					if(Long.parseLong(x.toString())>id)
						id=Long.parseLong(x.toString());
				}
				id++;
				return id;
			}
			else
				return id;
		}

}
