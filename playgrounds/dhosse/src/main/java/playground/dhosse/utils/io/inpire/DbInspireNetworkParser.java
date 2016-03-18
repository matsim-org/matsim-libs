package playground.dhosse.utils.io.inpire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;

import playground.boescpa.converters.osm.ptMapping.PseudoNetworkCreator;
import playground.dhosse.scenarios.generic.utils.Modes;
import playground.dhosse.utils.GeometryUtils;

/**
 * Parser for the rail network data provided by the Deutsche Bahn
 * (<a href="http://data.deutschebahn.com/datasets/streckennetz/">DB-Streckennetz</a>).
 * After reading the input xml file, the network data is converted into a matsim network.
 * 
 * @author dhosse
 *
 */
public class DbInspireNetworkParser extends MatsimXmlParser {
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new DbInspireNetworkParser(scenario, "EPSG:32632").parse(
				"/home/dhosse/Dokumente/10_data/data.db/DB-Netz_INSPIRE_20131128.gml");
		
		new NetworkWriter(scenario.getNetwork()).write("/home/dhosse/inspire-railNetwork.xml");
		
		GeometryUtils.writeNetwork2Shapefile(scenario.getNetwork(), "/home/dhosse/shp/", "EPSG:32632");
		GeometryUtils.writeStopFacilities2Shapefile(scenario.getTransitSchedule().getFacilities().values(),
				"/home/dhosse/shp/", "EPSG:32632");
		
	}
	
	private static final Logger log = Logger.getLogger(DbInspireNetworkParser.class);

	private static final String TAG_DESIGN_SPEED = "DesignSpeed";
	private static final String TAG_DIRECTION = "direction";
	private static final String TAG_ELEMENT = "element";
	private static final String TAG_END_NODE = "endNode";
	private static final String TAG_FORM_OF_NODE = "formOfNode";
	private static final String TAG_GML_POS = "pos";
	private static final String TAG_GML_POS_LIST = "posList";
	private static final String TAG_LINK = "link";
	//TODO not yet implemented
//	private static final String TAG_MARKER_POST = "MarkerPost";
	private static final String TAG_NUMBER_OF_TRACKS = "NumberOfTracks";
	private static final String TAG_N_TRACKS = "numberOfTracks";
	private static final String TAG_RAILWAY_LINE = "RailwayLine";
	private static final String TAG_RAILWAY_LINE_CODE = "railwayLineCode";
	private static final String TAG_RAILWAY_LINK = "RailwayLink";
	private static final String TAG_RAILWAY_LINK_SEQ = "RailwayLinkSequence";
	private static final String TAG_RAILWAY_NODE = "RailwayNode";
	private static final String TAG_RAILWAY_STATION_NODE = "RailwayStationNode";
	private static final String TAG_SPEED = "speed";
	private static final String TAG_START_NODE = "startNode";
	private static final String TAG_TEXT = "text";
	private static final String TAG_TFD = "TrafficFlowDirection";
	
	private static final String ATT_GML_ID = "gml:id";
	private static final String ATT_XLINK = "xlink:href";
	
	private static final String VAL_BOTH_DIRECTIONS = "bothDirections";
	private static final String VAL_OPPOSITE_DIRECTION = "inOppositeDirection";
	
	private static final String SPACE = " ";
	private static final String PREFIX = "urn:x-dbnetze:oid";
	
	private Set<String> modes;
	
	private Network network;
	private TransitSchedule schedule;
	private CoordinateTransformation ct;
	
	private RailwayLink currentLink = null;
	private RailwayLinkSequence currentLinkSeq = null;
	private RailwayNode currentNode = null;
	private TrafficFlowDirection currentTfd = null;
	private DesignSpeed currentDesignSpeed = null;
	private NumberOfTracks currentNumberOfTracks = null;
	
	private RailwayLine currentRailwayLine = null;
	
	private RailwayStationNode currentStation = null;
	
	private Map<String, RailwayLink> railwayLinks;
	private Map<String, RailwayLinkSequence> railwayLinkSequences;
	private Map<String, RailwayNode> railwayNodes;
	private Map<String, TrafficFlowDirection> trafficFlowDirections;
	private Map<String, DesignSpeed> designSpeeds;
	private Map<String, NumberOfTracks> numberOfTracks;
	private Map<String, RailwayStationNode> stations;
	private Map<String, RailwayLine> railwayLines;
	
	private boolean cleanNetwork = false;
	
	private Map<String, String> linkId2LineId = new HashMap<>();
	
	public DbInspireNetworkParser(final Scenario scenario, String toCRS){
		
		this(scenario, toCRS, false);
		
	}
	
	public DbInspireNetworkParser(final Scenario scenario, String toCRS, boolean cleanNetwork) {

		this.network = scenario.getNetwork();
		this.schedule = scenario.getTransitSchedule();
		
		this.modes = new HashSet<>();
		this.modes.add(Modes.TRAIN);
		
		this.railwayLinks = new HashMap<>();
		this.railwayLinkSequences = new HashMap<>();
		this.railwayNodes = new HashMap<>();
		this.trafficFlowDirections = new HashMap<>();
		this.designSpeeds = new HashMap<>();
		this.numberOfTracks = new HashMap<>();
		this.stations = new HashMap<>();
		this.railwayLines = new HashMap<>();
		
		this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4258", toCRS);
		this.cleanNetwork = cleanNetwork;
		
		this.setValidating(false);
		
	}
	
	@Override
	public void parse(String filename){
		
		super.parse(filename);
		log.info("Converting...");
		convert();
		logConversionInfo();
		
	}
	
	private void logConversionInfo(){
		
		log.info("Conversion statistics");
		log.info("INSPIRE");
		log.info("Number of railway nodes:           " + this.railwayNodes.size());
		log.info("Number of railway links:           " + this.railwayLinks.size());
		log.info("Number of railway link sequences:  " + this.railwayLinkSequences.size());
		log.info("Number of railway station nodes:   " + this.stations.size());
		log.info("Number of railway lines:           " + this.railwayLines.size());
		log.info("MATSim:");
		log.info("Number of nodes:                   " + this.network.getNodes().size());
		log.info("Number of links:                   " + this.network.getLinks().size());
		log.info("Number of transit stop facilities: " + this.schedule.getFacilities().size());
		
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
			
//			if(this.currentRailwayLine != null){
//				for(int i = 0; i < atts.getLength(); i++){
//					System.out.println(atts.getLocalName(i));
//					System.out.println(atts.getValue(i));
//				}
//			}
			
			addLinkIdToSequence(atts);
			
		} else if(TAG_TFD.equals(name)){
			
			startTrafficFlowDirection(atts);
			
		} else if(TAG_ELEMENT.equals(name)){
			
			setLinkSeqRef(atts);
			
		} else if(TAG_DESIGN_SPEED.equals(name)){
			
			startDesignSpeed(atts);
			
		} else if(TAG_NUMBER_OF_TRACKS.equals(name)){
			
			startNumberOfTracks(atts);
			
		} else if(TAG_RAILWAY_STATION_NODE.equals(name)){
			
			startStation(atts);
			
		} else if(TAG_RAILWAY_LINE.equals(name)){
			
			this.currentRailwayLine = new RailwayLine();
			String id = atts.getValue(ATT_GML_ID);
			this.currentRailwayLine.id = id;
			
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
		else if(TAG_RAILWAY_LINK_SEQ.equals(name)){
			
			endLinkSequence();
			
		} else if(TAG_TFD.equals(name)){
			
			endTrafficFlowDirection();
			
		} else if(TAG_DIRECTION.equals(name)){
			
			if(this.currentTfd != null){
				
				setDirection(content);
				
			}
			
		} else if(TAG_SPEED.equals(name)){
			
			setDesignSpeed(content);
			
		} else if(TAG_DESIGN_SPEED.equals(name)){
			
			endDesignSpeed();
			
		} else if(TAG_NUMBER_OF_TRACKS.equals(name)){
			
			endNumberOfTracks();
			
		} else if(TAG_N_TRACKS.equals(name)){
			
			setNumberOfTracks(content);
			
		} if(TAG_RAILWAY_STATION_NODE.equals(name)){
			
			endStation();
			
		} else if(TAG_GML_POS.equals(name)){
			
			setPosition(content);
			
		} else if(TAG_TEXT.equals(name)){
			
			handleText(content);
			
		} else if(TAG_RAILWAY_LINE.equals(name)){
			
			this.railwayLines.put(this.currentRailwayLine.id, this.currentRailwayLine);
			this.currentRailwayLine = null;
			
		} else if(TAG_RAILWAY_LINE_CODE.equals(name)){
			
			this.currentRailwayLine.id = content;
			
		}
		
	}
	
	private void handleText(String content){
		
		if(this.currentStation != null){
			
			this.currentStation.name = content;
			
		}
		
	}
	
	private void startStation(Attributes atts){
		
		String id = atts.getValue(ATT_GML_ID);
		this.currentStation = new RailwayStationNode(id);
		
	}
	
	private void endStation(){
		
		this.stations.put(this.currentStation.id, this.currentStation);
		this.currentStation = null;
		
	}
	
	private void setPosition(String content){
		
		String[] splitContent = content.split(SPACE);
		double x = Double.parseDouble(splitContent[1]);
		double y = Double.parseDouble(splitContent[0]);
		
		if(this.currentStation != null){

			this.currentStation.coord = this.ct.transform(new Coord(x, y));
			
		}
		
	}
	
	private void convert(){
		
		createLinks();
		
		if(this.cleanNetwork){
			new NetworkCleaner().run(this.network);
		}
		
		createTransitStopFacilities();
		
	}
	
	private void createLinks(){
		
		int nodesCounter = 0;
		
		for(RailwayLink link : this.railwayLinks.values()){
			
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
			ll.setCapacity(30);
			ll.setLength(link.length);
			((LinkImpl)ll).setOrigId(this.linkId2LineId.get(link.id));
			this.network.addLink(ll);
			
			if(!fromNode.getOutLinks().containsKey(ll.getId())){
				
				fromNode.addOutLink(ll);
				
			}
			
			if(!toNode.getInLinks().containsKey(ll.getId())){
				
				toNode.addInLink(ll);
				
			}
				
		}
		
		setLinkProperties();
		
	}
	
	private void setLinkProperties(){
		
		for(RailwayLinkSequence seq : this.railwayLinkSequences.values()){
			
			String trafficFlowDirection = this.trafficFlowDirections.get(seq.id).direction;
			
			double designSpeed = this.designSpeeds.containsKey(seq.id) ?
					this.designSpeeds.get(seq.id).v : 120 / 3.6;
			
			int nTracks = this.numberOfTracks.containsKey(seq.id) ? this.numberOfTracks.get(seq.id).nTracks : 1;
			
			if(trafficFlowDirection.equalsIgnoreCase(VAL_BOTH_DIRECTIONS)){
				
				for(String id : seq.railwayLinkIds){
					
					Id<Link> linkId = Id.createLinkId(id);
					Link ref = this.network.getLinks().get(linkId);
					
					if(ref != null){
						
						ref.setNumberOfLanes(nTracks);
						ref.setFreespeed(designSpeed);

						Link reverse = this.network.getFactory().createLink(Id.createLinkId(id + "_2"),
								ref.getToNode(), ref.getFromNode());
						reverse.setAllowedModes(this.modes);
						reverse.setCapacity(ref.getCapacity());
						reverse.setFreespeed(ref.getFreespeed());
						reverse.setLength(ref.getLength());
						reverse.setNumberOfLanes(ref.getNumberOfLanes());
						((LinkImpl)reverse).setOrigId(((LinkImpl)ref).getOrigId());
						
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

						Link reverse = this.network.getFactory().createLink(Id.createLinkId(id + "_2"),
								ref.getToNode(), ref.getFromNode());
						reverse.setAllowedModes(this.modes);
						reverse.setCapacity(ref.getCapacity());
						reverse.setFreespeed(designSpeed);
						reverse.setLength(ref.getLength());
						reverse.setNumberOfLanes(nTracks);
						((LinkImpl)reverse).setOrigId(((LinkImpl)ref).getOrigId());
						
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
		
	}
	
	private void createTransitStopFacilities(){
		
		for(RailwayStationNode s : this.stations.values()){
			
			Coord coord = s.coord;
			
			Node nearestNode = ((NetworkImpl)this.network).getNearestNode(coord);
			
			int i = 0;
			
			for(Link l : nearestNode.getInLinks().values()){
				
				TransitStopFacility facility = this.schedule.getFactory()
						.createTransitStopFacility(Id.create(s.id.replace(SPACE, "") + "_" + i, TransitStopFacility.class),
								s.coord, false);
				facility.setLinkId(l.getId());
				facility.setName(s.name);
				this.schedule.addStopFacility(facility);
				i++;
				
			}
			
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
			
			Coord coord = this.ct.transform(new Coord(Double.parseDouble(splitContent[i+1]),
					Double.parseDouble(splitContent[i])));
			
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
	
		String id = atts.getValue(ATT_XLINK);
		
		if(id != null){

			id = id.replace(PREFIX, "");
			if(this.currentLinkSeq != null){
				this.currentLinkSeq.railwayLinkIds.add(id);
			} else if(this.currentRailwayLine != null){
				this.currentRailwayLine.linkIds.add(id);
				this.linkId2LineId.put(id, this.currentRailwayLine.id);
			}
			
		} else{
			
			if(this.currentRailwayLine != null){
				
				id = atts.getValue("href").replace(PREFIX, "");
				System.out.println(id + "\t" + this.currentRailwayLine.id);
				
				
			}
			
		}
		
	}
	
	private void startTrafficFlowDirection(Attributes atts) {

		this.currentTfd = new TrafficFlowDirection();
		
	}
	
	private void endTrafficFlowDirection() {

		this.trafficFlowDirections.put(this.currentTfd.refId, this.currentTfd);
		this.currentTfd = null;
		
	}
	
	private void setLinkSeqRef(Attributes atts) {
		
		String refId = atts.getValue(ATT_XLINK).replace(PREFIX, "");
		
		if(this.currentTfd != null){
			
			this.currentTfd.refId = refId;
			
		} else if(this.currentNumberOfTracks != null){
			
			this.currentNumberOfTracks.refId = refId;
			
		} else if(this.currentDesignSpeed != null){
			
			this.currentDesignSpeed.refId = refId;
			
		}
		
	}
	
	private void setDirection(String content) {
		
		this.currentTfd.direction = content;
		
	}
	
	private void startDesignSpeed(Attributes att){
		
		this.currentDesignSpeed = new DesignSpeed();
		
	}
	
	private void endDesignSpeed(){
		
		this.designSpeeds.put(this.currentDesignSpeed.refId, this.currentDesignSpeed);
		this.currentDesignSpeed = null;
		
	}
	
	private void setDesignSpeed(String content){
		
		double speed = Double.parseDouble(content) / 3.6;
		this.currentDesignSpeed.v = speed;
		
	}
	
	private void startNumberOfTracks(Attributes atts){
		
		this.currentNumberOfTracks = new NumberOfTracks();
		
	}
	
	private void endNumberOfTracks(){
		
		this.numberOfTracks.put(this.currentNumberOfTracks.refId, this.currentNumberOfTracks);
		this.currentNumberOfTracks = null;
		
	}
	
	private void setNumberOfTracks(String content){
		
		int nTracks = Integer.parseInt(content);
		this.currentNumberOfTracks.nTracks = nTracks;
		
	}
	
	/**
	 * Network link.
	 * 
	 * @author dhosse
	 *
	 */
	private class RailwayLink{
		
		private String id;
		private String fromNodeId;
		private String toNodeId;
		private String lineId;
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
	
	/**
	 * Sequence of railway links.
	 * 
	 * @author dhosse
	 *
	 */
	class RailwayLinkSequence{
		
		private String id;
		private LinkedList<String> railwayLinkIds;
		
		RailwayLinkSequence(String id){
			this.id = id;
			this.railwayLinkIds = new LinkedList<>();
		}
		
	}
	
	/**
	 * Start and end points for railway links.
	 * 
	 * @author dhosse
	 *
	 */
	class RailwayNode{
		
		private String id;
		
		RailwayNode(String id){
			this.id = id;
		}
		
	}
	
	/**
	 * Determines the traffic flow direction of a railway link sequence.
	 * 
	 * @author dhosse
	 *
	 */
	class TrafficFlowDirection{
		
		private String refId;
		private String direction;
		
		TrafficFlowDirection(){};
		
	}
	
	/**
	 * Determines the maximum speed on a railway link sequence.
	 * 
	 * @author dhosse
	 *
	 */
	class DesignSpeed{
		
		private String refId;
		private double v;
		
		DesignSpeed(){};
		
	}
	
	/**
	 * Wrapper class that stores the number of tracks on a line sequence.
	 * 
	 * @author dhosse
	 *
	 */
	class NumberOfTracks{
		
		private String refId;
		private int nTracks;
		
		NumberOfTracks(){};
		
	}
	
	/**
	 * The node representation of a railway station.</br>
	 * 
	 * @author dhosse
	 *
	 */
	class RailwayStationNode{
		
		private String id;
		private String name;
		private Coord coord;
		
		RailwayStationNode(String id){
			this.id = id;
		}
		
	}
	
	class RailwayLine{
		
		private String id;
		private Set<String> linkIds;
		
		RailwayLine(){
			this.linkIds = new HashSet<>();
		}
		
	}

}
