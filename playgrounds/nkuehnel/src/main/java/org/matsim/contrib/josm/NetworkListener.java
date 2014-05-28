package org.matsim.contrib.josm;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NodeImpl;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Listens to changes in the dataset and their effects on the Network
 * 
 * 
 */
class NetworkListener implements DataSetListener {
	private Network network;
	private OsmDataLayer layer;

	private Map<Way, List<Link>> way2Links;
	private Map<Link, List<WaySegment>> link2Segments;

	public NetworkListener(OsmDataLayer layer, Network network,
			Map<Way, List<Link>> way2Links, Map<Link, List<WaySegment>> link2Segments)
			throws IllegalArgumentException {
		this.layer = layer;
		this.network = network;
		this.way2Links = way2Links;
		this.link2Segments = link2Segments;
	}

	@Override
	public void dataChanged(DataChangedEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("data changed");
	}

	@Override
	public void nodeMoved(NodeMovedEvent moved) {

		Id id = new IdImpl(String.valueOf(moved.getNode().getUniqueId()));
		if(!network.getNodes().containsKey(id)) {
			return;
		}
		Node node = network.getNodes().get(id);
		node.getCoord().setXY(moved.getNode().getCoor().lon(),
				moved.getNode().getCoor().lat());
		
		for(OsmPrimitive primitive: moved.getNode().getReferrers()) {
			if(primitive instanceof Way) {
				List<Link> oldLinks = way2Links.remove(primitive);
				if (oldLinks != null) {
					for (Link link : oldLinks) {
						System.out.println("remove because node moved.");
						Link removedLink = network.removeLink(link.getId());
						MATSimPlugin.toggleDialog.notifyDataChanged(network);
						System.out.println(removedLink);
					}
				}
				enterWay2Links((Way) primitive);
				MATSimPlugin.toggleDialog.notifyDataChanged(network);
			}
		}

//		for (Link link : node.getInLinks().values()) {
//			long wayId;
//			String tempId = link.getId().toString();
//			if (tempId.contains("_")) {
//				wayId = Long
//						.parseLong(tempId.substring(0, tempId.indexOf("_")));
//			} else {
//				wayId = Long.parseLong(tempId);
//			}
//			if (!((Way) layer.data
//					.getPrimitiveById(wayId, OsmPrimitiveType.WAY))
//					.hasKey("length")) {
//				link.setLength(OsmConvertDefaults.calculateWGS84Length(link
//						.getFromNode().getCoord(), link.getToNode().getCoord()));
//			}
//		}
//		for (Link link : node.getOutLinks().values()) {
//			long wayId;
//			String tempId = link.getId().toString();
//			if (tempId.contains("_")) {
//				wayId = Long
//						.parseLong(tempId.substring(0, tempId.indexOf("_")));
//			} else {
//				wayId = Long.parseLong(tempId);
//			}
//			if (!((Way) layer.data
//					.getPrimitiveById(wayId, OsmPrimitiveType.WAY))
//					.hasKey("length")) {
//				link.setLength(OsmConvertDefaults.calculateWGS84Length(link
//						.getFromNode().getCoord(), link.getToNode().getCoord()));
//			}
//		}
		MATSimPlugin.toggleDialog.notifyDataChanged(network);
	}

	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("other");
	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent added) {
		System.out.println(added.wasIncomplete());
		for (OsmPrimitive primitive : added.getPrimitives()) {
			if (primitive instanceof Way) {
				Way way = (Way) primitive;

				if (!way2Links.containsKey(way)) {
					enterWay2Links(way);
				}
			}
		}
	}

	private void enterWay2Links(Way way) {
		NewConverter.convertWay(way, network, way2Links,
				link2Segments);
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved) {
		System.out.println(primitivesRemoved.wasComplete());
		for (OsmPrimitive primitive : primitivesRemoved.getPrimitives()) {
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				String id = String.valueOf(primitive.getUniqueId());
				if (network.getNodes().containsKey(new IdImpl(id))) {
					Node node = network.getNodes().get(new IdImpl(id));
					System.out.println("node removed!");
					network.removeNode(node.getId());
				}
			} else if (primitive instanceof Way) {
				if (way2Links.containsKey(primitive)) {
					List<Link> links = way2Links.remove(primitive);
					for (Link link : links) {
						link2Segments.remove(link);
						network.removeLink(link.getId());
						System.out.println("link removed!");
					}
				}
			}
		}
		System.out.println("have links: " + network.getLinks().size());
		MATSimPlugin.toggleDialog.notifyDataChanged(network);
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0) {
		System.out.println("change");

	}

	@Override
	public void tagsChanged(TagsChangedEvent changed) {
		System.out.println(changed.getType().toString());
		for (OsmPrimitive primitive : changed.getPrimitives()) {
			if (primitive instanceof Way) {
				Way way = (Way) primitive;
				List<Link> oldLinks = way2Links.remove(way);
				if (oldLinks != null) {
					for (Link link : oldLinks) {
						System.out.println("remove because tag changed.");
						Link removedLink = network.removeLink(link.getId());
						MATSimPlugin.toggleDialog.notifyDataChanged(network);
						System.out.println(removedLink);
					}
				}
				enterWay2Links(way);
				MATSimPlugin.toggleDialog.notifyDataChanged(network);
			} else if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				org.openstreetmap.josm.data.osm.Node node = (org.openstreetmap.josm.data.osm.Node) primitive;
				Node matsimNode = network.getNodes().get(
						new IdImpl(Long.toString(node.getUniqueId())));
				if (matsimNode != null) {
					if (node.hasKey(ImportTask.NODE_TAG_ID)) {
						((NodeImpl) matsimNode).setOrigId(node
								.get(ImportTask.NODE_TAG_ID));
					} else {
						((NodeImpl) matsimNode).setOrigId(matsimNode.getId()
								.toString());
					}
				}
			}
		}
		System.out.println("have links: " + network.getLinks().size());
	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent changed) {
		for (org.openstreetmap.josm.data.osm.Node node: changed.getChangedWay().getNodes()) {
			if(node.isReferredByWays(2)) {
				for (OsmPrimitive prim: node.getReferrers()) {
					if (prim instanceof Way && !prim.equals(changed.getChangedWay())) {
						List<Link> oldLinks = way2Links.remove(prim);
						if (oldLinks != null) {
							for (Link link : oldLinks) {
								System.out.println("remove because way intersection.");
								Link removedLink = network.removeLink(link.getId());
								MATSimPlugin.toggleDialog.notifyDataChanged(network);
								System.out.println(removedLink);
							}
						}
						enterWay2Links((Way) prim);
						MATSimPlugin.toggleDialog.notifyDataChanged(network);
					}
				}
			}
		}
		List<Link> oldLinks = way2Links.remove(changed.getChangedWay());
		if (oldLinks != null) {
			for (Link link : oldLinks) {
				System.out.println("remove because way nodes changed.");
				Link removedLink = network.removeLink(link.getId());
				MATSimPlugin.toggleDialog.notifyDataChanged(network);
				System.out.println(removedLink);
			}
		}
		enterWay2Links((Way) changed.getChangedWay());
		MATSimPlugin.toggleDialog.notifyDataChanged(network);
	}
}
