package josmMatsimPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
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

public class NetworkListener implements DataSetListener
{
	private Network network;
	private CoordinateTransformation ct;
	private String originSystem;
	private Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
	private NetworkLayer layer;

	public NetworkListener(NetworkLayer layer, Network network,
			Map<Way, List<Link>> way2Links, String originSystem)
	{
		this.layer = layer;
		this.network = network;
		this.originSystem = originSystem;
		this.ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, originSystem);
		this.way2Links = way2Links;
	}

	@Override
	public void dataChanged(DataChangedEvent arg0)
	{
		// TODO Auto-generated method stub
		System.out.println("data changed");
	}

	@Override
	public void nodeMoved(NodeMovedEvent moved)
	{

		Id id = new IdImpl(String.valueOf(moved.getNode().getUniqueId()));

		Coord temp = ct.transform(new CoordImpl(
				moved.getNode().getCoor().lon(), moved.getNode().getCoor()
						.lat()));
		network.getNodes().get(id).getCoord().setXY(temp.getX(), temp.getY());

		MATSimPlugin.toggleDialog.updateTable(layer);
	}
	

	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent arg0)
	{
		// TODO Auto-generated method stub
		System.out.println("other");
	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent added)
	{
		// added.getDataset().clearSelection();
		System.out.println(added.wasIncomplete());
		for (OsmPrimitive primitive : added.getPrimitives())
		{
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node)
			{
				org.openstreetmap.josm.data.osm.Node node = (org.openstreetmap.josm.data.osm.Node) primitive;
				Coord coord = new CoordImpl(node.getCoor().lon(), node
						.getCoor().lat());
				IdImpl id = new IdImpl(primitive.getUniqueId());
				if (!network.getNodes().containsKey(id))
				{
					System.out.println(id);
					System.out.println(primitive.isDeleted() + " "
							+ primitive.isUndeleted());
					Node nodeTemp = network.getFactory().createNode(id, coord);
					network.addNode(nodeTemp);
					System.out.println("node hinzugefuegt!" + nodeTemp.getId());
				}

			} else if (primitive instanceof Way)
			{
				Way way = (Way) primitive;
				if (!way2Links.containsKey(way))
				{
					List<Link> links = enterWay2Links(way);
					for (Link link : links)
					{
						network.addLink(link);
					}
				}
			}

			// else if (primitive instanceof Way)
			// {
			//
			// LinkAddedDialog dialog = new
			// LinkAddedDialog(String.valueOf(((Way) primitive).getLength()));
			// JOptionPane pane = new JOptionPane(dialog,
			// JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			// dialog.setOptionPane(pane);
			// JDialog dlg = pane.createDialog(Main.parent, tr("Add link"));
			// dlg.setAlwaysOnTop(true);
			// dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			// dlg.setVisible(true);
			//
			// added.getDataset().removePrimitive(primitive);
			//
			// if(pane.getValue()!=null)
			// {
			// if(((Integer)pane.getValue()) == JOptionPane.OK_OPTION)
			// {
			// Node fromNode = network.getNodes().get(new
			// IdImpl(String.valueOf(((Way) primitive).firstNode().getId())));
			// Node toNode = network.getNodes().get(new
			// IdImpl(String.valueOf(((Way) primitive).lastNode().getId())));
			// String linkLength =
			// String.valueOf(Defaults.calculateWGS84Length(fromNode.getCoord(),
			// toNode.getCoord()));
			//
			//
			//
			// if (dialog.drawnDirection.isSelected())
			// {
			// Link link = network.getFactory().createLink(new IdImpl((maxId)),
			// fromNode, toNode);
			// link.setLength(Double.parseDouble(linkLength));
			// link.setCapacity(Double.parseDouble(LinkAddedDialog.capacity.getText()));
			// link.setFreespeed(Double.parseDouble(LinkAddedDialog.freeSpeed.getText()));
			// link.setNumberOfLanes(Double.parseDouble(LinkAddedDialog.numberOfLanes.getText()));
			// network.addLink(link);
			//
			// way.put("length", linkLength);
			// way.put("capacity", LinkAddedDialog.capacityRev.getText());
			// way.put("freespeed", LinkAddedDialog.freeSpeedRev.getText());
			// way.put("numberOfLanes",
			// LinkAddedDialog.numberOfLanesRev.getText());
			// way.addNode( ((Way) primitive).firstNode());
			// way.addNode( ((Way) primitive).lastNode());
			// added.getDataset().addPrimitive(way);
			// }
			// if (dialog.reverseDirection.isSelected())
			// {
			// Link linkReverse = network.getFactory().createLink(new
			// IdImpl((maxId+1)), toNode, fromNode);
			// linkReverse.setLength(Double.parseDouble(linkLength));
			// linkReverse.setCapacity(Double.parseDouble(LinkAddedDialog.capacityRev.getText()));
			// linkReverse.setFreespeed(Double.parseDouble(LinkAddedDialog.freeSpeedRev.getText()));
			// linkReverse.setNumberOfLanes(Double.parseDouble(LinkAddedDialog.numberOfLanesRev.getText()));
			// network.addLink(linkReverse);
			//
			// added.getDataset().beginUpdate();
			// Way wayReverse = new Way((maxId+1), 1);
			// wayReverse.put("length", linkLength);
			// wayReverse.put("capacity",
			// LinkAddedDialog.capacityRev.getText());
			// wayReverse.put("freespeed",
			// LinkAddedDialog.freeSpeedRev.getText());
			// wayReverse.put("numberOfLanes",
			// LinkAddedDialog.numberOfLanesRev.getText());
			// wayReverse.addNode( ((Way) primitive).lastNode());
			// wayReverse.addNode( ((Way) primitive).firstNode());
			// added.getDataset().addPrimitive(wayReverse);
			// added.getDataset().endUpdate();
			// }
			// }
			// }
			// dlg.dispose();
			// }
			// }
			// }
		}
		MATSimPlugin.toggleDialog.updateTable(layer);
	}

	private List<Link> enterWay2Links(Way way)
	{
		List<Link> links = computeWay2Links(way);
		List<Link> previous = way2Links.put(way, links);
		if (previous != null)
		{
			throw new RuntimeException("Shouldn't happen.");
		}
		return links;
	}

	public List<Link> computeWay2Links(Way way)
	{
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
		Node fromNode = matsim4osm(way.firstNode());
		Node toNode = matsim4osm(way.lastNode());
		Link link = network.getFactory().createLink(new IdImpl(way.getUniqueId()),
				fromNode, toNode);
		link.setCapacity(Double.parseDouble(keys.get("capacity")));
		link.setFreespeed(Double.parseDouble(keys.get("freespeed")));
		link.setNumberOfLanes(Double.parseDouble(keys.get("permlanes")));
		link.setLength(Double.parseDouble(keys.get("length")));
		return Collections.singletonList(link);
	}

	public Node matsim4osm(org.openstreetmap.josm.data.osm.Node firstNode)
	{
		return network.getNodes().get(new IdImpl(firstNode.getUniqueId()));
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved)
	{
		System.out.println(primitivesRemoved.wasComplete());
		for (OsmPrimitive primitive : primitivesRemoved.getPrimitives())
		{
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node)
			{
				String id = String.valueOf(primitive.getUniqueId());
				Node node = network.getNodes().get(new IdImpl(id));
				System.out.println("node removed!");
				network.removeNode(node.getId());
			} else if (primitive instanceof Way)
			{
				List<Link> links = way2Links.remove(((Way) primitive));
				for (Link link : links)
				{
					network.removeLink(link.getId());
					System.out.println("link removed!");
				}
			}
		}
		System.out.println("have links: " + network.getLinks().size());
		MATSimPlugin.toggleDialog.updateTable(layer);
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void tagsChanged(TagsChangedEvent changed)
	{
		for (OsmPrimitive primitive : changed.getPrimitives())
		{
			if (primitive instanceof Way)
			{
				Way way = (Way) primitive;
				List<Link> oldLinks = way2Links.remove(way);
				List<Link> newLinks = enterWay2Links(way);
				for (Link link : oldLinks)
				{
					System.out.println("remove because tag changed.");
					network.removeLink(link.getId());
				}
				for (Link link : newLinks)
				{
					System.out.println("add because tag changed.");
					network.addLink(link);
				}
			}
		}

		System.out.println("have links: " + network.getLinks().size());

		MATSimPlugin.toggleDialog.updateTable(layer);
	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent changed)
	{
		System.out.println("waychange " + changed.getChangedWay().getUniqueId());

	}

}
