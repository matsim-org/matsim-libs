package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.TransitScheduleBuilderImpl;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.PTRouter.PTTimeTable;

/**
 * Reads a TransitSchedule and creates from it:
 * -Plain network: A node represent a station. A link represents a simple connection between stations 
 * -logic layer network: with sequences of independent nodes for each TansitLine, includes transfer links
 * -logicTransitSchedule: with a cloned stop facility for each transit stop of each Transit Line with new Id's mapped to the real transitFacilities
 * -A PTRouter object containing departure information according to the logicTransitSchedule
 * -logicToPlainConverter: translates logic nodes and links into plain nodes and links. 
 */

public class LogicFactory{
	private TransitSchedule transitSchedule;
	private NetworkLayer logicNet= new NetworkLayer();
	private NetworkLayer plainNet= new NetworkLayer();
	private TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
	private TransitSchedule logicTransitSchedule = builder.createTransitSchedule();
	private LogicIntoPlainTranslator logicToPlainTranslator; 
	
	private Map<Id,List<NodeImpl>> facilityNodeMap = new TreeMap<Id,List<NodeImpl>>(); /** <key =PlainStop, value = List of logicStops to be joined by transfer links>*/
	public Map<Id,NodeImpl> logicToPlanStopMap = new TreeMap<Id,NodeImpl>();    // stores the equivalent plainNode of a logicNode   <logic, plain>
	private Map<Id,Id> nodeLineMap = new TreeMap<Id,Id>();                  
	
	long newLinkId=0;
	long newPlainLinkId=0;
	long newStopId=0;
	
	final String STANDARDLINK = "Standard";
	
	public LogicFactory(final TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
		createLogicNet();
		this.logicToPlainTranslator = new LogicIntoPlainTranslator(plainNet,  logicToPlanStopMap);
	}
	
	//Creates a logic network file and a logic TransitSchedule file with individualized id's for nodes and stops*/
	private void createLogicNet(){
	
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			TransitLine logicTransitLine = this.builder.createTransitLine(transitLine.getId()); 
			
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				List<TransitRouteStop> logicTransitStops = new ArrayList<TransitRouteStop>();
				NodeImpl lastLogicNode = null;
				NodeImpl lastPlainNode=null;
				boolean first= true;
				
				//iterates in each transit stop to create nodes and links */
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) { 
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility(); 
					
					//Creates nodes
					Coord coord = transitStopFacility.getCoord();
					Id idStopFacility = transitStopFacility.getId();  
					Id logicalId= createLogicalId(idStopFacility, transitLine.getId()); 
					NodeImpl logicNode= logicNet.createNode(logicalId, coord);
					NodeImpl plainNode= createPlainNode(transitStopFacility);
					logicToPlanStopMap.put(logicNode.getId(), plainNode);
					
					//fills the facilityNodeMap to create transfer links later on
					if (!facilityNodeMap.containsKey(idStopFacility)){
						List<NodeImpl> nodeStationArray = new ArrayList<NodeImpl>();
						facilityNodeMap.put(idStopFacility, nodeStationArray);
					}
					facilityNodeMap.get(idStopFacility).add(logicNode);

					//Creates links
					if (!first){
						createLogicLink(new IdImpl(newLinkId++), lastLogicNode, logicNode, "Standard");
						createPlainLink(lastPlainNode, plainNode);
					}else{
						first=false;
					}

					//creates logic stops and stopFacilities
					TransitStopFacility logicTransitStopFacility = this.builder.createTransitStopFacility(logicalId, coord); 
					logicTransitSchedule.addStopFacility(logicTransitStopFacility);
					TransitRouteStop logicTransitRouteStop = this.builder.createTransitRouteStop(logicTransitStopFacility, transitRouteStop.getArrivalDelay(), transitRouteStop.getDepartureDelay()); 
					logicTransitStops.add(logicTransitRouteStop);
					
					lastLogicNode= logicNode;
					lastPlainNode= plainNode;
				}
				TransitRoute logicTransitRoute = this.builder.createTransitRoute(transitRoute.getId(), null, logicTransitStops, transitRoute.getTransportMode());
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
	
	//Created a new id for a new node. the nodeLinMap stores the transitLine of each node. */ 
	private Id createLogicalId(final Id idStopFacility, final Id lineId){
		Id newId = new IdImpl(newStopId++);
		nodeLineMap.put(newId, lineId);
		return newId;
	}
	
	private void createTransferLinks(){
		for (List<NodeImpl> chList : facilityNodeMap.values()) 
			for (NodeImpl fromNode : chList) 
				for (NodeImpl toNode : chList){ 
					boolean belongToSameLine = nodeLineMap.get(fromNode.getId()) == nodeLineMap.get(toNode.getId());
					if (!belongToSameLine && !fromNode.equals(toNode)  && willJoin2StandardLinks(fromNode, toNode)){
						Id idNewLink = new IdImpl("T" + ++newLinkId);
						createLogicLink(idNewLink, fromNode, toNode, "Transfer");
					}
				}
		facilityNodeMap = null;
	}
	
	public void createDetachedTransferLinks (final double distance){
		for (NodeImpl centerNode: logicNet.getNodes().values()){
			Collection<NodeImpl> nearNodes= logicNet.getNearestNodes(centerNode.getCoord(), distance);
			nearNodes.remove(centerNode);
			for (NodeImpl nearNode : nearNodes){
				boolean areConected = centerNode.getOutNodes().containsValue(nearNode); 
				boolean belongToSameLine = nodeLineMap.get(centerNode.getId()) == nodeLineMap.get(nearNode.getId()); 
				if (!belongToSameLine && !areConected && willJoin2StandardLinks(centerNode, nearNode)){  /**avoid joining nodes that are already joined by standard links AND joining nodes of the same Line*/
					Id idNewLink = new IdImpl("DT" + ++newLinkId);
					createLogicLink(idNewLink, centerNode, nearNode, "DetTransfer");
				}
			}
		}
		//NodeLineMap= null;??
	}
	
	/**Asks if the fromNode has at least one standard inLink and also if the toNode has at least a standard outLink
	 * Otherwise it is senseless to create a transfer link between them */
	private boolean willJoin2StandardLinks(NodeImpl fromNode, NodeImpl toNode){
		int numInGoingStandards =0;
		for (LinkImpl inLink : fromNode.getInLinks().values()){
			if (inLink.getType().equals(STANDARDLINK)) 	numInGoingStandards++;
		}	
		
		int numOutgoingStandards =0;
		for (LinkImpl outLink : toNode.getOutLinks().values()){
			if (outLink.getType().equals(STANDARDLINK)) numOutgoingStandards++;
		}
		
		return (numInGoingStandards>0) && (numOutgoingStandards>0); 
	}
	
	/**Creates the links for the logical network, one for transitRoute*/
	private void createLogicLink(Id id, NodeImpl fromNode, NodeImpl toNode, String type){
		double length= CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		logicNet.createLink(id, fromNode, toNode, length , 1.0 , 1.0, 1.0, "0", type);	
	}
	
	/**Creates the links for the plain network, only one between two stations*/
	private void createPlainLink(NodeImpl fromNode, NodeImpl toNode){
		if (!fromNode.getOutNodes().containsValue(toNode)){
			double length= CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
			plainNet.createLink(new IdImpl(newPlainLinkId++), fromNode, toNode, length, 1.0, 1.0 , 1);
		}
	}
	
	private NodeImpl createPlainNode(TransitStopFacility transitStopFacility){
		NodeImpl plainNode = null;
		Id id = transitStopFacility.getId();
		if (this.plainNet.getNodes().containsKey(id)){
			plainNode = plainNet.getNode(id);
		}else{
			plainNode = plainNet.createNode(id, transitStopFacility.getCoord());
		}
		return plainNode;
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
		new NetworkWriter(plainNet, outPlainNetFile).write();
		System.out.println("done.");
	}

	/****************get methods************/
	public NetworkLayer getLogicNet(){
		return this.logicNet;
	}

	public NetworkLayer getPlainNet(){
		return this.plainNet;
	}
	
	public LogicIntoPlainTranslator getLogicToPlainTranslator(){
		return this.logicToPlainTranslator ;
	}

	public PTRouter getPTRouter(){
		PTTimeTable logicPTTimeTable = new PTTimeTable();
		TransitTravelTimeCalculator transitTravelTimeCalculator = new TransitTravelTimeCalculator(logicTransitSchedule,logicNet);
		transitTravelTimeCalculator.fillTimeTable(logicPTTimeTable);
		PTRouter ptRouter = new PTRouter(logicNet, logicPTTimeTable, logicToPlainTranslator);
		return ptRouter; 
	}

}
