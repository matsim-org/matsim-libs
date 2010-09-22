package playground.mmoyo.PTRouter;

//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
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
 * -logic layer network: with sequences of independent nodes for each TansitLine, includes transfer links
 * -A PTRouter object containing departure information according to the logicTransitSchedule
 * -logicToPlainConverter: translates logic nodes and links into plain nodes and links.
 */

public class LogicFactory{
	private NetworkImpl logicNet= NetworkImpl.createNetwork();
	private NetworkImpl plainNet= NetworkImpl.createNetwork();
	private LogicIntoPlainTranslator logicToPlainTranslator;

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

				//Creates an array of Transit Route departures
				int departuresSize= transitRoute.getDepartures().size();
				double[] departuresArray =new double[departuresSize];
				int i=0;
				for (Departure departure : transitRoute.getDepartures().values()){
					departuresArray[i++]=departure.getDepartureTime();
				}

				//iterates in each transit stop to create nodes and links
				int transitStopIndex=0;
				double lastDepartureOffset=0;
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) {
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();

					//Creates nodes
					Node plainNode= createPlainNode(transitStopFacility);

					Station logicStation= new Station (new IdImpl(newStopId++), transitStopFacility.getCoord());
					logicStation.setTransitRoute(transitRoute);
					logicStation.setTransitLine(transitLine);
					logicStation.setTransitRouteStop(transitRouteStop);
					logicStation.setPlainNode(plainNode);
					if (transitStopIndex==transitRoute.getStops().size()-1)logicStation.setLastStation(true);
					logicNet.addNode(logicStation);

					//Creates links
					if (transitStopIndex>0){
						Link plainLink= createPlainLink(lastPlainNode, plainNode);
						PTLink logicLink = new PTLink(new IdImpl(newLinkId++), lastLogicNode, logicStation, logicNet, PTValues.STANDARD_STR);
						logicLink.setTravelTime(transitRouteStop.getDepartureOffset() -lastDepartureOffset);
						logicLink.setPlainLink(plainLink);
						logicLink.setTransitLine(transitLine);
						logicLink.setTransitRoute(transitRoute);
						logicStation.setInStandardLink(logicLink);
					}else{
						logicStation.setFirstStation(true);
					}

					//saves departures in an array for each node
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

		//create transfer and DetTransfer links	ORIGINAL
		final String DETTRANS_PREFIX ="DT";
		final String TRANS_PREFIX ="T";
		/* This is the one working, commented only to try the compatibility with multinode router*/
		int transfers = 0;
		int detTransfers = 0;
		for (Node centerNode: logicNet.getNodes().values()){
			Collection<Node> nearNodes= logicNet.getNearestNodes(centerNode.getCoord(), PTValues.DETTRANSFER_RANGE );
			for (Node nearNode : nearNodes){
				Station fromNode= (Station)centerNode;
				Station toNode =  (Station)nearNode;
				if (fromNode.getTransitLine() != toNode.getTransitLine()) {
					if (!fromNode.isFirstStation() && !toNode.isLastStation()) {
						if (fromNode.getTransitRouteStop().getStopFacility() != toNode.getTransitRouteStop().getStopFacility()) {
							new PTLink(new IdImpl(DETTRANS_PREFIX + ++newDetTransfLinkId),centerNode, nearNode, logicNet, PTValues.DETTRANSFER_STR);
							detTransfers++;
						}else{
							new PTLink(new IdImpl(TRANS_PREFIX + ++newTransfLinkId), centerNode, nearNode, logicNet, PTValues.TRANSFER_STR);
							transfers++;
						}
					}
				}
			}
		}

		////////////Temporarly to match the multinode dikjstra router///////////////////////////////////////////////////////////////////////////////
		/*
		List<Tuple<Node, Node>> toBeAdded = new LinkedList<Tuple<Node, Node>>();
		// connect all stops with walking links if they're located less than beelineWalkConnectionDistance from each other
		for (Node node : logicNet.getNodes().values()) {
			if (node.getInLinks().size() > 0) { // only add links from this node to other nodes if agents actually can arrive here
				for (Node node2 : logicNet.getNearestNodes(node.getCoord(), PTValues.DETTRANSFER_RANGE)) {
					if ((node != node2) && (node2.getOutLinks().size() > 0)) { // only add links to other nodes when agents can depart there
						Station fromNode= (Station)node;
						Station toNode =  (Station)node2;
						if ((fromNode.getTransitLine() != toNode.getTransitLine()) || (fromNode.getTransitStopFacility() != toNode.getTransitStopFacility())) {
							// do not yet add them to the network, as this would change in/out-links
							toBeAdded.add(new Tuple<Node, Node>(node, node2));
						}
					}
				}
			}
		}
		for (Tuple<Node, Node> tuple : toBeAdded) {
			new PTLink(new IdImpl(TRANS_PREFIX + ++newTransfLinkId), tuple.getFirst(), tuple.getSecond(), logicNet, PTValues.TRANSFER_STR);
		}
		*/
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




		//System.out.println("\n\n\n\n\ntransfers: "  + transfers + " detTransfers: " + detTransfers + " total: " + ( transfers + detTransfers));
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
		new NetworkWriter(logicNet).write(outLogicNetFile);
		new NetworkWriter(plainNet).write(outPlainNetFile);
		System.out.println("done.");
	}

	/****************get methods************/
	public NetworkImpl getLogicNet(){
		return this.logicNet;
	}

	public NetworkImpl getPlainNet(){
		return this.plainNet;
	}

	public LogicIntoPlainTranslator getLogicToPlainTranslator(){
		if (this.logicToPlainTranslator==null){
			this.logicToPlainTranslator = new LogicIntoPlainTranslator(plainNet);
		}
		return this.logicToPlainTranslator;
	}
}
