package org.matsim.contrib.josm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.josm.OsmConvertDefaults.OsmHighwayDefaults;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

class NewConverter {
	private final static Logger log = Logger.getLogger(NewConverter.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";

	static boolean keepPaths = Main.pref.getBoolean("matsim_keepPaths", false);

	private static final List<String> TRANSPORT_MODES = Arrays.asList(
			TransportMode.bike, TransportMode.car, TransportMode.other,
			TransportMode.pt, TransportMode.ride, TransportMode.transit_walk,
			TransportMode.walk);

	static Map<String, OsmHighwayDefaults> highwayDefaults;

	// converts complete data layer and fills the given MATSim data structures
	// as well as data mappings
	public static void convertOsmLayer(OsmDataLayer layer, Scenario scenario,
			Map<Way, List<Link>> way2Links,
			Map<Link, List<WaySegment>> link2Segments,
			Map<Relation, TransitRoute> relation2Route) {
		log.info("=== Starting conversion of Osm data ===");
		log.setLevel(Level.WARN);

		// could be used for area filtering in future releases
		// List<JoinedPolygon> polygons = new ArrayList<JoinedPolygon>();
		// for (Way way : layer.data.getWays()) {
		// if (way.isClosed() && way.hasTag("matsim:convert_Area", "active")) {
		// polygons.add(new MultipolygonBuilder.JoinedPolygon(way));
		// }
		// }

		// convert single way
		if (!layer.data.getWays().isEmpty()) {
			for (Way way : layer.data.getWays()) {
				if (!way.isDeleted() /* && isInArea(polygons, way) */) {
					convertWay(way, scenario.getNetwork(), way2Links,
							link2Segments);

				}
			}

			List<Relation> publicTransportRoutesOsm = new ArrayList<Relation>();
			List<Relation> publicTransportRoutesMatsim = new ArrayList<Relation>();

			// check which relations should be converted to routes differed by
			// matsim and osm tag scheme
			for (Relation relation : layer.data.getRelations()) {
				if (!relation.isDeleted()
						&& relation.hasTag("type", new String[] { "route",
								"matsimRoute" })) {
					if (relation.hasTag("route", new String[] { "train",
							"track", "bus", "light_rail", "tram", "subway" })) {
						// if (relation.hasIncompleteMembers()) {
						// DownloadRelationMemberTask task = new
						// DownloadRelationMemberTask(
						// relation, relation.getIncompleteMembers(),
						// layer);
						// task.run();
						// }
						publicTransportRoutesOsm.add(relation);
					} else if (relation.hasTag("type", "matsimRoute")) {
						publicTransportRoutesMatsim.add(relation);
					}
				}
			}

			// sort elements by the way they are linked to each other
			RelationSorter sorter = new RelationSorter();

			// convert osm tagged relations
			for (Relation relation : publicTransportRoutesOsm) {
				sorter.sortMembers(relation.getMembers());
				convertTransitRouteOsm(relation, scenario, relation2Route);
			}
			// convert matsim tagged relations
			for (Relation relation : publicTransportRoutesMatsim) {
				sorter.sortMembers(relation.getMembers());
				convertTransitRouteMatsim(relation, scenario, way2Links,
						relation2Route);
			}

		}
		log.info("=== End of Conversion. #Links: "
				+ scenario.getNetwork().getLinks().size() + " | #Nodes: "
				+ scenario.getNetwork().getNodes().size() + " ===");
	}

	// private static boolean isInArea(List<JoinedPolygon> polygons, Way way) {
	//
	// for (JoinedPolygon polygon: polygons) {
	// for (Node node: way.getNodes()) {
	// if ((Geometry.nodeInsidePolygon(node, polygon.getNodes()))) {
	// return true;
	// }
	// }
	// }
	// return false;
	// }

	public static void convertWay(Way way, Network network,
			Map<Way, List<Link>> way2Links,
			Map<Link, List<WaySegment>> link2Segments) {
		log.info("### Way " + way.getUniqueId() + " (" + way.getNodesCount()
				+ " nodes) ###");
		List<Link> links = new ArrayList<Link>();
		highwayDefaults = OsmConvertDefaults.getDefaults();
		if (way.getNodesCount() > 1) {
			if (way.hasTag(TAG_HIGHWAY, highwayDefaults.keySet())
					|| meetsMatsimReq(way.getKeys())) {
				List<Node> nodeOrder = new ArrayList<Node>();
				StringBuilder nodeOrderLog = new StringBuilder();
				for (int l = 0; l < way.getNodesCount(); l++) {
					Node current = way.getNode(l);
					if (current.getDataSet() == null) {
						continue;
					}
					if (l == 0 || l == way.getNodesCount() - 1 || keepPaths) {
						nodeOrder.add(current);
						log.debug("--- Way " + way.getUniqueId()
								+ ": dumped node " + l + " ("
								+ current.getUniqueId() + ") ");
						nodeOrderLog.append("(" + l + ") ");
					} else if (current
							.equals(way.getNode(way.getNodesCount() - 1))) {
						nodeOrder.add(current); // add node twice if it occurs
												// twice in a loop so length
												// to this node is not
												// calculated wrong
						log.debug("--- Way " + way.getUniqueId()
								+ ": dumped node " + l + " ("
								+ current.getUniqueId()
								+ ") beginning of loop / closed area ");
						nodeOrderLog.append("(" + l + ") ");
					} else if (current.isConnectionNode()) {
						for (OsmPrimitive prim : current.getReferrers()) {
							if (prim instanceof Way && !prim.equals(way)) {
								if (((Way) prim).hasKey(TAG_HIGHWAY)
										|| meetsMatsimReq(prim.getKeys())) {
									nodeOrder.add(current);
									log.debug("--- Way " + way.getUniqueId()
											+ ": dumped node " + l + " ("
											+ current.getUniqueId()
											+ ") way intersection");
									nodeOrderLog.append("(" + l + ") ");
									break;
								}
							}
						}
					}
				}

				log.debug("--- Way " + way.getUniqueId()
						+ ": order of kept nodes [ " + nodeOrderLog.toString()
						+ "]");

				for (Node node : nodeOrder) {
					checkNode(network, node);

					log.debug("--- Way " + way.getUniqueId()
							+ ": created / updated MATSim node "
							+ node.getUniqueId());
				}

				Double length = 0.;
				Double capacity = 0.;
				Double freespeed = 0.;
				Double nofLanes = 0.;
				boolean oneway = true;
				Set<String> modes = new HashSet<String>();
				boolean onewayReverse = false;

				Map<String, String> keys = way.getKeys();
				if (keys.containsKey(TAG_HIGHWAY)) {
					String highway = keys.get(TAG_HIGHWAY);

					// load defaults
					OsmHighwayDefaults defaults = highwayDefaults.get(highway);
					if (defaults != null) {

						if (defaults.hierarchy > Main.pref.getInteger(
								"matsim_filter_hierarchy", 6)) {
							return;
						}
						nofLanes = defaults.lanes;
						double laneCapacity = defaults.laneCapacity;
						freespeed = defaults.freespeed;
						oneway = defaults.oneway;

						// check if there are tags that overwrite defaults
						// - check tag "junction"
						if ("roundabout".equals(keys.get(TAG_JUNCTION))) {
							// if "junction" is not set in tags, get()
							// returns null and
							// equals()
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
								oneway = false; // may be used to overwrite
												// defaults
							} else {
								log.warn("--- Way " + way.getUniqueId()
										+ ": could not parse oneway tag");
							}
						}

						// In case trunks, primary and secondary roads are
						// marked as
						// oneway,
						// the default number of lanes should be two instead
						// of one.
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
								log.warn("--- Way " + way.getUniqueId()
										+ ": could not parse maxspeed tag");
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
								log.warn("--- Way " + way.getUniqueId()
										+ ": could not parse lanes tag");
							}
						}
						// create the link(s)
						capacity = nofLanes * laneCapacity;
					}
				}
				if (keys.containsKey("capacity")) {
					Double capacityTag = parseDoubleIfPossible(keys
							.get("capacity"));
					if (capacityTag != null) {
						capacity = capacityTag;
					} else {
						log.warn("--- Way " + way.getUniqueId()
								+ ": could not parse MATSim capacity tag");
					}
				}
				if (keys.containsKey("freespeed")) {
					Double freespeedTag = parseDoubleIfPossible(keys
							.get("freespeed"));
					if (freespeedTag != null) {
						freespeed = freespeedTag;
					} else {
						log.warn("--- Way " + way.getUniqueId()
								+ ": could not parse MATSim freespeed tag");
					}
				}
				if (keys.containsKey("permlanes")) {
					Double permlanesTag = parseDoubleIfPossible(keys
							.get("permlanes"));
					if (permlanesTag != null) {
						nofLanes = permlanesTag;
					} else {
						log.warn("--- Way " + way.getUniqueId()
								+ ": could not parse MATSim permlanes tag");
					}
				}
				if (keys.containsKey("modes")) {
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
					} else {
						log.warn("--- Way " + way.getUniqueId()
								+ ": could not parse MATSim modes tag");
					}
				}

				Double tempLength = null;
				if (keys.containsKey("length")) {
					Double temp = parseDoubleIfPossible(keys.get("length"));
					if (temp != null) {
						tempLength = temp;

					} else {
						log.warn("--- Way " + way.getUniqueId()
								+ ": could not parse MATSim length tag");
					}
				}

				if (modes.isEmpty()) {
					modes.add(TransportMode.car);
				}

				long increment = 0;
				for (int k = 1; k < nodeOrder.size(); k++) {
					List<WaySegment> segs = new ArrayList<WaySegment>();
					Node nodeFrom = nodeOrder.get(k - 1);
					Node nodeTo = nodeOrder.get(k);

					if (nodeFrom.equals(nodeTo) && !keepPaths) {
						// skip uninteresting loop
						log.warn("--- Way " + way.getUniqueId()
								+ ": contains loose loop / closed area.");
						break;
					}

					int fromIdx = way.getNodes().indexOf(nodeFrom);
					int toIdx = way.getNodes().indexOf(nodeTo);
					if (fromIdx > toIdx) { // loop, take latter occurrence
						toIdx = way.getNodes().lastIndexOf(nodeTo);
					}

					length = 0.;
					for (int m = fromIdx; m < toIdx; m++) {
						segs.add(new WaySegment(way, m));
						length += way
								.getNode(m)
								.getCoor()
								.greatCircleDistance(
										way.getNode(m + 1).getCoor());
					}
					log.debug("--- Way " + way.getUniqueId()
							+ ": length between " + fromIdx + " and " + toIdx
							+ ": " + length);
					if (tempLength != null) {
						length = tempLength * length / way.getLength();
					}
					List<Link> tempLinks = createLink(network, way, nodeFrom,
							nodeTo, length, increment, oneway, onewayReverse,
							freespeed, capacity, nofLanes, modes);
					for (Link link : tempLinks) {
						link2Segments.put(link, segs);
					}
					links.addAll(tempLinks);
					increment++;
				}
			}

		}
		log.debug("### Finished Way " + way.getUniqueId() + ". " + links.size()
				+ " links resulted. ###");
		if (way == null || links.isEmpty() || links == null) {
			return;
		} else {
			way2Links.put(way, links);
		}
	}

	// create or update matsim node
	private static void checkNode(Network network, Node node) {
		Id<Node> nodeId = Id.create(node.getUniqueId(), Node.class);
		if (!node.isIncomplete()) {
			if (!network.getNodes().containsKey(nodeId)) {
				double lat = node.getCoor().lat();
				double lon = node.getCoor().lon();
				org.matsim.api.core.v01.network.Node nn = network
						.getFactory()
						.createNode(
								Id.create(
										node.getUniqueId(),
										org.matsim.api.core.v01.network.Node.class),
								new CoordImpl(lon, lat));
				if (node.hasKey(ImportTask.NODE_TAG_ID)) {
					((NodeImpl) nn).setOrigId(node.get(ImportTask.NODE_TAG_ID));
				} else {
					((NodeImpl) nn).setOrigId(nn.getId().toString());
				}
				network.addNode(nn);
			} else {
				if (node.hasKey(ImportTask.NODE_TAG_ID)) {
					((NodeImpl) network.getNodes().get(nodeId)).setOrigId(node
							.get(ImportTask.NODE_TAG_ID));
				} else {
					((NodeImpl) network.getNodes().get(nodeId))
							.setOrigId(String.valueOf(node.getUniqueId()));
				}
				Coord coord = new CoordImpl(node.getCoor().getX(), node
						.getCoor().getY());
				((NodeImpl) network.getNodes().get(nodeId)).setCoord(coord);
			}
		}
	}

	// create stop facility at position of given node, if already used by
	// another route, duplicate stop with incremental id
	private static void checkStopFacility(Scenario scenario, Node node,
			List<TransitStopFacility> stops, Id<TransitStopFacility> stopId) {

		int i = 0;
		while (scenario.getTransitSchedule().getFacilities()
				.containsKey(stopId)) {
			stopId = Id.create("(" + i + ")_" + stopId.toString(),
					TransitStopFacility.class);
			i++;
		}
		double lat = node.getCoor().lat();
		double lon = node.getCoor().lon();
		TransitStopFacility stop = scenario
				.getTransitSchedule()
				.getFactory()
				.createTransitStopFacility(stopId, new CoordImpl(lon, lat),
						true);

		stop.setName(node.getLocalName());

		scenario.getTransitSchedule().addStopFacility(stop);
		stops.add(stop);
		return;

	}

	public static void convertTransitRouteOsm(Relation relation,
			Scenario scenario, Map<Relation, TransitRoute> relation2Route) {
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		List<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();

		// create stop facilities
		for (RelationMember member : relation.getMembers()) {
			if (member.isNode() && !member.getMember().isIncomplete()) { // stops
																			// are
																			// always
																			// created
																			// as
																			// nodes
				checkNode(scenario.getNetwork(), member.getNode());
				Id<TransitStopFacility> stopId = Id.create(
						relation.getUniqueId() + "_" + member.getUniqueId(),
						TransitStopFacility.class);
				checkStopFacility(scenario, member.getNode(), stops, stopId);
			}
		}

		if (stops.isEmpty()) {
			return;
		}

		// create beeline route between stop facilities
		links = createBeeLineRoute(relation, scenario, stops);

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		Id<TransitLine> lineId;
		if (relation.hasKey("ref")) {
			lineId = Id.create(relation.get("ref"), TransitLine.class);
		} else {
			lineId = Id.create(relation.getUniqueId(), TransitLine.class);
		}

		TransitLine tLine;
		if (!scenario.getTransitSchedule().getTransitLines()
				.containsKey(lineId)) {
			tLine = builder.createTransitLine(lineId);
		} else {
			tLine = scenario.getTransitSchedule().getTransitLines().get(lineId);
		}

		Id<Link> firstLinkId = stops.get(0).getLinkId();
		Id<Link> secondLinkId = stops.get(stops.size() - 1).getLinkId();
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(firstLinkId,
				secondLinkId);

		networkRoute.setLinkIds(firstLinkId, links, secondLinkId);

		ArrayList<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();
		for (TransitStopFacility stop : stops) {
			routeStops.add(builder.createTransitRouteStop(stop, 0, 0));
		}

		TransitRoute tRoute = builder.createTransitRoute(
				Id.create(relation.getUniqueId(), TransitRoute.class),
				networkRoute, routeStops, "pt");

		tLine.addRoute(tRoute);

		schedule.removeTransitLine(tLine);
		schedule.addTransitLine(tLine);
		relation2Route.put(relation, tRoute);
	}

	public static void convertTransitRouteMatsim(Relation relation,
			Scenario scenario, Map<Way, List<Link>> way2Links,
			Map<Relation, TransitRoute> relation2Route) {

		List<Id<Link>> links = new ArrayList<Id<Link>>();
		List<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();

		Way previous = null;
		// create route from ways
		for (RelationMember member : relation.getMembers()) {
			if (member.isWay() && !member.getMember().isIncomplete()) {
				if (way2Links.containsKey(member.getWay()) // all used ways have
															// to refer to one
															// directional
															// MATSim links
						&& !member.getWay().hasKey(TAG_HIGHWAY)) {
					if (previous != null) {
						if (!previous.lastNode().equals(
								member.getWay().firstNode())) {
							for (TransitStopFacility removeStop : stops) {
								scenario.getTransitSchedule()
										.removeStopFacility(removeStop);
							}
							return; // return and delete already created stops
									// of the route if one of the ways is not
									// connected to the previous one
						}
					}
					previous = member.getWay();
					for (Link link : way2Links.get(member.getWay())) { // add
																		// links
																		// referred
																		// by te
																		// given
																		// way
																		// to
																		// the
																		// route
						links.add(link.getId());
					}
					if (member.getWay().hasKey("stopId")) { // create a stop
															// facility at the
															// and of the last
															// link that is
															// referred by this
															// way
						Id<TransitStopFacility> stopId = (Id.create(member
								.getWay().get("stopId"),
								TransitStopFacility.class));
						if (scenario.getTransitSchedule().getFacilities()
								.containsKey(stopId)) {
							for (TransitStopFacility removeStop : stops) {
								scenario.getTransitSchedule()
										.removeStopFacility(removeStop);
							}
							return;
						}
						double lat = member.getWay().lastNode().getCoor().lat();
						double lon = member.getWay().lastNode().getCoor().lon();
						TransitStopFacility stop = scenario
								.getTransitSchedule()
								.getFactory()
								.createTransitStopFacility(stopId,
										new CoordImpl(lon, lat), true);
						if (member.getWay().hasKey("stopName")) {
							stop.setName(member.getWay().get("stopName"));
						}
						List<Link> wayLinks = way2Links.get(member.getWay());
						stop.setLinkId(wayLinks.get(wayLinks.size() - 1)
								.getId());
						scenario.getTransitSchedule().addStopFacility(stop);
						stops.add(stop);
					}
				} else {
					return;
				}
			}
		}
		if (links.size() < 2) {
			return;
		}
		Id<Link> firstLinkId = links.remove(0);
		Id<Link> lastLinkId = links.remove(links.size() - 1);

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		Id<TransitLine> lineId;
		if (relation.hasKey("name")) {
			lineId = Id.create(relation.get("name"), TransitLine.class);
		} else {
			return;
		}

		TransitLine tLine;
		if (!scenario.getTransitSchedule().getTransitLines()
				.containsKey(lineId)) {
			tLine = builder.createTransitLine(lineId);
		} else {
			tLine = scenario.getTransitSchedule().getTransitLines().get(lineId);
		}

		NetworkRoute networkRoute = new LinkNetworkRouteImpl(firstLinkId,
				lastLinkId);

		networkRoute.setLinkIds(firstLinkId, links, lastLinkId);

		ArrayList<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();
		for (TransitStopFacility stop : stops) {
			routeStops.add(builder.createTransitRouteStop(stop, 0, 0));
		}

		TransitRoute tRoute;
		if (relation.hasKey("routeId")) {
			tRoute = builder.createTransitRoute(
					Id.create(relation.get("routeId"), TransitRoute.class),
					networkRoute, routeStops, "pt");
		} else {
			tRoute = builder.createTransitRoute(
					Id.create(relation.getUniqueId(), TransitRoute.class),
					networkRoute, routeStops, "pt");
		}

		tLine.addRoute(tRoute);

		schedule.removeTransitLine(tLine);
		schedule.addTransitLine(tLine);
		relation2Route.put(relation, tRoute);
	}

	private static List<Id<Link>> createBeeLineRoute(Relation relation,
			Scenario scenario, List<TransitStopFacility> stops) {
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		int increment = 0;
		TransitStopFacility previous = null;

		for (TransitStopFacility stop : stops) {
			if (previous == null) {
				previous = stop; // create dummy link with length=null from and
									// to first stop
			}

			Set<String> mode = Collections.singleton(TransportMode.pt);
			String fromNodeId = previous.getId().toString();
			Node fromNode = (Node) relation.getDataSet().getPrimitiveById(
					Long.parseLong(fromNodeId.substring(fromNodeId
							.lastIndexOf("_") + 1)), OsmPrimitiveType.NODE);
			String toNodeId = stop.getId().toString();
			Node toNode = (Node) relation.getDataSet()
					.getPrimitiveById(
							Long.parseLong(toNodeId.substring(toNodeId
									.lastIndexOf("_") + 1)),
							OsmPrimitiveType.NODE);
			double length = fromNode.getCoor().greatCircleDistance( // beeline
																	// distance
					toNode.getCoor());
			for (Link link : createLink(scenario.getNetwork(), relation,
					fromNode, toNode, length, increment, true, false,
					120 / 3.6, 2000., 1., mode)) {
				stop.setLinkId(link.getId());
				if (increment != 0 && increment != stops.size() - 1) {

					links.add(link.getId());
				}
			}

			previous = stop;
			increment++;
		}
		return links;
	}

	// checks for used MATSim tag scheme
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

	// creates links between given nodes along the respective WaySegments.
	// adapted from original OsmNetworkReader
	private static List<Link> createLink(final Network network,
			final OsmPrimitive primitive, final Node fromNode,
			final Node toNode, double length, long increment, boolean oneway,
			boolean onewayReverse, Double freespeed, Double capacity,
			Double nofLanes, Set<String> modes) {

		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		List<Link> links = new ArrayList<Link>();
		Id<org.matsim.api.core.v01.network.Node> fromId = Id.create(
				fromNode.getUniqueId(),
				org.matsim.api.core.v01.network.Node.class);
		Id<org.matsim.api.core.v01.network.Node> toId = Id.create(
				toNode.getUniqueId(),
				org.matsim.api.core.v01.network.Node.class);
		if (network.getNodes().get(fromId) != null
				&& network.getNodes().get(toId) != null) {

			String id = String.valueOf(primitive.getUniqueId()) + "_"
					+ increment;
			String origId;

			if (primitive instanceof Way
					&& primitive.hasKey(ImportTask.WAY_TAG_ID)) {
				origId = primitive.get(ImportTask.WAY_TAG_ID);
			} else if (primitive instanceof Relation) {
				if (primitive.hasKey("ref")) {
					origId = id + "_" + primitive.get("ref");
				} else {
					origId = id + "_TRANSIT";
				}
			} else {
				origId = id;
			}

			if (!onewayReverse) {
				Link l = network.getFactory().createLink(
						Id.create(id, Link.class),
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
				log.info("--- Way " + primitive.getUniqueId() + ": link "
						+ ((LinkImpl) l).getOrigId() + " created");
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(
						Id.create(id + "_r", Link.class),
						network.getNodes().get(toId),
						network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId + "_r");
				}
				network.addLink(l);
				links.add(l);
				log.info("--- Way " + primitive.getUniqueId() + ": link "
						+ ((LinkImpl) l).getOrigId() + " created");
			}
		}
		return links;
	}
}
