package playground.smetzler.bike.old;
//package playground.smetzler.bike;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.core.network.LinkImpl;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.io.OsmNetworkReader;
//import org.matsim.core.utils.io.OsmNetworkReader.OsmHighwayDefaults;
//import org.matsim.core.utils.io.OsmNetworkReader.OsmNode;
//import org.matsim.core.utils.io.OsmNetworkReader.OsmWay;
//
//public class OsmBikeNetworkReader extends OsmNetworkReader {
//
//	public OsmBikeNetworkReader(Network network, CoordinateTransformation transformation) {
//		super(network, transformation);
//		// TODO Auto-generated constructor stub
//		
//
//	}
//
//
//	private void createBikeLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
//		
//	};
//	
//	
//	
//	
//	
//	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
//		
//	String highway = way.tags.get(TAG_HIGHWAY);
//
//    if ("no".equals(way.tags.get(TAG_ACCESS))) {
//         return;
//    }
//	
//	// load defaults
//	OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
//	if (defaults == null) {
//		this.unknownHighways.add(highway);
//		return;
//	}
//
//	double nofLanes = defaults.lanesPerDirection;
//	double laneCapacity = defaults.laneCapacity;
//	double freespeed = defaults.freespeed;
//	double freespeedFactor = defaults.freespeedFactor;
//	boolean oneway = defaults.oneway;
//	boolean onewayReverse = false;
//
//	// check if there are tags that overwrite defaults
//	// - check tag "junction"
//	if ("roundabout".equals(way.tags.get(TAG_JUNCTION))) {
//		// if "junction" is not set in tags, get() returns null and equals() evaluates to false
//		oneway = true;
//	}
//
//	// check tag "oneway"
//	String onewayTag = way.tags.get(TAG_ONEWAY);
//	if (onewayTag != null) {
//		if ("yes".equals(onewayTag)) {
//			oneway = true;
//		} else if ("true".equals(onewayTag)) {
//			oneway = true;
//		} else if ("1".equals(onewayTag)) {
//			oneway = true;
//		} else if ("-1".equals(onewayTag)) {
//			onewayReverse = true;
//			oneway = false;
//		} else if ("no".equals(onewayTag)) {
//			oneway = false; // may be used to overwrite defaults
//        }
//		else {
//            log.warn("Could not interpret oneway tag:" + onewayTag + ". Ignoring it.");
//		}
//	}
//
//    // In case trunks, primary and secondary roads are marked as oneway,
//    // the default number of lanes should be two instead of one.
//    if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")){
//        if((oneway || onewayReverse) && nofLanes == 1.0){
//            nofLanes = 2.0;
//        }
//	}
//
//	String maxspeedTag = way.tags.get(TAG_MAXSPEED);
//	if (maxspeedTag != null) {
//		try {
//			freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert km/h to m/s
//		} catch (NumberFormatException e) {
//			if (!this.unknownMaxspeedTags.contains(maxspeedTag)) {
//				this.unknownMaxspeedTags.add(maxspeedTag);
//				log.warn("Could not parse maxspeed tag:" + e.getMessage() + ". Ignoring it.");
//			}
//		}
//	}
//
//	// check tag "lanes"
//	String lanesTag = way.tags.get(TAG_LANES);
//	if (lanesTag != null) {
//		try {
//			double totalNofLanes = Double.parseDouble(lanesTag);
//			if (totalNofLanes > 0) {
//				nofLanes = totalNofLanes;
//
//				//By default, the OSM lanes tag specifies the total number of lanes in both directions.
//				//So if the road is not oneway (onewayReverse), let's distribute them between both directions
//				//michalm, jan'16
//	            if (!oneway && !onewayReverse) {
//	                nofLanes /= 2.;
//	            }
//			}
//		} catch (Exception e) {
//			if (!this.unknownLanesTags.contains(lanesTag)) {
//				this.unknownLanesTags.add(lanesTag);
//				log.warn("Could not parse lanes tag:" + e.getMessage() + ". Ignoring it.");
//			}
//		}
//	}
//
//	// create the link(s)
//	double capacity = nofLanes * laneCapacity;
//
//	if (this.scaleMaxSpeed) {
//		freespeed = freespeed * freespeedFactor;
//	}
//
//	// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
//	Id<Node> fromId = Id.create(fromNode.id, Node.class);
//	Id<Node> toId = Id.create(toNode.id, Node.class);
//	if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null){
//		String origId = Long.toString(way.id);
//
//		if (!onewayReverse) {
//			Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
//			l.setLength(length);
//			l.setFreespeed(freespeed);
//			l.setCapacity(capacity);
//			l.setNumberOfLanes(nofLanes);
//			if (l instanceof LinkImpl) {
//				((LinkImpl) l).setOrigId(origId);
//			}
//			network.addLink(l);
//			this.id++;
//		}
//		if (!oneway) {
//			Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
//			l.setLength(length);
//			l.setFreespeed(freespeed);
//			l.setCapacity(capacity);
//			l.setNumberOfLanes(nofLanes);
//			if (l instanceof LinkImpl) {
//				((LinkImpl) l).setOrigId(origId);
//			}
//			network.addLink(l);
//			this.id++;
//		}
//
//	}
//	}
//	
//	
//	
//}
//
