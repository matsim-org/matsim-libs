package josmMatsimPlugin;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import josmMatsimPlugin.ExportDefaults.OsmHighwayDefaults;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * writes out a network from an OsmDataLayer
 * @author nkuehnel
 * 
 */
public class Osm2Network extends AbstractJosm2Network
{
	private final static Logger log = Logger.getLogger(AbstractJosm2Network.class);
	final Map<String, OsmHighwayDefaults> highwayDefaults;
	final List<OsmFilter> hierarchyLayers = new ArrayList<OsmFilter>();
	protected boolean scaleMaxSpeed = false;
	private String targetSystem;


	protected Osm2Network(Network network,
			CoordinateTransformation transformation, boolean keepPaths, String targetSystem, Map<String, OsmHighwayDefaults> defaults)
	{
		super(network, transformation);
		this.keepPaths=keepPaths;
		this.targetSystem=targetSystem;
		this.highwayDefaults=defaults;
	
	}
	
	@Override
	protected boolean shouldWrite(OsmPrimitive osm)
	{
		return !osm.isNewOrUndeleted() || !osm.isDeleted();
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

			// check which nodes are used
			for (OsmWay way : this.ways.values())
			{
				String highway = way.tags.get(TAG_HIGHWAY);
				if ((highway != null)
						&& (this.highwayDefaults.containsKey(highway)))
				{
					// check to which level a way belongs
					way.hierarchy = this.highwayDefaults.get(highway).hierarchy;

					// first and last are counted twice, so they are kept in all
					// cases
					this.nodes.get(way.nodes.get(0)).ways++;
					this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;

					for (Long nodeId : way.nodes)
					{
						OsmNode node = this.nodes.get(nodeId);
						if (this.hierarchyLayers.isEmpty())
						{
							node.used = true;
							node.ways++;
						} else
						{
							for (OsmFilter osmFilter : this.hierarchyLayers)
							{
								if (osmFilter.coordInFilter(node.coord,
										way.hierarchy))
								{
									node.used = true;
									node.ways++;
									break;
								}
							}
						}
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
				// verify we did not mark nodes as unused that build a loop
				for (OsmWay way : this.ways.values())
				{
					String highway = way.tags.get(TAG_HIGHWAY);
					if ((highway != null)
							&& (this.highwayDefaults.containsKey(highway)))
					{
						int prevRealNodeIndex = 0;
						OsmNode prevRealNode = this.nodes.get(way.nodes
								.get(prevRealNodeIndex));

						for (int i = 1; i < way.nodes.size(); i++)
						{
							OsmNode node = this.nodes.get(way.nodes.get(i));
							if (node.used)
							{
								if (prevRealNode == node)
								{
									/*
									 * We detected a loop between to "real" nodes.
									 * Set some nodes between the
									 * start/end-loop-node to "used" again. But
									 * don't set all of them to "used", as we still
									 * want to do some network-thinning. I decided
									 * to use sqrt(.)-many nodes in between...
									 */
									double increment = Math.sqrt(i
											- prevRealNodeIndex);
									double nextNodeToKeep = prevRealNodeIndex
											+ increment;
									for (double j = nextNodeToKeep; j < i; j += increment)
									{
										int index = (int) Math.floor(j);
										OsmNode intermediaryNode = this.nodes
												.get(way.nodes.get(index));
										intermediaryNode.used = true;
									}
								}
								prevRealNodeIndex = i;
								prevRealNode = node;
							}
						}
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
			this.id = 1;
			for (OsmWay way : this.ways.values())
			{
				String highway = way.tags.get(TAG_HIGHWAY);
				
				if (highway != null)
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
								if(this.targetSystem.equals("WGS84"))
									length+= calculateWGS84Length(lastToNode.coord, toNode.coord);
								else
									length += CoordUtils.calcDistance(lastToNode.coord,
											toNode.coord);
								
								if (toNode.used)
								{
									if (this.hierarchyLayers.isEmpty())
									{
										createLink(this.network, way, fromNode,
												toNode, length);
									} else
									{
										for (OsmFilter osmFilter : this.hierarchyLayers)
										{
											if (osmFilter.coordInFilter(
													fromNode.coord, way.hierarchy))
											{
												createLink(this.network, way,
														fromNode, toNode, length);
												break;
											}
											if (osmFilter.coordInFilter(
													toNode.coord, way.hierarchy))
											{
												createLink(this.network, way,
														fromNode, toNode, length);
												break;
											}
										}
									}
									fromNode = toNode;
									length = 0.0;
								}
								lastToNode = toNode;
							}
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
		String highway = way.tags.get(TAG_HIGHWAY);
		
		
		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		
		long id = this.id;
		String origId = Long.toString(way.id);
		double nofLanes = 0;
		double laneCapacity = 0;
		double freespeed = 0;
		double freespeedFactor = 0;
		boolean oneway = false;
		boolean onewayReverse = false;
		double capacity = 0;
		
		if (defaults!=null)
		{
			nofLanes = defaults.lanes;
			laneCapacity = defaults.laneCapacity;
			freespeed = defaults.freespeed;
			freespeedFactor = defaults.freespeedFactor;
			oneway = defaults.oneway;
			onewayReverse = false;
			
			
			// check if there are tags that overwrite defaults
			// - check tag "junction"
			if ("roundabout".equals(way.tags.get(TAG_JUNCTION)))
			{
				// if "junction" is not set in tags, get() returns null and equals()
				// evaluates to false
				oneway = true;
			}

			
			
			// check tag "oneway"
			String onewayTag = way.tags.get(TAG_ONEWAY);
			if (onewayTag != null)
			{
				if ("yes".equals(onewayTag))
				{
					oneway = true;
				} else if ("true".equals(onewayTag))
				{
					oneway = true;
				} else if ("1".equals(onewayTag))
				{
					oneway = true;
				} else if ("-1".equals(onewayTag))
				{
					onewayReverse = true;
					oneway = false;
				} else if ("no".equals(onewayTag))
				{
					oneway = false; // may be used to overwrite defaults
				}
			}

			// In case trunks, primary and secondary roads are marked as oneway,
			// the default number of lanes should be two instead of one.
			if (highway.equalsIgnoreCase("trunk")
					|| highway.equalsIgnoreCase("primary")
					|| highway.equalsIgnoreCase("secondary"))
			{
				if (oneway && nofLanes == 1.0)
				{
					nofLanes = 2.0;
				}
			}

			String maxspeedTag = way.tags.get(TAG_MAXSPEED);
			if (maxspeedTag != null)
			{
				try
				{
					freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert
																		// km/h to
																		// m/s
				} catch (NumberFormatException e)
				{
					if (!this.unknownMaxspeedTags.contains(maxspeedTag))
					{
						this.unknownMaxspeedTags.add(maxspeedTag);
						log.warn("Could not parse maxspeed tag:" + e.getMessage()
								+ ". Ignoring it.");
					}
				}
			}

			// check tag "lanes"
			String lanesTag = way.tags.get(TAG_LANES);
			if (lanesTag != null)
			{
				try
				{
					double tmp = Double.parseDouble(lanesTag);
					if (tmp > 0)
					{
						nofLanes = tmp;
					}
				} catch (Exception e)
				{
					if (!this.unknownLanesTags.contains(lanesTag))
					{
						this.unknownLanesTags.add(lanesTag);
						log.warn("Could not parse lanes tag:" + e.getMessage()
								+ ". Ignoring it.");
					}
				}
			}

			capacity = nofLanes * laneCapacity;

			if (this.scaleMaxSpeed)
			{
				freespeed = freespeed * freespeedFactor;
			}
		}
		
		
		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		Id fromId = new IdImpl(fromNode.id);
		Id toId = new IdImpl(toNode.id);
		
		while(network.getLinks().containsKey((id)));
		{
			id++;
		}
		
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
				this.id++;
				id++;
			}
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
				this.id++;
			}
		}
	}
		
	private double calculateWGS84Length(Coord coord, Coord coord2)
	{
		double lon1=coord.getX();
		double lat1= coord.getY();
		
		double lon2=coord2.getX();
		double lat2= coord2.getY();
		
		double lat =(lat1+lat2) / 2 * 0.01745;
		double dx = 111.3 * Math.cos(lat) * (lon1 - lon2);
		double dy = 111.3 * (lat1 - lat2);
		
		return Math.sqrt(dx*dx + dy * dy)*1000;
	}
	
	
	

}
