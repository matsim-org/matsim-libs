package org.matsim.contrib.josm;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NodeImpl;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.*;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import java.util.List;
import java.util.Map;

/**
 * Listens to changes in the dataset and their effects on the Network
 * 
 * 
 */
class NetworkListener implements DataSetListener {
	private Network network;
	private OsmDataLayer layer;

	private Map<Way, List<Link>> way2Links;
	private Map<Link, WaySegment> link2Segment;

	public NetworkListener(OsmDataLayer layer, Network network,
			Map<Way, List<Link>> way2Links, Map<Link, WaySegment> link2Segment)
			throws IllegalArgumentException {
		this.layer = layer;
		this.network = network;
		this.way2Links = way2Links;
		this.link2Segment = link2Segment;
	}

	@Override
	public void dataChanged(DataChangedEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("data changed");
	}

	@Override
	public void nodeMoved(NodeMovedEvent moved) {

		Id id = new IdImpl(String.valueOf(moved.getNode().getUniqueId()));
		Node node = network.getNodes().get(id);
		node.getCoord().setXY(moved.getNode().getCoor().lon(),
				moved.getNode().getCoor().lat());

		for (Link link : node.getInLinks().values()) {
			long wayId;
			String tempId = link.getId().toString();
			if (tempId.contains("_")) {
				wayId = Long
						.parseLong(tempId.substring(0, tempId.indexOf("_")));
			} else {
				wayId = Long.parseLong(tempId);
			}
			if (!layer.data
					.getPrimitiveById(wayId, OsmPrimitiveType.WAY)
					.hasKey("length")) {
				link.setLength(OsmConvertDefaults.calculateWGS84Length(link
						.getFromNode().getCoord(), link.getToNode().getCoord()));
			}
		}
		for (Link link : node.getOutLinks().values()) {
			long wayId;
			String tempId = link.getId().toString();
			if (tempId.contains("_")) {
				wayId = Long
						.parseLong(tempId.substring(0, tempId.indexOf("_")));
			} else {
				wayId = Long.parseLong(tempId);
			}
			if (!layer.data
					.getPrimitiveById(wayId, OsmPrimitiveType.WAY)
					.hasKey("length")) {
				link.setLength(OsmConvertDefaults.calculateWGS84Length(link
						.getFromNode().getCoord(), link.getToNode().getCoord()));
			}
		}
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
		NewConverter.convertWay(way, network, way.getUniqueId(), way2Links,
				link2Segment);
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
						link2Segment.remove(link);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void tagsChanged(TagsChangedEvent changed) {
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
		System.out
				.println("waychange " + changed.getChangedWay().getUniqueId());
	}
}
