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

import josmMatsimPlugin.OsmConvertDefaults.OsmHighwayDefaults;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * 
 * 
 * @author nkuehnel
 */
public class Converter {
	private final static Logger log = Logger.getLogger(Converter.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";

	private final static String[] ALL_TAGS = new String[] { TAG_LANES,
			TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY };

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
	private final Set<String> unknownHighways = new HashSet<String>();
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();
	private long id = 0;
	Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Network network;
	private boolean scaleMaxSpeed = false;
	private boolean keepPaths;

	final List<OsmFilter> hierarchyLayers = new ArrayList<OsmFilter>();

	private DataSet dataSet;

	public Converter(DataSet dataSet, Network network) {
		this.dataSet = dataSet;
		this.network = network;
		this.keepPaths = Main.pref.getBoolean(
				"matsim_convertDefaults_keepPaths", false);
		this.highwayDefaults = OsmConvertDefaults.getDefaults();
	}

	/**
	 * In case the speed limit allowed does not represent the speed a vehicle
	 * can actually realize, e.g. by constrains of traffic lights not explicitly
	 * modeled, a kind of "average simulated speed" can be used.
	 * 
	 * Defaults to <code>false</code>.
	 * 
	 * @param scaleMaxSpeed
	 *            <code>true</code> to scale the speed limit down by the value
	 *            specified by the
	 *            {@link #setHighwayDefaults(int, String, double, double, double, double)
	 *            defaults}.
	 */
	public void setScaleMaxSpeed(final boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	public void convert() {

		writeNodes(dataSet.getNodes());
		writeWays(dataSet.getWays());

		if (this.network instanceof NetworkImpl) {
			((NetworkImpl) this.network).setCapacityPeriod(3600);
		}

		Iterator<Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Long, OsmWay> entry = it.next();
			for (Long nodeId : entry.getValue().nodes) {
				if (this.nodes.get(nodeId) == null) {
					it.remove();
					break;
				}
			}
		}

		// check which nodes are used
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			if ((highway != null)
					&& (this.highwayDefaults.containsKey(highway))) {
				// check to which level a way belongs
				way.hierarchy = this.highwayDefaults.get(highway).hierarchy;

				// first and last are counted twice, so they are kept in all
				// cases
				this.nodes.get(way.nodes.get(0)).ways++;
				this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;

				for (Long nodeId : way.nodes) {
					OsmNode node = this.nodes.get(nodeId);
					if (this.hierarchyLayers.isEmpty()) {
						node.used = true;
						node.ways++;
					} else {
						for (OsmFilter osmFilter : this.hierarchyLayers) {
							if (osmFilter.coordInFilter(node.coord,
									way.hierarchy)) {
								node.used = true;
								node.ways++;
								break;
							}
						}
					}
				}
			}
		}

		if (!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			for (OsmNode node : this.nodes.values()) {
				if ((node.ways == 1) && (!this.keepPaths)) {
					node.used = false;
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
				String highway = way.tags.get(TAG_HIGHWAY);
				if ((highway != null)
						&& (this.highwayDefaults.containsKey(highway))) {
					int prevRealNodeIndex = 0;
					OsmNode prevRealNode = this.nodes.get(way.nodes
							.get(prevRealNodeIndex));

					for (int i = 1; i < way.nodes.size(); i++) {
						OsmNode node = this.nodes.get(way.nodes.get(i));
						if (node.used) {
							if (prevRealNode == node) {
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
								for (double j = nextNodeToKeep; j < i; j += increment) {
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
		for (OsmNode node : this.nodes.values()) {
			if (node.used) {
				Node nn = this.network.getFactory().createNode(
						new IdImpl(node.id), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			if (highway != null) {
				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
				double length = 0.0;
				OsmNode lastToNode = fromNode;
				if (fromNode.used) {
					for (int i = 1, n = way.nodes.size(); i < n; i++) {
						OsmNode toNode = this.nodes.get(way.nodes.get(i));
						if (toNode != lastToNode) {
							length += OsmConvertDefaults.calculateWGS84Length(lastToNode.coord,
									toNode.coord);
							if (toNode.used) {

								if (this.hierarchyLayers.isEmpty()) {
									createLink(this.network, way, fromNode,
											toNode, length);
								} else {
									for (OsmFilter osmFilter : this.hierarchyLayers) {
										if (osmFilter.coordInFilter(
												fromNode.coord, way.hierarchy)) {
											createLink(this.network, way,
													fromNode, toNode, length);
											break;
										}
										if (osmFilter.coordInFilter(
												toNode.coord, way.hierarchy)) {
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

		log.info("= conversion statistics: ==========================");
		log.info("MATSim: # nodes created: " + this.network.getNodes().size());
		log.info("MATSim: # links created: " + this.network.getLinks().size());

		if (this.unknownHighways.size() > 0) {
			log.info("The following highway-types had no defaults set and were thus NOT converted:");
			for (String highwayType : this.unknownHighways) {
				log.info("- \"" + highwayType + "\"");
			}
		}
		log.info("= end of conversion statistics ====================");
	}

	private void createLink(final Network network, final OsmWay way,
			final OsmNode fromNode, final OsmNode toNode, final double length) {
		String highway = way.tags.get(TAG_HIGHWAY);

		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null) {
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
		if ("roundabout".equals(way.tags.get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals()
			// evaluates to false
			oneway = true;
		}

		// check tag "oneway"
		String onewayTag = way.tags.get(TAG_ONEWAY);
		if (onewayTag != null) {
			if ("yes".equals(onewayTag)) {
				oneway = true;
			} else if ("true".equals(onewayTag)) {
				oneway = true;
			} else if ("1".equals(onewayTag)) {
				oneway = true;
			} else if ("-1".equals(onewayTag)) {
				onewayReverse = true;
				oneway = false;
			} else if ("no".equals(onewayTag)) {
				oneway = false; // may be used to overwrite defaults
			}
		}

		// In case trunks, primary and secondary roads are marked as oneway,
		// the default number of lanes should be two instead of one.
		if (highway.equalsIgnoreCase("trunk")
				|| highway.equalsIgnoreCase("primary")
				|| highway.equalsIgnoreCase("secondary")) {
			if (oneway && nofLanes == 1.0) {
				nofLanes = 2.0;
			}
		}

		String maxspeedTag = way.tags.get(TAG_MAXSPEED);
		if (maxspeedTag != null) {
			try {
				freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert
				// km/h to
				// m/s
			} catch (NumberFormatException e) {
				if (!this.unknownMaxspeedTags.contains(maxspeedTag)) {
					this.unknownMaxspeedTags.add(maxspeedTag);
					log.warn("Could not parse maxspeed tag:" + e.getMessage()
							+ ". Ignoring it.");
				}
			}
		}

		// check tag "lanes"
		String lanesTag = way.tags.get(TAG_LANES);
		if (lanesTag != null) {
			try {
				double tmp = Double.parseDouble(lanesTag);
				if (tmp > 0) {
					nofLanes = tmp;
				}
			} catch (Exception e) {
				if (!this.unknownLanesTags.contains(lanesTag)) {
					this.unknownLanesTags.add(lanesTag);
					log.warn("Could not parse lanes tag:" + e.getMessage()
							+ ". Ignoring it.");
				}
			}
		}

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		Id fromId = new IdImpl(fromNode.id);
		Id toId = new IdImpl(toNode.id);
		if (network.getNodes().get(fromId) != null
				&& network.getNodes().get(toId) != null) {
			String origId = Long.toString(way.id);

			if (!onewayReverse) {
				Link l = network.getFactory().createLink(new IdImpl(this.id),
						network.getNodes().get(fromId),
						network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(new IdImpl(this.id),
						network.getNodes().get(toId),
						network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}
		}
	}

	private static class OsmNode {
		public final long id;
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}

	private static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>();
		public final Map<String, String> tags = new HashMap<String, String>();
		public int hierarchy = -1;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	public void writeNodes(
			Collection<org.openstreetmap.josm.data.osm.Node> nodes) {
		for (org.openstreetmap.josm.data.osm.Node n : sortById(nodes)) {
			if (shouldWrite(n)) {
				visit(n);
			}
		}
	}

	protected void writeWays(Collection<Way> ways) {
		for (Way w : sortById(ways)) {
			if (shouldWrite(w)) {
				visit(w);
			}
		}
	}

	public void visit(INode n) {
		if (n.isIncomplete())
			return;
		Long id = n.getUniqueId();
		double lat = n.getCoor().lat();
		double lon = n.getCoor().lon();
		this.nodes.put(id, new OsmNode(id, new CoordImpl(lon, lat)));
	}

	public void visit(IWay w) {
		if (w.isIncomplete())
			return;
		Long id = w.getUniqueId();
		OsmWay way = new OsmWay(id);
		for (int i = 0; i < w.getNodesCount(); ++i) {
			org.openstreetmap.josm.data.osm.Node node = ((Way) w).getNode(i);
			way.nodes.add(node.getUniqueId());
		}
		if (way.nodes.isEmpty()) {
			return;
		}
		for (String tag : ALL_TAGS) {
			for (String ref : w.getKeys().keySet()) {
				if (tag.equals(ref)) {
					way.tags.put(ref, w.getKeys().get(ref));
					break;
				}
			}
		}

		this.ways.put(id, way);
	}

	private boolean shouldWrite(OsmPrimitive osm) {
		return !osm.isNewOrUndeleted() || !osm.isDeleted();
	}

	private final Comparator<OsmPrimitive> byIdComparator = new Comparator<OsmPrimitive>() {
		@Override
		public int compare(OsmPrimitive o1, OsmPrimitive o2) {
			return (o1.getUniqueId() < o2.getUniqueId() ? -1 : (o1
					.getUniqueId() == o2.getUniqueId() ? 0 : 1));
		}
	};

	private <T extends OsmPrimitive> Collection<T> sortById(
			Collection<T> primitives) {
		List<T> result = new ArrayList<T>(primitives.size());
		result.addAll(primitives);
		Collections.sort(result, byIdComparator);
		return result;
	}

	
}
