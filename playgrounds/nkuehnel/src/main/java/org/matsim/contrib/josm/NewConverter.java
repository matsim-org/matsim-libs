package org.matsim.contrib.josm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.josm.OsmConvertDefaults.OsmHighwayDefaults;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;

class NewConverter {
	private final static Logger log = Logger.getLogger(NewConverter.class);
	
	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final static Set<String> unknownLanesTags = new HashSet<String>();

    private static final List<String> TRANSPORT_MODES = Arrays.asList(
			TransportMode.bike, TransportMode.car, TransportMode.other,
			TransportMode.pt, TransportMode.ride, TransportMode.transit_walk,
			TransportMode.walk);
	
	static Map<String, OsmHighwayDefaults> highwayDefaults;
	
	
	public static void convertOsmLayer(DataSet dataSet, Network network, Map<Way, List<Link>> way2Links, Map<Link, List<WaySegment>> link2Segments) {
		if(!dataSet.getWays().isEmpty()) {
			for(Way way: dataSet.getWays()) {
				if(!way.isDeleted()) {
				convertWay(way, network, way2Links, link2Segments);
				}
			}
		}
	}
	

	public static void convertWay(Way way, Network network, Map<Way, List<Link>> way2Links, Map<Link, List<WaySegment>> link2Segments) {
		List<Link> links = new ArrayList<Link>();
		highwayDefaults = OsmConvertDefaults.getDefaults();

		if(way.getNodesCount()<2) {
			return;
		}
		if (way.hasKey(TAG_HIGHWAY) ||  meetsMatsimReq(way.getKeys())) {
			if (highwayDefaults.containsKey(way.getKeys().get(TAG_HIGHWAY)) || meetsMatsimReq(way.getKeys())) {
				
				
				
				List<Node> nodeOrder = new ArrayList<Node>();
				for (int l=0; l<way.getNodesCount(); l++) {
					Node current = way.getNode(l);
					if(l == 0 || l==way.getNodesCount()-1) {
						nodeOrder.add(current);
					} else if ( current.isReferredByWays(2)) {
						for (OsmPrimitive prim: current.getReferrers()) {
							if(prim instanceof Way && !prim.equals(way)) {
								if (((Way) prim).hasKey(TAG_HIGHWAY) || meetsMatsimReq(prim.getKeys())) {
									nodeOrder.add(current);
								}
							}
						}
					} 
				}
				
				for (Node node: nodeOrder) {
					if(!network.getNodes().containsKey(new IdImpl(node.getUniqueId()))) {
						double lat = node.getCoor().lat();
						double lon = node.getCoor().lon();
						org.matsim.api.core.v01.network.Node nn = network.getFactory().createNode(
									new IdImpl(node.getUniqueId()), new CoordImpl(lon, lat));
						((NodeImpl) nn).setOrigId(String.valueOf(node.getUniqueId()));
						network.addNode(nn);
					}
				}
				
				System.out.println(nodeOrder.size()+" nodes.");
				
				long increment = 0;
				for(int k=1; k<nodeOrder.size(); k++) {
					List<WaySegment> segs = new ArrayList<WaySegment>();
					Node nodeFrom = nodeOrder.get(k-1);
					Node nodeTo = nodeOrder.get(k);
					int fromIdx = way.getNodes().indexOf(nodeFrom);
					int toIdx = way.getNodes().indexOf(nodeTo);
					
					double length =0;
					for(int m=fromIdx; m<toIdx; m++) {
						segs.add(new WaySegment(way, m));
						length += way.getNode(m).getCoor().greatCircleDistance(way.getNode(m+1).getCoor());
					}
					List<Link> tempLinks = createLink(network, way, nodeFrom, nodeTo, length, increment);
					for(Link link: tempLinks) {
						link2Segments.put(link, segs);
					}
					links.addAll(tempLinks);
					increment++;
				}
				
				
				
//				segments.add(new WaySegment(way, 0));
//				for (int i = 1; i < way.getRealNodesCount()-1; i++) {
//					segments.add(new WaySegment(way, i));
//				}
//				
//				if (way.firstNode().equals(way.lastNode())) {
//					segments.add(new WaySegment(way, way.getRealNodesCount()-1));
//				}
//				
//				System.out.println("Segments: " + segments.size());
//				
//				for(WaySegment segment: segments) {
//					Coord first = new CoordImpl(segment.getFirstNode().getCoor().lon(), segment.getFirstNode().getCoor().lat());
//					Coord second = new CoordImpl(segment.getSecondNode().getCoor().lon(), segment.getSecondNode().getCoor().lat());
//					Double length = OsmConvertDefaults.calculateWGS84Length(
//							first, second);
//					createLink(network, segment,
//							segment.getFirstNode(), segment.getSecondNode(), length, links, id, link2Segment);
//				}
			}
		}
		way2Links.put(way, links);
	}
	
	private static boolean meetsMatsimReq(Map<String, String> keys) {
		if (!keys.containsKey("capacity"))
			return false;
		if (!keys.containsKey("freespeed"))
			return false;
		if (!keys.containsKey("permlanes"))
			return false;
		if (!keys.containsKey("modes"))
			return false;
		return true;
	}
	
	private static Double parseDoubleIfPossible(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<Link> createLink(final Network network, final Way way,
			final Node fromNode, final Node toNode, double length, long increment) {
		
		Double capacity = 0.;
		Double freespeed = 0.;
		Double nofLanes = 0.;
		boolean oneway = true;
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		boolean onewayReverse = false;
		
		Map<String, String> keys = way.getKeys();
		if(keys.containsKey(TAG_HIGHWAY)) {
			String highway = keys.get(TAG_HIGHWAY);
			
			// load defaults
			OsmHighwayDefaults defaults = highwayDefaults.get(highway);
			if (defaults != null) {
				
				nofLanes = defaults.lanes;
				double laneCapacity = defaults.laneCapacity;
				freespeed = defaults.freespeed;
				oneway = defaults.oneway;

				// check if there are tags that overwrite defaults
				// - check tag "junction"
				if ("roundabout".equals(keys.get(TAG_JUNCTION))) {
					// if "junction" is not set in tags, get() returns null and equals()
					// evaluates to false
					oneway = true;
				}

				// check tag "oneway"
				String onewayTag = keys.get(TAG_ONEWAY);
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

				String maxspeedTag = keys.get(TAG_MAXSPEED);
				if (maxspeedTag != null) {
					try {
						freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert
						// km/h to
						// m/s
					} catch (NumberFormatException e) {
						if (!unknownMaxspeedTags.contains(maxspeedTag)) {
							unknownMaxspeedTags.add(maxspeedTag);
							log.warn("Could not parse maxspeed tag:" + e.getMessage()
									+ ". Ignoring it.");
						}
					}
				}

				// check tag "lanes"
				String lanesTag = keys.get(TAG_LANES);
				if (lanesTag != null) {
					try {
						double tmp = Double.parseDouble(lanesTag);
						if (tmp > 0) {
							nofLanes = tmp;
						}
					} catch (Exception e) {
						if (!unknownLanesTags.contains(lanesTag)) {
							unknownLanesTags.add(lanesTag);
							log.warn("Could not parse lanes tag:" + e.getMessage()
									+ ". Ignoring it.");
						}
					}
				}
				// create the link(s)
				capacity = nofLanes * laneCapacity;
			}
		}
		if(keys.containsKey("capacity")) {
			Double capacityTag = parseDoubleIfPossible(keys.get("capacity"));
			if (capacityTag != null) {
				capacity = capacityTag;
			}
		}
		if(keys.containsKey("freespeed")) {
			Double freespeedTag = parseDoubleIfPossible(keys.get("freespeed"));
			if (freespeedTag != null) {
				freespeed = freespeedTag;
			}
		}
		if(keys.containsKey("permlanes")) {
			Double permlanesTag = parseDoubleIfPossible(keys.get("permlanes"));
			if (permlanesTag != null) {
				nofLanes = permlanesTag;
			}
		}
		if(keys.containsKey("modes")) {
			Set<String> tempModes = new HashSet<String>();
			String tempArray[] = keys.get("modes").split(";");
            for (String mode : tempArray) {
                if (TRANSPORT_MODES.contains(mode)) {
                    tempModes.add(mode);
                }
            }
			if (tempModes.size() != 0) {
				modes.clear();
				modes.addAll(tempModes);
			}
		}
		if(keys.containsKey("length")) {
			Double temp = parseDoubleIfPossible(keys.get("length"));
			if(temp != null) {
				length = temp;
			}
		}
		
		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		List<Link> links = new ArrayList<Link>();
		Id fromId = new IdImpl(fromNode.getUniqueId());
		Id toId = new IdImpl(toNode.getUniqueId());
		if (network.getNodes().get(fromId) != null
				&& network.getNodes().get(toId) != null) {
			String origId = way.getUniqueId()+"_"+(increment);
			if (keys.containsKey(ImportTask.WAY_TAG_ID)) {
					origId = keys.get(ImportTask.WAY_TAG_ID)+"_"+(increment);
			}
			if (!onewayReverse) {
				Link l = network.getFactory().createLink(new IdImpl(Long.toString(way.getUniqueId())+"_"+(increment)),
						network.getNodes().get(fromId),
						network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				links.add(l);
				System.out.println(l.toString());
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(new IdImpl(Long.toString(way.getUniqueId())+"_"+(increment)+"_r"),
						network.getNodes().get(toId),
						network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId+"_r");
				}
				network.addLink(l);
				links.add(l);
				System.out.println(l.toString());
			}
		}
		return links;
	}
}




