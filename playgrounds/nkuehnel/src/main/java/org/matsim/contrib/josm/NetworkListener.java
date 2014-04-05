package org.matsim.contrib.josm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;

/**
 * Listens to changes in the dataset and their effects on the Network
 * 
 * 
 */
public class NetworkListener implements DataSetListener {
	private Network network;
	private CoordinateTransformation ct;
	private NetworkLayer layer;
	private static final List<String> TRANSPORT_MODES = Arrays.asList(
			TransportMode.bike, TransportMode.car, TransportMode.other,
			TransportMode.pt, TransportMode.ride, TransportMode.transit_walk,
			TransportMode.walk);

	public NetworkListener(NetworkLayer layer) throws IllegalArgumentException{
		this.layer = layer;
		this.network = layer.getMatsimNetwork();
		this.ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, layer.getCoordSystem());
	}

	@Override
	public void dataChanged(DataChangedEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("data changed");
	}

	@Override
	public void nodeMoved(NodeMovedEvent moved) {

		Id id = new IdImpl(String.valueOf(moved.getNode().getUniqueId()));

		Coord temp = ct.transform(new CoordImpl(
				moved.getNode().getCoor().lon(), moved.getNode().getCoor()
						.lat()));
		Node node = network.getNodes().get(id);
		node.getCoord().setXY(temp.getX(), temp.getY());

		for (Link link : node.getInLinks().values()) {
			if (!((Way) layer.data.getPrimitiveById(
					Long.parseLong(link.getId().toString()),
					OsmPrimitiveType.WAY)).hasKey("length")) {
				link.setLength(linkLength(link));
			}
		}
		for (Link link : node.getOutLinks().values()) {
			if (!((Way) layer.data.getPrimitiveById(
					Long.parseLong(link.getId().toString()),
					OsmPrimitiveType.WAY)).hasKey("length")) {
				link.setLength(linkLength(link));
			}
		}

		MATSimPlugin.toggleDialog.notifyDataChanged(layer);
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
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				org.openstreetmap.josm.data.osm.Node node = (org.openstreetmap.josm.data.osm.Node) primitive;
				Coord coord = ct.transform(new CoordImpl(node.getCoor().lon(),
						node.getCoor().lat()));
				IdImpl id = new IdImpl(primitive.getUniqueId());
				if (!network.getNodes().containsKey(id)) {
					System.out.println(id);
					System.out.println(primitive.isDeleted() + " "
							+ primitive.isUndeleted());
					Node nodeTemp = network.getFactory().createNode(id, coord);
					if (node.hasKey(ImportTask.NODE_TAG_ID)) {
						((NodeImpl) nodeTemp).setOrigId(node
								.get(ImportTask.NODE_TAG_ID));
					} else {
						((NodeImpl) nodeTemp).setOrigId(id.toString());
					}
					network.addNode(nodeTemp);
					System.out.println("node hinzugefuegt!" + nodeTemp.getId());
					MATSimPlugin.toggleDialog.notifyDataChanged(layer);
				}
			} else if (primitive instanceof Way) {
				Way way = (Way) primitive;

				if (!layer.getWay2Links().containsKey(way)) {
					List<Link> links = enterWay2Links(way);
					for (Link link : links) {
						network.addLink(link);
						MATSimPlugin.toggleDialog.notifyDataChanged(layer);
					}
				}
			}
		}
	}

	private List<Link> enterWay2Links(Way way) {
		List<Link> links = computeWay2Links(way);
		List<Link> previous = layer.getWay2Links().put(way, links);
		if (previous != null) {
			throw new RuntimeException("Shouldn't happen.");
		}
		return links;
	}

	private List<Link> computeWay2Links(Way way) {
		Map<String, String> keys = way.getKeys();
		if (way.getNodesCount() != 2)
			return Collections.emptyList();
		if (!keys.containsKey("capacity"))
			return Collections.emptyList();
		if (!keys.containsKey("freespeed"))
			return Collections.emptyList();
		if (!keys.containsKey("permlanes"))
			return Collections.emptyList();
		if (!keys.containsKey("modes"))
			return Collections.emptyList();

		String id = Long.toString(way.getUniqueId());
		Node fromNode = matsim4osm(way.firstNode());
		Node toNode = matsim4osm(way.lastNode());
		if (fromNode == null || toNode == null) {
			return Collections.emptyList();
		}
		Link link = network.getFactory().createLink(new IdImpl(id), fromNode,
				toNode);
		if (keys.containsKey(ImportTask.WAY_TAG_ID)) {
			((LinkImpl) link).setOrigId(keys.get(ImportTask.WAY_TAG_ID));
		} else {
			((LinkImpl) link).setOrigId(id.toString());
		}
		Double capacity = parseDoubleIfPossible(keys.get("capacity"));
		if (capacity == null) {
			return Collections.emptyList();
		}
		Double freespeed = parseDoubleIfPossible(keys.get("freespeed"));
		if (freespeed == null) {
			return Collections.emptyList();
		}
		Double permlanes = parseDoubleIfPossible(keys.get("permlanes"));
		if (permlanes == null) {
			return Collections.emptyList();
		}

		link.setCapacity(capacity);
		link.setFreespeed(freespeed);
		link.setNumberOfLanes(permlanes);

		Set<String> modes = new HashSet<String>();
		String tempArray[] = keys.get("modes").split(";");
		for (int i = 0; i < tempArray.length; i++) {
			String mode = tempArray[i];
			if (TRANSPORT_MODES.contains(mode)) {
				modes.add(tempArray[i]);
			}
		}
		if (modes.size() == 0) {
			return Collections.emptyList();
		}
		link.setAllowedModes(modes);

		if (keys.containsKey("length")) {
			link.setLength(Double.parseDouble(keys.get("length")));
		} else {
			Double length = linkLength(link);
			link.setLength(length);
		}
		return Collections.singletonList(link);
	}

	private Double parseDoubleIfPossible(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double linkLength(Link link) {
		Double length;
		if (layer.getCoordSystem().equals(TransformationFactory.WGS84)) {
			length = OsmConvertDefaults.calculateWGS84Length(link.getFromNode()
					.getCoord(), link.getToNode().getCoord());
		} else {
			length = CoordUtils.calcDistance(link.getFromNode().getCoord(),
					link.getToNode().getCoord());
		}
		return length;
	}

	private Node matsim4osm(org.openstreetmap.josm.data.osm.Node firstNode) {
		String id = Long.toString(firstNode.getUniqueId());
		Node node = network.getNodes().get(new IdImpl(id));
		return node;
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved) {
		System.out.println(primitivesRemoved.wasComplete());
		for (OsmPrimitive primitive : primitivesRemoved.getPrimitives()) {
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node) {
				String id = String.valueOf(primitive.getUniqueId());
				Node node = network.getNodes().get(new IdImpl(id));
				System.out.println("node removed!");
				network.removeNode(node.getId());
			} else if (primitive instanceof Way) {
				List<Link> links = layer.getWay2Links().remove(
						((Way) primitive));
				for (Link link : links) {
					network.removeLink(link.getId());
					System.out.println("link removed!");
				}
			}
		}
		System.out.println("have links: " + network.getLinks().size());
		MATSimPlugin.toggleDialog.notifyDataChanged(layer);
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
				List<Link> oldLinks = layer.getWay2Links().remove(way);
				List<Link> newLinks = enterWay2Links(way);
				for (Link link : oldLinks) {
					System.out.println("remove because tag changed.");
					Link removedLink = network.removeLink(link.getId());
					MATSimPlugin.toggleDialog.notifyDataChanged(layer);
					System.out.println(removedLink);
				}
				for (Link link : newLinks) {
					System.out.println("add because tag changed.");
					network.addLink(link);
					MATSimPlugin.toggleDialog.notifyDataChanged(layer);
				}
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
