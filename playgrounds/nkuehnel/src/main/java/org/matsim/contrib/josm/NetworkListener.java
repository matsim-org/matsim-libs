package org.matsim.contrib.josm;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
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

	private Network network;

	private Map<Way, List<Link>> way2Links;
	private Map<Link, List<WaySegment>> link2Segments;

	public NetworkListener(Network network, Map<Way, List<Link>> way2Links,
			Map<Link, List<WaySegment>> link2Segments)
			throws IllegalArgumentException {
		this.network = network;
		this.way2Links = way2Links;
		this.link2Segments = link2Segments;
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
			}
		}
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved) {
		for (OsmPrimitive primitive : primitivesRemoved.getPrimitives()) {
			log.info("Primitive removed. "+ primitive.getType() + " " + primitive.getUniqueId());
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				String id = String.valueOf(primitive.getUniqueId());
				if (network.getNodes().containsKey(new IdImpl(id))) {
					Node node = network.getNodes().get(new IdImpl(id));
					log.debug("MATSim Node removed. " + ((NodeImpl) node).getOrigId());
					network.removeNode(node.getId());
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
						network.removeLink(link.getId());
					}
					way2Links.remove(primitive);
				}
			}
		}
		log.info("Number of links: " + network.getLinks().size());
		MATSimPlugin.toggleDialog.notifyDataChanged(network);
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0) {
		log.debug("Relation member changed " + arg0.getType());

	}

	@Override
	public void tagsChanged(TagsChangedEvent changed) {
		log.debug("Tags changed " + changed.getType() + " "+ changed.getPrimitive().getType() + " " + changed.getPrimitive().getUniqueId());
		for (OsmPrimitive primitive : changed.getPrimitives()) {
			if (primitive instanceof Way) {
				visit((Way) primitive);
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
		MATSimPlugin.toggleDialog.notifyDataChanged(network);
		if (oldLinks != null) {
			for (Link link : oldLinks) {
				Link removedLink = network.removeLink(link.getId());
				log.debug(removedLink + " removed.");
			}
		}
		if (!way.isDeleted()) {
			NewConverter.convertWay(way, network, way2Links, link2Segments);
			MATSimPlugin.toggleDialog.notifyDataChanged(network);
		}
		log.info("Number of links: " + network.getLinks().size());
	}

	@Override
	public void visit(Relation arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Changeset arg0) {
		// TODO Auto-generated method stub

	}
}
