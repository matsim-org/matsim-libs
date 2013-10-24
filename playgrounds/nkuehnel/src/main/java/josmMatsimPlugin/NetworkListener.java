package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
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
	private NetworkLayer layer;
	private CoordinateTransformation ct;
	
	public NetworkListener()
	{
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
		{
			this.layer= (NetworkLayer) Main.main.getActiveLayer();
			this.ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,layer.getCoordSystem());
		}
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
		if(layer != null)
		{
			String id = String.valueOf(moved.getNode().getId());
			Map<String, Node> nodes = layer.getNodes();
			
			if ( nodes.containsKey(id))
			{
				Coord temp= ct.transform(new CoordImpl(moved.getNode().getCoor().lon(), moved.getNode().getCoor().lat()));
				nodes.get(id).getCoord().setXY(temp.getX(), temp.getY());
				
				for (Link link: layer.getLinks().values())
				{
					if(link.getFromNode().getId().toString().equals(id) || 
							link.getToNode().getId().toString().equals(id))
					{
						if (layer.getCoordSystem().equals("WGS84"))
							link.setLength(Defaults.calculateWGS84Length(link.getToNode().getCoord(), link.getFromNode().getCoord()));
						else
							link.setLength(CoordUtils.calcDistance(link.getToNode().getCoord(), link.getFromNode().getCoord()));
						OsmPrimitiveType type = OsmPrimitiveType.WAY;
						Way way =(Way) layer.data.getPrimitiveById(Long.parseLong(link.getId().toString()), type);
						for(Entry<String, String> entry:way.getKeys().entrySet())
						{
							if (entry.getKey().equals("length"))
								way.put("length", (String.valueOf(link.getLength())));
								System.out.println("laenge angepasst");
						}
					}
				}
			}
		}
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
		layer.data.clearSelection();
		
		for (OsmPrimitive primitive: added.getPrimitives())
		{
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node)
			{
				if (primitive.getId()<=0)
				{
					layer.data.removePrimitive(primitive);
					
					long maxId=0;
					for (String id: layer.getNodes().keySet())
					{
						maxId= Math.max(maxId, Long.parseLong(id));
					}
					maxId++;
					
					layer.data.beginUpdate();
					org.openstreetmap.josm.data.osm.Node node = new org.openstreetmap.josm.data.osm.Node(maxId, 1);
					node.setCoor(((org.openstreetmap.josm.data.osm.Node) primitive).getCoor());
					layer.data.addPrimitive(node);
					layer.data.endUpdate();
//					
					Coord coord = new CoordImpl(node.getCoor().lon(), node.getCoor().lat());
					Node nodeTemp =layer.getMatsimNetwork().getFactory().createNode(new IdImpl(maxId), coord);
					layer.getMatsimNetwork().addNode(nodeTemp);
					System.out.println("node hinzugefuegt!"+nodeTemp.getId());
					layer.getNodes().put(nodeTemp.getId().toString(), nodeTemp);
				}
			}
			else if (primitive instanceof Way)
			{
				if (primitive.getId()<=0)
				{
					LinkAddedDialog dialog = new LinkAddedDialog(String.valueOf(((Way) primitive).getLength()));
			        JOptionPane pane = new JOptionPane(dialog, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			        dialog.setOptionPane(pane);
			        JDialog dlg = pane.createDialog(Main.parent, tr("Add link"));
			        dlg.setAlwaysOnTop(true);
			        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			        dlg.setVisible(true);
			        
			        layer.data.removePrimitive(primitive);
			        
			        if(pane.getValue()!=null)
			        {
			        	if(((Integer)pane.getValue()) == JOptionPane.OK_OPTION)
			        	{
			        		Node fromNode = layer.getNodes().get(String.valueOf(((Way) primitive).firstNode().getId()));
							Node toNode = layer.getNodes().get(String.valueOf(((Way) primitive).lastNode().getId()));
							String linkLength = String.valueOf(Defaults.calculateWGS84Length(fromNode.getCoord(), toNode.getCoord()));

							long maxId=0;
							for (String id: layer.getLinks().keySet())
							{
								maxId= Math.max(maxId, Long.parseLong(id));
							}
							maxId++;
			        		
			        		if (dialog.drawnDirection.isSelected())
			        		{
			        			Link link = layer.getMatsimNetwork().getFactory().createLink(new IdImpl((maxId)), fromNode, toNode);
			        			link.setLength(Double.parseDouble(linkLength));
			        			link.setCapacity(Double.parseDouble(LinkAddedDialog.capacity.getText()));
			        			link.setFreespeed(Double.parseDouble(LinkAddedDialog.freeSpeed.getText()));
			        			link.setNumberOfLanes(Double.parseDouble(LinkAddedDialog.numberOfLanes.getText()));
			        			layer.getMatsimNetwork().addLink(link);
			        			
			        			layer.data.beginUpdate();
			        			Way way = new Way((maxId), 1);
			        			way.put("length", linkLength);
			        			way.put("capacity", LinkAddedDialog.capacityRev.getText());
			        			way.put("freespeed", LinkAddedDialog.freeSpeedRev.getText());
			        			way.put("numberOfLanes", LinkAddedDialog.numberOfLanesRev.getText());
			        			way.addNode( ((Way) primitive).firstNode());
								way.addNode( ((Way) primitive).lastNode());
								layer.data.addPrimitive(way);
								layer.data.endUpdate();
								
								layer.getLinks().put(String.valueOf((maxId)), link);
			        			
			        		}
			        		if (dialog.reverseDirection.isSelected())
			        		{
			        			Link linkReverse = layer.getMatsimNetwork().getFactory().createLink(new IdImpl((maxId+1)), toNode, fromNode);
			        			linkReverse.setLength(Double.parseDouble(linkLength));
			        			linkReverse.setCapacity(Double.parseDouble(LinkAddedDialog.capacityRev.getText()));
			        			linkReverse.setFreespeed(Double.parseDouble(LinkAddedDialog.freeSpeedRev.getText()));
			        			linkReverse.setNumberOfLanes(Double.parseDouble(LinkAddedDialog.numberOfLanesRev.getText()));
			        			layer.getMatsimNetwork().addLink(linkReverse);
			        			
			        			layer.data.beginUpdate();
			        			Way wayReverse = new Way((maxId+1), 1);
			        			wayReverse.put("length", linkLength);
			        			wayReverse.put("capacity", LinkAddedDialog.capacityRev.getText());
			        			wayReverse.put("freespeed", LinkAddedDialog.freeSpeedRev.getText());
			        			wayReverse.put("numberOfLanes", LinkAddedDialog.numberOfLanesRev.getText());
								wayReverse.addNode( ((Way) primitive).lastNode());
								wayReverse.addNode( ((Way) primitive).firstNode());
								layer.data.addPrimitive(wayReverse);
								layer.data.endUpdate();
								
								layer.getLinks().put(String.valueOf((maxId+1)), linkReverse);
			        		}
			        	}
			        }
			        dlg.dispose();
				}
			}
		}
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent primitivesRemoved)
	{
		layer.data.clearSelection();
		layer.data.beginUpdate();
		for (OsmPrimitive primitive: primitivesRemoved.getPrimitives())
		{
			if (primitive instanceof org.openstreetmap.josm.data.osm.Node)
			{
				String id = String.valueOf(primitive.getId());
				Node node = layer.getNodes().get(id);
				if(node!= null)
				{
					System.out.println("node removed!");
					layer.getMatsimNetwork().removeNode(node.getId());
					layer.getNodes().remove(node.getId().toString());
				}
				
			}
			else if(primitive instanceof Way)
			{
				String id = String.valueOf(primitive.getId());
				Link link = layer.getLinks().get(id);
				if(link!= null)
				{
					System.out.println("link removed!");
					layer.getMatsimNetwork().removeLink(link.getId());
					layer.getLinks().remove(link.getId().toString());
				}
			}
		}
		layer.data.endUpdate();
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void tagsChanged(TagsChangedEvent changed)
	{
//		tags ändern!
		
	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent changed)
	{
//		System.out.println("waychange");
//		layer.data.clearSelection();
//		layer.data.beginUpdate();
//		
//		Way way = changed.getChangedWay();
//		
//		layer.data.removePrimitive(way);
//		
//		Link correspondingLink=layer.getLinks().get(String.valueOf(way.getId()));
//		
//		if(correspondingLink!=null)
//		{
//			int fromIndex=0;
//			int toIndex=0;
//			boolean reverseDirection=false;
//			for (org.openstreetmap.josm.data.osm.Node node: way.getNodes())
//			{
//				if(Double.parseDouble(correspondingLink.getFromNode().getId().toString())==node.getId())
//				{
//					fromIndex=way.getNodes().indexOf(node);
//				}
//				else if(Double.parseDouble(correspondingLink.getToNode().getId().toString())==node.getId())
//				{
//					toIndex=way.getNodes().indexOf(node);
//				}
//			}
//			
//			if (toIndex<fromIndex)
//			{
//				System.out.println("reverse direction!");
//				reverseDirection=true;
//			}
//			
//			
//			long maxId=0;
//			for (String id: layer.getLinks().keySet())
//			{
//				maxId= Math.max(maxId, Long.parseLong(id));
//			}
//			maxId++;
//			
//			for (int i=0; i<(way.getNodesCount()-2);i++)
//			{
//    			Node fromNode=layer.getNodes().get(String.valueOf(way.getNode(i).getId()));
//    			Node toNode=layer.getNodes().get(String.valueOf(way.getNode(i+1).getId()));
//    			
//    			String linkLength = String.valueOf(Defaults.calculateWGS84Length(fromNode.getCoord(), toNode.getCoord()));
//    			
//    			Link link;
//    			
//    			if(!reverseDirection)
//    			{
//    				link = layer.getMatsimNetwork().getFactory().createLink(new IdImpl(maxId), fromNode, toNode);
//    			}
//    			else
//    				link = layer.getMatsimNetwork().getFactory().createLink(new IdImpl(maxId), toNode, fromNode);
//    			
//    			Way newWay = new Way(maxId, 1);
//				newWay.addNode(way.getNode(i));
//				newWay.addNode(way.getNode(i+1));
//    			newWay.put("length", linkLength);
//    			newWay.put("capacity", String.valueOf(correspondingLink.getCapacity()));
//    			newWay.put("freespeed", String.valueOf(correspondingLink.getFreespeed()));
//    			newWay.put("numberOfLanes", String.valueOf(correspondingLink.getNumberOfLanes()));
//    			
//    			link.setFreespeed(correspondingLink.getFreespeed());
//    			link.setCapacity(correspondingLink.getCapacity());
//    			link.setLength(Double.parseDouble(linkLength));
//    			link.setNumberOfLanes(correspondingLink.getNumberOfLanes());
//    			
//    			layer.getLinks().put(String.valueOf(maxId), link);
//    			layer.getMatsimNetwork().addLink(link);
//    			layer.data.addPrimitive(newWay);		
//    			
//    			maxId++;
//			}
//		}
//		layer.data.endUpdate();
		
	}

}
