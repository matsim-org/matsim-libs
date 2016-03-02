package playground.dhosse.utils.io.inpire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dhosse.scenarios.generic.utils.Modes;
import playground.dhosse.utils.GeometryUtils;

public class DbInspireNetworkParser extends MatsimXmlParser {
	
	public static void main(String args[]){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new DbInspireNetworkParser(scenario.getNetwork(), "EPSG:32632").parse(
				"/home/dhosse/Dokumente/10_data/data.db/DB-Netz_INSPIRE_20131128.gml");
		new NetworkWriter(scenario.getNetwork()).write("/home/dhosse/inspire-railNetwork.xml");
		GeometryUtils.writeNetwork2Shapefile(scenario.getNetwork(), "/home/dhosse/shp/", "EPSG:32632");
	}

	private static final String TAG_DIRECTION = "direction";
	private static final String TAG_ELEMENT = "element";
	private static final String TAG_END_NODE = "endNode";
	private static final String TAG_FORM_OF_NODE = "formOfNode";
	private static final String TAG_GML_POS = "pos";
	private static final String TAG_GML_POS_LIST = "posList";
	private static final String TAG_LINK = "link";
	private static final String TAG_MARKER_POST = "MarkerPost";
	private static final String TAG_RAILWAY_LINK = "RailwayLink";
	private static final String TAG_RAILWAY_LINK_SEQ = "RailwayLinkSequence";
	private static final String TAG_RAILWAY_NODE = "RailwayNode";
	private static final String TAG_RAILWAY_STATION_NODE = "RailwayStationNode";
	private static final String TAG_START_NODE = "startNode";
	private static final String TAG_TFD = "TrafficFlowDirection";
	
	private static final String ATT_GML_ID = "gml:id";
	private static final String ATT_XLINK = "xlink:href";
	
	private static final String VAL_BOTH_DIRECTIONS = "bothDirections";
	private static final String VAL_OPPOSITE_DIRECTION = "inOppositeDirection";
	
	private static final String SPACE = " ";
	private static final String PREFIX = "urn:x-dbnetze:oid";
	
	private Set<String> modes;
	
	private Network network;
	private CoordinateTransformation ct;
	
	private RailwayLink currentLink = null;
	private RailwayLinkSequence currentLinkSeq = null;
	private RailwayNode currentNode = null;
	private TrafficFlowDirection currentTfd = null;
	
	private Map<String, RailwayLink> railwayLinks;
	private Map<String, RailwayLinkSequence> railwayLinkSequences;
	private Map<String, RailwayNode> railwayNodes;
	private Map<String, TrafficFlowDirection> trafficFlowDirections;
	
	private boolean cleanNetwork = false;
	
	public DbInspireNetworkParser(final Network network, String toCRS){
		
		this(network, toCRS, false);
		
	}
	
	public DbInspireNetworkParser(final Network network, String toCRS, boolean cleanNetwork) {

		this.network = network;
		this.modes = new HashSet<>();
		this.modes.add(Modes.TRAIN);
		this.railwayLinks = new HashMap<>();
		this.railwayLinkSequences = new HashMap<>();
		this.railwayNodes = new HashMap<>();
		this.trafficFlowDirections = new HashMap<>();
		this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4258", toCRS);
		this.cleanNetwork = cleanNetwork;
		this.setValidating(false);
		
	}
	
	@Override
	public void parse(String filename){
		
		super.parse(filename);
		convert();
		
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if(TAG_RAILWAY_LINK.equals(name)){
			
			startLink(atts);
			
		} else if(TAG_START_NODE.equals(name)){
			
			setFromNodeForCurrentLink(atts);
			
		} else if(TAG_END_NODE.equals(name)){
			
			setToNodeForCurrentLink(atts);
			
		} else if(TAG_RAILWAY_NODE.equals(name)){
			
			startNode(atts);
			
		} else if(TAG_RAILWAY_LINK_SEQ.equals(name)){
			
			startLinkSequence(atts);
			
		} else if(TAG_LINK.equals(name)){
			
			if(this.currentLinkSeq != null && atts.getValue(ATT_XLINK) != null){
				
				addLinkIdToSequence(atts);
				
			}
			
		} else if(TAG_TFD.equals(name)){
			
			startTrafficFlowDirection(atts);
			
		} else if(TAG_ELEMENT.equals(name)){
			
			if(this.currentTfd != null){
				
				setLinkSeqRef(atts);
				
			}
			
		}
		
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

		if(TAG_RAILWAY_LINK.equals(name)){
			
			endLink();
			
		} else if(TAG_RAILWAY_NODE.equals(name)){
			
			endNode();
			
		} else if(TAG_GML_POS_LIST.equals(name)){
			
			setCurrentLinkLength(content);
			
		} else if(TAG_FORM_OF_NODE.equals(name)){
			
			setFormOfNode(content);
			
		}
//		else if(TAG_GML_POS.equals(name)){
//			
//			if(this.currentNode != null){
//				
//				setNodePosition(content);
//				
//			}
//			
//		}
		else if(TAG_RAILWAY_LINK_SEQ.equals(name)){
			
			endLinkSequence();
			
		} else if(TAG_TFD.equals(name)){
			
			endTrafficFlowDirection();
			
		} else if(TAG_DIRECTION.equals(name)){
			
			if(this.currentTfd != null){
				
				setDirection(content);
				
			}
			
		}
		
	}
	
	private void convert(){
		
//		for(RailwayNode node : this.railwayNodes.values()){
//			
//			Id<Node> nodeId = Id.createNodeId(node.id);
//			Node nn = this.network.getFactory().createNode(nodeId, node.coord);
//			this.network.addNode(nn);
//			
//		}

		int nodesCounter = 0;
		
		for(RailwayLink link : this.railwayLinks.values()){
			
//			if(link.fromNodeId.equals("Node-1482517_2")){
//				System.out.println();
//			}
			
			if(link.fromNodeId.equals("")){
				link.fromNodeId = "Node_" + Integer.toString(nodesCounter);
				nodesCounter++;
			}
			if(link.toNodeId.equals("")){
				link.toNodeId = "Node_" + Integer.toString(nodesCounter);
				nodesCounter++;
			}
			
			Id<Link> linkId = Id.createLinkId(link.id);
			Id<Node> fromNodeId = Id.createNodeId(link.fromNodeId);
			Id<Node> toNodeId = Id.createNodeId(link.toNodeId + "_2");
			
			List<Node> nodes = (List<Node>) ((NetworkImpl)this.network).getNearestNodes(link.fromCoord, 0);
			List<Node> nodes2 = (List<Node>) ((NetworkImpl)this.network).getNearestNodes(link.toCoord, 0);
			
			Node fromNode = nodes.size() > 0 ? nodes.get(0) : null;
			if(fromNode == null){
				if(this.network.getNodes().containsKey(fromNodeId)){
					fromNodeId = Id.createNodeId("Node_" + nodesCounter);
					fromNode = this.network.getFactory().createNode(fromNodeId, link.fromCoord);
					this.network.addNode(fromNode);
					nodesCounter++;
				} else {
					fromNode = this.network.getFactory().createNode(fromNodeId, link.fromCoord);
					this.network.addNode(fromNode);
				}
			} else {
				fromNodeId = fromNode.getId();
			}
			Node toNode = nodes2.size() > 0 ? nodes2.get(0) : null;
			if(toNode == null){
				if(this.network.getNodes().containsKey(toNodeId)){
					toNodeId = Id.createNodeId("Node_" + nodesCounter);
					toNode = this.network.getFactory().createNode(toNodeId, link.toCoord);
					this.network.addNode(toNode);
					nodesCounter++;
				} else {
					toNode = this.network.getFactory().createNode(toNodeId, link.toCoord);
					this.network.addNode(toNode);
				}
			} else{
				toNodeId = toNode.getId();
			}
			
			Link ll = this.network.getFactory().createLink(linkId, this.network.getNodes().get(fromNodeId),
					this.network.getNodes().get(toNodeId));
			ll.setAllowedModes(this.modes);
			ll.setLength(link.length);
			this.network.addLink(ll);
			
			if(!fromNode.getOutLinks().containsKey(ll.getId())){
				
				fromNode.addOutLink(ll);
				
			}
			
			if(!toNode.getInLinks().containsKey(ll.getId())){
				
				toNode.addInLink(ll);
				
			}
				
		}
		
		for(RailwayLinkSequence seq : this.railwayLinkSequences.values()){
			
			String trafficFlowDirection = this.trafficFlowDirections.get(seq.id).direction;
			
			if(trafficFlowDirection.equalsIgnoreCase(VAL_BOTH_DIRECTIONS)){
				
				for(String id : seq.railwayLinkIds){
					
					Id<Link> linkId = Id.createLinkId(id);
					Link ref = this.network.getLinks().get(linkId);
					
					if(ref != null){

						Link reverse = this.network.getFactory().createLink(Id.createLinkId(id + "_2"), ref.getToNode(), ref.getFromNode());
						reverse.setAllowedModes(this.modes);
						reverse.setCapacity(ref.getCapacity());
						reverse.setFreespeed(ref.getFreespeed());
						reverse.setLength(ref.getLength());
						reverse.setNumberOfLanes(ref.getNumberOfLanes());
						
						this.network.addLink(reverse);
						
						if(!ref.getToNode().getOutLinks().containsKey(reverse.getId())){
						
							ref.getToNode().addOutLink(reverse);
						
						}
						
						if(!ref.getFromNode().getInLinks().containsKey(reverse.getId())){
						
							ref.getFromNode().addInLink(reverse);
							
						}
						
					}
					
				}
				
			} else if(trafficFlowDirection.equalsIgnoreCase(VAL_OPPOSITE_DIRECTION)){
				
				for(String id : seq.railwayLinkIds){
					
					Id<Link> linkId = Id.createLinkId(id);
					Link ref = this.network.getLinks().get(linkId);
					
					if(ref != null){

						Link reverse = this.network.getFactory().createLink(Id.createLinkId(id + "_2"), ref.getToNode(), ref.getFromNode());
						reverse.setAllowedModes(this.modes);
						reverse.setCapacity(ref.getCapacity());
						reverse.setFreespeed(ref.getFreespeed());
						reverse.setLength(ref.getLength());
						reverse.setNumberOfLanes(ref.getNumberOfLanes());
						
						this.network.addLink(reverse);
						this.network.removeLink(ref.getId());
						ref.getToNode().getInLinks().remove(ref);
						ref.getFromNode().getOutLinks().remove(ref);
						
						if(!ref.getToNode().getOutLinks().containsKey(reverse.getId())){
						
							ref.getToNode().addOutLink(reverse);
						
						}
						
						if(!ref.getFromNode().getInLinks().containsKey(reverse.getId())){
						
							ref.getFromNode().addInLink(reverse);
							
						}
						
					}
					
				}
				
			}
			
		}
		
		if(this.cleanNetwork){
			new NetworkCleaner().run(this.network);
		}
		
	}
	
	private void startLink(Attributes atts){
		
		String id = atts.getValue(ATT_GML_ID);
		this.currentLink = new RailwayLink(id);
		
	}
	
	private void endLink(){
		
		this.railwayLinks.put(this.currentLink.id, this.currentLink);
		this.currentLink = null;
		
	}
	
	private void setFromNodeForCurrentLink(Attributes atts){
		
		String id = atts.getValue(ATT_XLINK).replace(PREFIX, "");
		this.currentLink.fromNodeId = id;
		
	}
	
	private void setToNodeForCurrentLink(Attributes atts){
		
		this.currentLink.toNodeId = atts.getValue(ATT_XLINK).replace(PREFIX, "");
		
	}
	
	private void setCurrentLinkLength(String content){
		
		String[] splitContent = content.split(SPACE);
		
		Coord lastCoord = null;
		double length = 0;
		
		for(int i = 0; i < splitContent.length-1; i+=2){
			
			Coord coord = this.ct.transform(new Coord(Double.parseDouble(splitContent[i+1]), Double.parseDouble(splitContent[i])));
			
			if(i > 0){
				
				length += CoordUtils.calcEuclideanDistance(lastCoord, coord);
				
			}
			
			if(i >= splitContent.length - 2){
				
				this.currentLink.toCoord = coord;
				
			}
			if(i == 0){
				
				this.currentLink.fromCoord = coord;
				
			}
			
			lastCoord = coord;
			
		}
		
		this.currentLink.length = length;
		
	}
	
	private void startNode(Attributes atts){
		
		String id = atts.getValue(ATT_GML_ID);
		this.currentNode = new RailwayNode(id);
		
	}
	
	private void endNode(){
		
		this.railwayNodes.put(this.currentNode.id, this.currentNode);
		this.currentNode = null;
		
	}
	
//	private void setNodePosition(String content){
//		
//		String[] xy = content.split(SPACE);
//		
//		this.currentNode.coord = this.ct.transform(new Coord(Double.parseDouble(xy[1]), Double.parseDouble(xy[0])));
//		
//	}
	
	private void setFormOfNode(String content){
		
	}
	
	private void startLinkSequence(Attributes atts){
		
		String id = atts.getValue(ATT_GML_ID);
		this.currentLinkSeq = new RailwayLinkSequence(id);
		
	}
	
	private void endLinkSequence(){
		
		this.railwayLinkSequences.put(this.currentLinkSeq.id, this.currentLinkSeq);
		this.currentLinkSeq = null;
		
	}
	
	private void addLinkIdToSequence(Attributes atts){
	
		String id = atts.getValue(ATT_XLINK).replace(PREFIX, "");
		this.currentLinkSeq.railwayLinkIds.add(id);
		
	}
	
	private void startTrafficFlowDirection(Attributes atts) {

		this.currentTfd = new TrafficFlowDirection();
		
	}
	
	private void endTrafficFlowDirection() {

		this.trafficFlowDirections.put(this.currentTfd.refId, this.currentTfd);
		this.currentTfd = null;
		
	}
	
	private void setLinkSeqRef(Attributes atts) {
		
		this.currentTfd.refId = atts.getValue(ATT_XLINK).replace(PREFIX, "");
		
	}
	
	private void setDirection(String content) {
		
		this.currentTfd.direction = content;
		
	}
	
	private class RailwayLink{
		
		private String id;
		private String fromNodeId;
		private String toNodeId;
		private double length;
		private Coord fromCoord;
		private Coord toCoord;
		
		RailwayLink(String id){
			
			this(id, 0., null, null);
			
		}
		
		RailwayLink(String id, double length, String fromNodeId, String toNodeId){
			
			this.id = id;
			this.length = length;
			this.fromNodeId = fromNodeId;
			this.toNodeId = toNodeId;
			
		}
		
	}
	
	class RailwayLinkSequence{
		
		private String id;
		private LinkedList<String> railwayLinkIds;
		
		RailwayLinkSequence(String id){
			this.id = id;
			this.railwayLinkIds = new LinkedList<>();
		}
		
	}
	
	class RailwayNode{
		
		private String id;
		
		RailwayNode(String id){
			this.id = id;
		}
		
	}
	
	class TrafficFlowDirection{
		
		private String refId;
		private String direction;
		
		TrafficFlowDirection(){};
		
	}

}
