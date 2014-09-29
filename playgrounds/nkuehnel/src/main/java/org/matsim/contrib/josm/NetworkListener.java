package org.matsim.contrib.josm;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Listens to changes in the dataset and their effects on the Network
 * 
 * 
 */
class NetworkListener implements DataSetListener, Visitor {

	private final Logger log = Logger.getLogger(NetworkListener.class);

	private Scenario scenario;

	private Map<Way, List<Link>> way2Links;
	private Map<Link, List<WaySegment>> link2Segments;

	private Map<Relation, TransitRoute> relation2Route;

	public NetworkListener(Scenario scenario, Map<Way, List<Link>> way2Links,
			Map<Link, List<WaySegment>> link2Segments, Map<Relation, TransitRoute> relation2Route)
			throws IllegalArgumentException {
		this.scenario = scenario;
		this.way2Links = way2Links;
		this.link2Segments = link2Segments;
		this.relation2Route = relation2Route;
		log.debug("Listener initialized");
	}

	@Override
	public void dataChanged(DataChangedEvent arg0) {
		log.debug("Data changed. " + arg0.getType());
		if (Main.main.getActiveLayer() != null) {
			Main.main.getCurrentDataSet().clearSelection();
			MATSimPlugin.toggleDialog.activeLayerChange(Main.main.getActiveLayer(), Main.main.getActiveLayer());
		}
	}

	@Override
	public void nodeMoved(NodeMovedEvent moved) {
		log.debug("Node(s) moved.");
		moved.getNode().visitReferrers(this);
	}

	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent arg0) {
		log.debug("Other dataset change. " + arg0.getType());
	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent added) {
		for (OsmPrimitive primitive : added.getPrimitives()) {
			log.info("Primitive added. "+ primitive.getType() + " " + primitive.getUniqueId());
			if (primitive instanceof Way) {
				visit((Way) primitive);
				primitive.visitReferrers(this);
			} else if (primitive instanceof Relation) {
				visit((Relation) primitive);
			}
		}
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved) {
		for (OsmPrimitive primitive : primitivesRemoved.getPrimitives()) {
			log.info("Primitive removed. "+ primitive.getType() + " " + primitive.getUniqueId());
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				String id = String.valueOf(primitive.getUniqueId());
				if (scenario.getNetwork().getNodes().containsKey(Id.create(id, Node.class))) {
					Node node = scenario.getNetwork().getNodes().get(Id.create(id, Node.class));
					log.debug("MATSim Node removed. " + ((NodeImpl) node).getOrigId());
					scenario.getNetwork().removeNode(node.getId());
				}
				primitive.visitReferrers(this);
			} else if (primitive instanceof Way) {
				if (way2Links.containsKey(primitive)) {
					List<Link> links = way2Links.remove(primitive);
					for (Link link : links) {
						System.out.println(link.getFromNode().getId());
						link2Segments.remove(link);
						log.debug("MATSim Link removed. "
								+ ((LinkImpl) link).getOrigId());
						scenario.getNetwork().removeLink(link.getId());
					}
					way2Links.remove(primitive);
				}
				primitive.visitReferrers(this);
			} else if (primitive instanceof Relation) {
				log.debug("Relation deleted "+ primitive.getUniqueId());
				if(relation2Route.containsKey(primitive)) {
					TransitRoute route = relation2Route.get(primitive);
					for (Id<Link> linkId: route.getRoute().getLinkIds()) {
						if(!link2Segments.containsKey(scenario.getNetwork().getLinks().get(linkId))) {
							scenario.getNetwork().removeLink(linkId);
						}
					}
					
					Id<TransitLine> lineId;
					if (primitive.hasKey("ref")) {
						lineId = Id.create(primitive.get("ref"), TransitLine.class);
					} else if (primitive.hasKey("name")) {
						lineId = Id.create(primitive.get("name"), TransitLine.class);
					} else {
						lineId = Id.create(primitive.getUniqueId(), TransitLine.class);
					}
					
					for (TransitRouteStop stop: route.getStops()) {
						Id<Link> linkId = stop.getStopFacility().getLinkId();
						Link link = scenario.getNetwork().getLinks().get(linkId);
						if (link2Segments.containsKey(link)) {
							scenario.getNetwork().removeLink(linkId);
						}
						scenario.getTransitSchedule().removeStopFacility(stop.getStopFacility());
					}
					
					if (scenario.getTransitSchedule().getTransitLines().containsKey(lineId)) {
						log.debug("line found");
						TransitLine line = scenario.getTransitSchedule().getTransitLines().get(lineId);
						if (line.getRoutes().containsValue(route)) {
						line.removeRoute(route);
						}
						if(line.getRoutes().isEmpty()) {
							scenario.getTransitSchedule().removeTransitLine(line);
						}
					}
					relation2Route.remove(primitive);
				}
			}
		}
		log.info("Number of links: " + scenario.getNetwork().getLinks().size());
		MATSimPlugin.toggleDialog.notifyDataChanged(scenario);
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0) {
		log.debug("Relation member changed " + arg0.getType());
		visit(arg0.getRelation()); 
	}

	@Override
	public void tagsChanged(TagsChangedEvent changed) {
		log.debug("Tags changed " + changed.getType() + " "+ changed.getPrimitive().getType() + " " + changed.getPrimitive().getUniqueId());
		for (OsmPrimitive primitive : changed.getPrimitives()) {
			if (primitive instanceof Way) {
				visit((Way) primitive);
				primitive.visitReferrers(this);
				for (org.openstreetmap.josm.data.osm.Node node : ((Way) primitive)
						.getNodes()) {
					if (node.isReferredByWays(2)) {
						for (OsmPrimitive prim : node.getReferrers()) {
							if (prim instanceof Way && !prim.equals(primitive)) {
								visit((Way) prim);
							}
						}
					}
				}
			} else if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				primitive.visitReferrers(this);
			}
		}
		for(TransitStopFacility stop: scenario.getTransitSchedule().getFacilities().values()) {
			System.out.println(stop.getName());
		}
	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent changed) {
		log.debug("Way Nodes changed " + changed.getType() + " " + changed.getChangedWay().getType() + " " + changed.getChangedWay().getUniqueId());
		for (org.openstreetmap.josm.data.osm.Node node : changed
				.getChangedWay().getNodes()) {
			if (node.isReferredByWays(2)) {
				for (OsmPrimitive prim : node.getReferrers()) {
					if (prim instanceof Way
							&& !prim.equals(changed.getChangedWay())) {
						visit((Way) prim);
					}
				}
			}
		}
		visit(changed.getChangedWay());
	}

	@Override
	public void visit(org.openstreetmap.josm.data.osm.Node arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Way way) {
		if (Main.main.getCurrentDataSet() != null) {
			Main.main.getCurrentDataSet().clearHighlightedWaySegments();
		}
		List<Link> oldLinks = way2Links.remove(way);
		MATSimPlugin.toggleDialog.notifyDataChanged(scenario);
		if (oldLinks != null) {
			for (Link link : oldLinks) {
				Link removedLink = scenario.getNetwork().removeLink(link.getId());
				log.debug(removedLink + " removed.");
			}
		}
		if (!way.isDeleted()) {
			NewConverter.convertWay(way, scenario.getNetwork(), way2Links, link2Segments);
			MATSimPlugin.toggleDialog.notifyDataChanged(scenario);
		}
		log.info("Number of links: " + scenario.getNetwork().getLinks().size());
	}

	@Override
	public void visit(Relation relation) {
		if (!relation.isDeleted() && relation.hasTag("type", new String[] {"route", "matsimRoute"})) {
			if (relation.hasTag("route", new String[] { "train",
					"track", "bus", "light_rail", "tram", "subway" }) || relation.hasTag("type","matsimRoute")) {
				
				if(relation2Route.containsKey(relation)) {
					TransitRoute route = relation2Route.get(relation);
					for (Id<Link> linkId: route.getRoute().getLinkIds()) {
						if(!link2Segments.containsKey(scenario.getNetwork().getLinks().get(linkId))) {
							scenario.getNetwork().removeLink(linkId);
						}
					}
					
					Id<TransitLine> lineId;
					if (relation.hasKey("ref")) {
						lineId = Id.create(relation.get("ref"), TransitLine.class);
					} else if (relation.hasKey("name")) {
						lineId = Id.create(relation.get("name"), TransitLine.class);
					} else {
						lineId = Id.create(relation.getUniqueId(), TransitLine.class);
					}
					
					for (TransitRouteStop stop: route.getStops()) {
						Id<Link> linkId = stop.getStopFacility().getLinkId();
						Link link = scenario.getNetwork().getLinks().get(linkId);
						if (!link2Segments.containsKey(link)) {
							scenario.getNetwork().removeLink(linkId);
						}
						scenario.getTransitSchedule().removeStopFacility(stop.getStopFacility());
					}
					
					if (scenario.getTransitSchedule().getTransitLines().containsKey(lineId)) {
						log.debug("line found");
						TransitLine line = scenario.getTransitSchedule().getTransitLines().get(lineId);
						if (line.getRoutes().containsValue(route)) {
						line.removeRoute(route);
						}
						if(line.getRoutes().isEmpty()) {
							scenario.getTransitSchedule().removeTransitLine(line);
						}
					}
					relation2Route.remove(relation);
				}
				
				if (relation.hasTag("route", new String[] { "train",
						"track", "bus", "light_rail", "tram", "subway" })) {
				
					NewConverter.convertTransitRouteOsm(relation, scenario, relation2Route);
				} else if(relation.hasTag("type", "matsimRoute")) {
					NewConverter.convertTransitRouteMatsim(relation, scenario, way2Links, relation2Route);
				}
				MATSimPlugin.toggleDialog.notifyDataChanged(scenario);
			}
		}

	}

	@Override
	public void visit(Changeset arg0) {
		// TODO Auto-generated method stub

	}
}
