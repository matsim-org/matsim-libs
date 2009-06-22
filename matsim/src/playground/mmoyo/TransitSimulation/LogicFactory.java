package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitScheduleWriterV1;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.matsim.core.api.network.Link;

/**
 * Reads a TransitSchedule and creates:
 * -Plain network: A node represent a station. A link represents a path between stations 
 * -logic layer network: a sequence of independent nodes for each TansitLine
 * -logic layer TransitSchedule: a Facility for each transit stop with new Id's mapped to the real transitFaciliteies
 * @param transitSchedule 
 */
public class LogicFactory{
	private TransitSchedule transitSchedule;
	private NetworkLayer logicNet= new NetworkLayer();
	private NetworkLayer plainNet= new NetworkLayer();
	private TransitSchedule logicTransitSchedule = new TransitSchedule(); 
	private Map<Id,List<Node>> facilityNodeMap = new TreeMap<Id,List<Node>>(); /** key= PlainStop value = List of logicStops to be joined by transfer links*/
	private Map<Id,Id> logicStopMap = new TreeMap<Id,Id>(); 
	private Map<Id,Id> nodeLineMap = new TreeMap<Id,Id>();
	private MultiKeyMap multiKeyMap;
	long newLinkId=0;
	long newPlainLinkId=0;
	long newStopId=0;
	
	public LogicFactory(final TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
		createLogicNet();
	}
	
	/**Creates a logic network file and a logic TransitSchedule file with individualized id's for nodes and stops*/
	private void createLogicNet(){
	
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			TransitLine logicTransitLine = new TransitLine(transitLine.getId()); 
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				List<TransitRouteStop> logicTransitStops = new ArrayList<TransitRouteStop>();
				Node lastNode = null;
				Node lastPlainNode=null;
				boolean first= true;
				
				/**iterates in each transit stop to create nodes and links */
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) { 
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility(); 
					
					/** Create node*/
					Coord coord = transitStopFacility.getCoord();
					Id idStopFacility = transitStopFacility.getId();  
					Id logicalId= createLogicalId(idStopFacility, transitRoute.getId()); 
					
					Node node= logicNet.createNode(logicalId, coord);
					node.setType(idStopFacility.toString());
					
					Node plainNode= createPlainNode(transitStopFacility);
					
					/**fill the facilityNodeMap  this will identify all nodes of a station with the same coordinates this will help to create transfer links*/
					if (!facilityNodeMap.containsKey(idStopFacility)){
						List<Node> nodeStationArray = new ArrayList<Node>();
						facilityNodeMap.put(idStopFacility, nodeStationArray);
					}
					facilityNodeMap.get(idStopFacility).add(node);
					
					if (!first){
						createLogicLink(new IdImpl(newLinkId++), lastNode, node, "Standard");
						createPlainLink(lastPlainNode, plainNode);
					}else{
						first=false;
					}

					/**create logical stops and stopFacilities*/
					TransitStopFacility logicTransitStopFacility = new TransitStopFacility(logicalId, coord); 
					logicTransitSchedule.addStopFacility(logicTransitStopFacility);
					TransitRouteStop logicTransitRouteStop = new TransitRouteStop(logicTransitStopFacility, transitRouteStop.getArrivalDelay(), transitRouteStop.getDepartureDelay()); 
					logicTransitStops.add(logicTransitRouteStop);
					
					lastNode= node;
					lastPlainNode= plainNode;
				}
				TransitRoute logicTransitRoute = new TransitRoute(transitRoute.getId(), null, logicTransitStops, transitRoute.getTransportMode());
				for (Departure departure: transitRoute.getDepartures().values()) {
					logicTransitRoute.addDeparture(departure);
				}
				logicTransitRoute.setDescription(transitRoute.getDescription());
				logicTransitLine.addRoute(logicTransitRoute);
			}
			logicTransitSchedule.addTransitLine(logicTransitLine);
		}
		
		createTransferLinks();
		createDetachedTransferLinks(400);
	}
	
	/**Created a new id for a new node. Besides important values are stores in logicStopMap. */ 
	private Id createLogicalId(final Id idStopFacility, final Id lineId){
		Id newId = new IdImpl(newStopId++);
		logicStopMap.put(newId, idStopFacility);
		nodeLineMap.put(newId, lineId);
		return newId;
	}
	
	private void createTransferLinks(){
		for (List<Node> chList : facilityNodeMap.values()) 
			for (Node fromNode : chList) 
				for (Node toNode : chList) 
					if (!fromNode.equals(toNode)){
						Id idNewLink = new IdImpl("T" + ++newLinkId);
						createLogicLink(idNewLink, fromNode, toNode, "Transfer");
					}
		facilityNodeMap = null;
	}
	
	public void createDetachedTransferLinks (final double distance){
		for (Node centerNode: logicNet.getNodes().values()){
			Collection<Node> nearNodes= logicNet.getNearestNodes(centerNode.getCoord(), distance);
			nearNodes.remove(centerNode);
			for (Node nearNode : nearNodes){
				boolean areConected = centerNode.getOutNodes().containsValue(nearNode); 
				boolean belongToSameLine = nodeLineMap.get(centerNode.getId()) == nodeLineMap.get(nearNode.getId()); 
				if (!areConected && !belongToSameLine){  /**avoid joining nodes that are already joined by standard links AND joining nodes of the same Line*/
					Id idNewLink = new IdImpl("DT" + ++newLinkId);
					createLogicLink(idNewLink, centerNode, nearNode, "DetTransfer");
				}
			}
		}
		//NodeLineMap= null;??
	}
	
	/**links for the logical network, one for transitRoute*/
	private void createLogicLink(Id id, Node fromNode, Node toNode, String type){
		double length= CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		logicNet.createLink(id, fromNode, toNode, length , 1.0 , 1.0, 1.0, "0", type);	
	}
	
	/**links for the plain network, only one between two stations*/
	private void createPlainLink(Node fromNode, Node toNode){
		if (!fromNode.getOutNodes().containsValue(toNode)){
			double length= CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
			Link plainLink = plainNet.createLink(new IdImpl(newPlainLinkId++), fromNode, toNode, length, 1.0, 1.0 , 1);
			
		}
	}
	
	private Node createPlainNode(TransitStopFacility transitStopFacility){
		Node node = null;
		Id id = transitStopFacility.getId();
		if (this.plainNet.getNodes().containsKey(id)){
			node = plainNet.getNode(id);
		}else{
			node = plainNet.createNode(id, transitStopFacility.getCoord());
		}
		return node;
	}
	
	public void writeLogicElements(final String outPlainNetFile, final String outTransitScheduleFile, final String outLogicNetFile ){
		/**Writes logicTransitSchedule*/
		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (this.logicTransitSchedule);
		try{
			transitScheduleWriterV1.write(outTransitScheduleFile);
		} catch (IOException ex) {
			System.out.println(this + ex.getMessage());
		}
		new NetworkWriter(logicNet, outLogicNetFile).write();
		//new NetworkWriter(plainNet, outPlainNetFile).write();   //->for the time being the plainNet is itself the input
		System.out.println("done.");
	}

	public NetworkLayer getLogicNet() {
		return this.logicNet;
	}

	public NetworkLayer getPlainNet(){
		return this.plainNet;
	}

	public TransitSchedule getLogicTransitSchedule(){
		return this.logicTransitSchedule;
	}
	
	/** logicStopMap stores the relationship between the new "logical stops" and the real transit stop*/
	public Map<Id,Id> getLogicStopMap(){
		return this.logicStopMap;
	}
	
}
