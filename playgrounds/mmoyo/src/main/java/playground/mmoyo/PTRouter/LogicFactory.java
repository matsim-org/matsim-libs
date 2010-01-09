package playground.mmoyo.PTRouter;

//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;

//import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * Reads a TransitSchedule and creates from it:
 *-Plain network: A ptNode represent a station. A link represents a simple connection between stations 
 * -logic layer network: with sequences of independent nodes for each TansitLine, includes transfer links
 * -A PTRouter object containing departure information according to the logicTransitSchedule
 * -logicToPlainConverter: translates logic nodes and links into plain nodes and links. 
 */

public class LogicFactory{
	private NetworkLayer logicNet= new NetworkLayer();
	private NetworkLayer plainNet= new NetworkLayer();
	private LogicIntoPlainTranslator logicToPlainTranslator; 
	//03 dic private Map<Id,List<PTNode>> facilityNodeMap = new TreeMap<Id,List<PTNode>>(); /* <key =PlainStop, value = List of logicStops to be joined by transfer links>*/
	
	long newLinkId=0;
	long newTransfLinkId=0;
	long newDetTransfLinkId=0;
	long newPlainLinkId=0;
	long newStopId=0;

	/**Creates a logic network file and a logic TransitSchedule file with individualized id's for nodes and stops**/
	public LogicFactory(final TransitSchedule transitSchedule){
		double startTime = System.currentTimeMillis();
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				Node lastLogicNode = null;
				Node lastPlainNode=null;
				
				
				/**Creates an array of Transit Route departures*/
				int departuresSize= transitRoute.getDepartures().size();
				double[] departuresArray =new double[departuresSize];
				int i=0;
				for (Departure departure : transitRoute.getDepartures().values()){
					departuresArray[i++]=departure.getDepartureTime();
					//03 dici++;
				}
				
				//iterates in each transit stop to create nodes and links */
				int transitStopIndex=0;
				double lastDepartureOffset=0;
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) { 
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility(); 
					
					/**Creates nodes*/
					//plain node
					Node plainNode= createPlainNode(transitStopFacility);
					
					//logicNode
					//Coord coord = transitStopFacility.getCoord();			03 dic
					//Id idStopFacility = transitStopFacility.getId();    03 dic
					//Id logicId= new IdImpl(newStopId++); 
					Station logicStation= new Station (new IdImpl(newStopId++), transitStopFacility.getCoord());
					logicStation.setTransitRoute(transitRoute);
					logicStation.setTransitLine(transitLine);
					logicStation.setTransitRouteStop(transitRouteStop);
					logicStation.setPlainNode(plainNode);
					if (transitStopIndex==transitRoute.getStops().size()-1)logicStation.setLastStation(true);
					logicNet.addNode(logicStation);
					 
					//fills the facilityNodeMap to create transfer links
					/* 03 dic
					if (!facilityNodeMap.containsKey(idStopFacility)){
						List<PTNode> nodeStationArray = new ArrayList<PTNode>();
						facilityNodeMap.put(idStopFacility, nodeStationArray);
					}
					facilityNodeMap.get(idStopFacility).add(logicNode);
					 */

					/**Creates links*/
					if (transitStopIndex>0){
						Link plainLink= createPlainLink(lastPlainNode, plainNode);
						//03 dic Id logicLinkId = new IdImpl(newLinkId++);
						PTLink logicLink = new PTLink(new IdImpl(newLinkId++), lastLogicNode, logicStation, logicNet, PTValues.STANDARD_STR);
						logicLink.setTravelTime(transitRouteStop.getDepartureOffset() -lastDepartureOffset);
						logicLink.setPlainLink(plainLink);
						logicLink.setTransitLine(transitLine);
						logicLink.setTransitRoute(transitRoute);
						logicStation.setInStandardLink(logicLink);
					}else{
						logicStation.setFirstStation(true);
					}

					/**saves departures in an array for each node*/
					double[] nodeDeparturesArray =new double[departuresSize];
					for (int j=0; j<departuresSize; j++){
						double departureTime =departuresArray[j] + transitRouteStop.getDepartureOffset();
						if (departureTime > 86400) departureTime -=86400;
						nodeDeparturesArray[j] = departureTime; 
					} 
					Arrays.sort(nodeDeparturesArray);
					logicStation.setArrDep(nodeDeparturesArray);
					
					transitStopIndex++;
					lastLogicNode= logicStation;
					lastPlainNode= plainNode;
					lastDepartureOffset = transitRouteStop.getDepartureOffset();
				}
			}
		}
		
		//create transfer links
		/*03 dic
		for (List<PTNode> chList : facilityNodeMap.values()){ 
			for (PTNode fromNode : chList) {
				for (PTNode toNode : chList){ 
					if (fromNode.getTransitLine() != toNode.getTransitLine()){  //If they belongs to Same Line they won't be joined. This condition also joining the same node
						if (!fromNode.isFirstStation() && !toNode.isLastStation()){ //make sure that they will joing two standard links
							new PTLink(new IdImpl("T" + ++newLinkId), fromNode, toNode, logicNet, "Transfer", avWalkSpeed );
						}
					}
				}
			}
		}
		facilityNodeMap = null;
		*/
	
		//create transfer and DetTransfer links
		
		final String DETTRANS_PREFIX ="DT";
		final String TRANS_PREFIX ="T";
		
		for (NodeImpl centerNode: logicNet.getNodes().values()){
			Collection<NodeImpl> nearNodes= logicNet.getNearestNodes(centerNode.getCoord(), PTValues.DETTRANSFER_RANGE );
			for (NodeImpl nearNode : nearNodes){
				Station fromNode= (Station)centerNode;
				Station toNode =  (Station)nearNode;
				if (fromNode.getTransitLine() != toNode.getTransitLine()) { 
					if (!fromNode.isFirstStation() && !toNode.isLastStation()) {
						if (fromNode.getTransitRouteStop().getStopFacility() != toNode.getTransitRouteStop().getStopFacility()) {
							new PTLink(new IdImpl(DETTRANS_PREFIX + ++newDetTransfLinkId),centerNode, nearNode, logicNet, PTValues.DETTRANSFER_STR);
						}else{
							new PTLink(new IdImpl(TRANS_PREFIX + ++newTransfLinkId), centerNode, nearNode, logicNet, PTValues.TRANSFER_STR);
						}
					}
				}
			}
		}
		
		System.out.println("duration of logic net creation: " + (System.currentTimeMillis()-startTime));
	}
	
	/**returns -or creates if does not exist- a plain link between two nodes*/
	private Link createPlainLink(Node fromNode, Node toNode){
		Link linkImpl = null;
		if (!((NodeImpl) fromNode).getOutNodes().containsValue(toNode)){
			linkImpl= plainNet.createAndAddLink(new IdImpl(newPlainLinkId++), fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()), 1.0, 1.0 , 1);
		}
		else{
			for (Link outLink : fromNode.getOutLinks().values()){
				if (outLink.getToNode().equals(toNode)){
					return outLink;
				}
			}
		}
		return linkImpl;
	}
	
	private Node createPlainNode(TransitStopFacility transitStopFacility){
		Node plainNode = null;
		Id id = transitStopFacility.getId();
		if (this.plainNet.getNodes().containsKey(id)){
			plainNode = plainNet.getNodes().get(id);
		}else{
			plainNode = plainNet.createAndAddNode(id, transitStopFacility.getCoord());
		}
		return plainNode;
	}
	
	public void writeLogicElements(final String outPlainNetFile, final String outTransitScheduleFile, final String outLogicNetFile ){
		new NetworkWriter(logicNet).writeFile(outLogicNetFile);
		new NetworkWriter(plainNet).writeFile(outPlainNetFile);
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
		if (this.logicToPlainTranslator==null){
			this.logicToPlainTranslator = new LogicIntoPlainTranslator(plainNet);
		}
		return this.logicToPlainTranslator;
	}
}
