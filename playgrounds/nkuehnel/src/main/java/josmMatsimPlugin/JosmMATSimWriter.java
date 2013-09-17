package josmMatsimPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Counter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Save the dataset as a matsim intern network-xml format.
 * 
 * @author nkuehnel
 */

public class JosmMATSimWriter implements PrimitiveVisitor, MatsimWriter
{
	private final static Logger log = Logger.getLogger(JosmMATSimWriter.class);

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();

	private Network network;
	private CoordinateTransformation transform;

	final Counter nodeCounter = new Counter("node ");
	final Counter wayCounter = new Counter("way ");

	private final Set<String> unknownHighways = new HashSet<String>();
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();

	private boolean scaleMaxSpeed = false;

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static String[] ALL_TAGS = new String[]
	{ TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY };

	final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	final List<OsmFilter> hierarchyLayers = new ArrayList<OsmFilter>();

	private boolean keepPaths = false;
	private long id = 0;

	/**
	 * Do not call this directly. Use OsmWriterFactory instead.
	 */
	protected JosmMATSimWriter(final Network network,
			final CoordinateTransformation transformation, boolean useHighwayDefaults)
	{
		this.network = network;
		this.transform = transformation;
		
		if (useHighwayDefaults) {
			log.info("Falling back to default values.");
			this.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			this.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
		}
	}
	
	

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links, assuming it is no oneway road.
	 *
	 * @param hierarchy The hierarchy layer the highway appears.
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 *
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final int hierarchy , final String highwayType, final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}
	
	
	
	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param hierarchy The hierarchy layer the highway appears in.
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway <code>true</code> to say that this road is a oneway road
	 */
	public void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanes, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(hierarchy, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}
	
	
	
	

	public void writeLayer(OsmDataLayer layer)
	{
		writeContent(layer.data);
	}

	/**
	 * Writes the contents of the given dataset (nodes, then ways)
	 * 
	 * @param ds
	 *            The dataset to write
	 */
	public void writeContent(DataSet ds)
	{
		writeNodes(ds.getNodes());
		writeWays(ds.getWays());
		convert();
	}

	/**
	 * Writes the given nodes sorted by id
	 * 
	 * @param nodes
	 *            The nodes to write
	 * @since 5737
	 */
	public void writeNodes(Collection<Node> nodes)
	{
		for (Node n : sortById(nodes))
		{
			if (shouldWrite(n))
			{
				visit(n);
			}
		}
	}

	/**
	 * Writes the given ways sorted by id
	 * 
	 * @param ways
	 *            The ways to write
	 * @since 5737
	 */
	public void writeWays(Collection<Way> ways)
	{
		for (Way w : sortById(ways))
		{
			if (shouldWrite(w))
			{
				visit(w);
			}
		}
	}

	protected boolean shouldWrite(OsmPrimitive osm)
	{
		return !osm.isNewOrUndeleted() || !osm.isDeleted();
	}

	@Override
	public void visit(INode n)
	{
		if (n.isIncomplete())
			return;
		Long id = n.getUniqueId();
		double lat = n.getCoor().lat();
		double lon = n.getCoor().lon();
		this.nodes.put(
				id,
				new OsmNode(id, this.transform
						.transform(new CoordImpl(lon, lat))));
		this.nodeCounter.incCounter();
	}

	@Override
	public void visit(IWay w)
	{
		if (w.isIncomplete())
			return;
		Long id = w.getUniqueId();
		OsmWay way = new OsmWay(id);
		for (int i = 0; i < w.getNodesCount(); ++i)
		{
			way.nodes.add(w.getNodeId(i));
		}

		for (String tag : ALL_TAGS)
		{
			for (String ref : w.getKeys().keySet())
			{
				if (tag.equals(ref))
				{
					way.tags.put(ref, w.getKeys().get(ref));
					break;
				}
			}
		}

		this.ways.put(id, way);
	}

	@Override
	public void visit(IRelation e)
	{

	}

	protected static final Comparator<OsmPrimitive> byIdComparator = new Comparator<OsmPrimitive>()
	{
		@Override
		public int compare(OsmPrimitive o1, OsmPrimitive o2)
		{
			return (o1.getUniqueId() < o2.getUniqueId() ? -1 : (o1
					.getUniqueId() == o2.getUniqueId() ? 0 : 1));
		}
	};

	protected <T extends OsmPrimitive> Collection<T> sortById(
			Collection<T> primitives)
	{
		List<T> result = new ArrayList<T>(primitives.size());
		result.addAll(primitives);
		Collections.sort(result, byIdComparator);
		return result;
	}

	private void convert()
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
				System.out.println("Test 2");
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

	private static class OsmNode
	{
		public final long id;
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;

		public OsmNode(final long id, final Coord coord)
		{
			this.id = id;
			this.coord = coord;
		}
	}

	private static class OsmWay
	{
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>(4);
		public final Map<String, String> tags = new HashMap<String, String>(4);
		public int hierarchy = -1;

		public OsmWay(final long id)
		{
			this.id = id;
		}
	}

	private static class OsmHighwayDefaults
	{

		public final int hierarchy;
		public final double lanes;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final int hierarchy, final double lanes,
				final double freespeed, final double freespeedFactor,
				final double laneCapacity, final boolean oneway)
		{
			this.hierarchy = hierarchy;
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}

	private static class OsmFilter
	{
		private final Coord coordNW;
		private final Coord coordSE;
		private final int hierarchy;

		public OsmFilter(final Coord coordNW, final Coord coordSE,
				final int hierarchy)
		{
			this.coordNW = coordNW;
			this.coordSE = coordSE;
			this.hierarchy = hierarchy;
		}

		public boolean coordInFilter(final Coord coord, final int hierarchyLevel)
		{
			if (this.hierarchy < hierarchyLevel)
			{
				return false;
			}

			return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE
					.getX()) && (this.coordNW.getY() > coord.getY() && coord
					.getY() > this.coordSE.getY()));
		}
	}

	private void createLink(final Network network, final OsmWay way,
			final OsmNode fromNode, final OsmNode toNode, final double length)
	{
		String highway = way.tags.get(TAG_HIGHWAY);

		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null)
		{
			this.unknownHighways.add(highway);
			return;
		}

		double nofLanes = defaults.lanes;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		double freespeedFactor = defaults.freespeedFactor;
		boolean oneway = defaults.oneway;
		boolean onewayReverse = false;

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

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (this.scaleMaxSpeed)
		{
			freespeed = freespeed * freespeedFactor;
		}

		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		Id fromId = new IdImpl(fromNode.id);
		Id toId = new IdImpl(toNode.id);
		if (network.getNodes().get(fromId) != null
				&& network.getNodes().get(toId) != null)
		{
			String origId = Long.toString(way.id);

			if (!onewayReverse)
			{
				Link l = network.getFactory().createLink(new IdImpl(this.id),
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
			}
			if (!oneway)
			{
				Link l = network.getFactory().createLink(new IdImpl(this.id),
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
	
	@Override
	public void write(String filename)
	{
		new org.matsim.core.network.NetworkWriter(network)
				.writeFileV1(filename);
	}
}
