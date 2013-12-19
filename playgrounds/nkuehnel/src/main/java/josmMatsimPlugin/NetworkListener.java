package josmMatsimPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;

public class NetworkListener implements DataSetListener {
	private Network network;
	private CoordinateTransformation ct;
	private String originSystem;
	private Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
	private NetworkLayer layer;

	public NetworkListener(NetworkLayer layer, Map<Way, List<Link>> way2Links,
			String originSystem) {
		this.layer = layer;
		this.network = layer.getMatsimNetwork();
		this.originSystem = originSystem;
		this.ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, originSystem);
		this.way2Links = way2Links;
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
		network.getNodes().get(id).getCoord().setXY(temp.getX(), temp.getY());
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
					MATSimPlugin.toggleDialog.title(layer);
				}

			} else if (primitive instanceof Way) {
				Way way = (Way) primitive;
				if (!way2Links.containsKey(way)) {
					List<Link> links = enterWay2Links(way);
					for (Link link : links) {
						network.addLink(link);
						MATSimPlugin.toggleDialog.title(layer);
					}
				}
			}

		}
	}

	private List<Link> enterWay2Links(Way way) {
		List<Link> links = computeWay2Links(way);
		List<Link> previous = way2Links.put(way, links);
		if (previous != null) {
			throw new RuntimeException("Shouldn't happen.");
		}
		return links;
	}

	public List<Link> computeWay2Links(Way way) {
		Map<String, String> keys = way.getKeys();
		if (way.getNodesCount() != 2)
			return Collections.emptyList();
		if (!keys.containsKey("capacity"))
			return Collections.emptyList();
		if (!keys.containsKey("freespeed"))
			return Collections.emptyList();
		if (!keys.containsKey("permlanes"))
			return Collections.emptyList();
		if (!keys.containsKey("length"))
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
		link.setCapacity(Double.parseDouble(keys.get("capacity")));
		link.setFreespeed(Double.parseDouble(keys.get("freespeed")));
		link.setNumberOfLanes(Double.parseDouble(keys.get("permlanes")));
		link.setLength(Double.parseDouble(keys.get("length")));
		return Collections.singletonList(link);
	}

	public Node matsim4osm(org.openstreetmap.josm.data.osm.Node firstNode) {
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
				List<Link> links = way2Links.remove(((Way) primitive));
				for (Link link : links) {
					network.removeLink(link.getId());
					System.out.println("link removed!");
				}
			}
		}
		System.out.println("have links: " + network.getLinks().size());
		MATSimPlugin.toggleDialog.title(layer);
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
				List<Link> newLinks = enterWay2Links(way);
				for (Link link : oldLinks) {
					System.out.println("remove because tag changed.");
					Link removedLink = network.removeLink(link.getId());
					MATSimPlugin.toggleDialog.title(layer);
					System.out.println(removedLink);
				}
				for (Link link : newLinks) {
					System.out.println("add because tag changed.");
					network.addLink(link);
					MATSimPlugin.toggleDialog.title(layer);
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
