package playground.mmoyo.input;

import java.util.*;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.api.network.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.*;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTCase2.*;
import playground.mmoyo.PTRouter.*;
import org.matsim.core.network.NetworkLayer;
/** 
 * Second version of network factory for the PTCase2 (with no relationship to street network) 
 * Represent a network layer with independent route with transfer links at intersections 
 * @param inFileName path of file with ptlines information 
 * @param ptTimeTable empty timetable object to be filled with inFileName data 
 * @param outFileName path of network file to be created
 */ 
public class PTNetworkFactory2 {
	private static Logger log = Logger.getLogger(PTNetworkFactory2.class);
	
	public Map <Id,Double> linkTravelTimeMap = new TreeMap <Id,Double>();

	public PTNetworkFactory2(){
		super();
	}
	
	public NetworkLayer createNetwork(String inFileName, PTTimeTable2 ptTimeTable, String outFileName){
		NetworkLayer ptNetworkLayer1 = readNetFile(inFileName);
		readTimeTable(ptNetworkLayer1, ptTimeTable);
		createTransferLinks(ptNetworkLayer1, ptTimeTable);
		return ptNetworkLayer1;
	}

	public NetworkLayer readNetwork(String inFileName, PTTimeTable2 ptTimeTable){
		NetworkLayer ptNetworkLayer = readNetFile(inFileName);
		readTimeTable(ptNetworkLayer, ptTimeTable);
		return ptNetworkLayer;
	}
	
	public NetworkLayer readNetFile(String inFileName){
		NetworkFactory networkFactory = new NetworkFactory();
	
		NetworkLayer tempNet= new NetworkLayer(networkFactory);
		NetworkLayer ptNetworkLayer= new NetworkLayer(networkFactory);
		
		//Create a temporal network with normal Nodes
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(tempNet);
		matsimNetworkReader.readFile(inFileName);
		
		//Create the PTNetwork with PTNodes
		//List<PTNode> ptNodeList = new ArrayList<PTNode>();
		for (Node node: tempNet.getNodes().values()){
			PTNode ptNode = new PTNode(new IdImpl(node.getId().toString()),node.getCoord(),node.getType());
			ptNetworkLayer.getNodes().put(node.getId(),ptNode);
		}
	
		//Add Links
		for (Link l: tempNet.getLinks().values()){
			createPTLink(ptNetworkLayer, l.getId().toString(), l.getFromNode().getId(), l.getToNode().getId(), l.getType());
		}

		tempNet= null;
		networkFactory= null;
		matsimNetworkReader= null;
		return ptNetworkLayer;
	}

	/*
	//->This should be the new version
	public NetworkLayer createNetwork(String inFileName, PTTimeTable2 ptTimeTable, String OutFileName){
		PTNetworkReader ptNetworkReader = new PTNetworkReader();
		NetworkLayer ptNetworkLayer = ptNetworkReader.readNetFile(inFileName);
		readTimeTable(ptNetworkLayer, ptTimeTable);
		createTransferLinks(ptNetworkLayer, ptTimeTable);
		return ptNetworkLayer;
	}
	
	public NetworkLayer readNetwork(String inFileName, PTTimeTable2 ptTimeTable){
		PTNetworkReader ptNetworkReader = new PTNetworkReader();
		NetworkLayer ptNetworkLayer = ptNetworkReader.readNetFile(inFileName);
		readTimeTable(ptNetworkLayer, ptTimeTable);
		return ptNetworkLayer;
	}
	*/
	
	/**
	 * Reads the timetable File, validates that every node exists and loads the data in the ptTimeTable object
	 */	
	public void readTimeTable(final Network ptNetworkLayer, PTTimeTable2 ptTimeTable){
		PTNode ptLastNode = null;
		for (PTLine ptLine :  ptTimeTable.getPtLineList()) {
			//Test code
			/*
			System.out.println(ptLine.getId().toString());
			System.out.println(ptLine.getDirection());
			System.out.println(ptLine.getDepartures().toString());
			System.out.println(ptLine.getMinutes().toString() + "\n");
			*/
			/*Creates a map with travel times for every standard link */
			int indexMin=0;
			double travelTime=0;
			double lastTravelTime=0;
			boolean first=true;
			
			for (Id idNode : ptLine.getNodeRoute()) {
				PTNode ptNode = ((PTNode)ptNetworkLayer.getNode(idNode));
				if (ptNode==null){
					throw new java.lang.NullPointerException("Node does not exist:" + idNode); 
				}
				ptNode.setIdPTLine(ptLine.getId());
				double min = ptLine.getMinutes().get(indexMin);
				
				travelTime=min-lastTravelTime;
				if (!first){
					for (Link link : (ptNode.getInLinks().values())) {
						if (link.getFromNode().equals(ptLastNode)){
							linkTravelTimeMap.put(link.getId(), travelTime);
							//-->check if this temporary assignment improves the performance
							link.setCapacity(travelTime);
						}
					}
				}
				ptLastNode= ((PTNode)ptNetworkLayer.getNode(idNode));
				lastTravelTime= min;
				first=false;
				indexMin++;
			}
		}
	
		/**Calculates the travel time of each link and stores these data in ptTimeTable*/
		ptTimeTable.calculateTravelTimes(ptNetworkLayer);  //??
		ptTimeTable.setMaps(linkTravelTimeMap);
		//return ptTimeTable;
	}
	
	public void createTransferLinks(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimeTable) {
		PTStation stationMap = new PTStation(ptTimeTable);
		int maxLinkKey=0;
		for (List<Id> chList : stationMap.getIntersecionMap().values()) {
			if (chList.size() > 1) {
				for (Id idNode1 : chList) {
					for (Id idNode2 : chList) {
						if (!idNode1.equals(idNode2)){
							createPTLink(ptNetworkLayer, ("T" + maxLinkKey++), idNode1, idNode2,"Transfer");
						}
					}
				}
			}
		}
	}
	
	public void createPTLink(NetworkLayer net, String strIdLink, Id idFromNode, Id idToNode, String type){
		PTNode fromNode= (PTNode)net.getNode(idFromNode);
		PTNode toNode= (PTNode)net.getNode(idToNode);
		createPTLink(net, strIdLink, fromNode, toNode, type);
	}
	
	public void createPTLink(NetworkLayer net, String strIdLink, PTNode fromNode, PTNode toNode, String type){
		//->move to link factory
		if (fromNode==null)
			throw new java.lang.NullPointerException("fromNode does not exist in link:" + strIdLink); 
		if (toNode==null)
			throw new java.lang.NullPointerException("toNode does not exist in link:" + strIdLink); 
		Id idLink = new IdImpl(strIdLink);
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";
		net.createLink(idLink, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type);
		
		// the following moves nodes a bit apart so that all the different links that are on top of each other
		// get some distance between them.  kai, jun'09
//		if ( true ) {
//			double dx = - fromNode.getCoord().getX() + toNode.getCoord().getX() ;
//			double dy = - fromNode.getCoord().getY() + toNode.getCoord().getY() ;
//
//			// normalize
//			double sqrtTmp = Math.sqrt( dx*dx + dy*dy ) ;
//			if ( sqrtTmp > 0. ) {
//				dx = dx / sqrtTmp ;
//				dy = dy / sqrtTmp ;
//			} else {
//				dx = 0. ;
//				dy = 0. ;
//			}
//			
//			Coord newCoord ; 
//
//			newCoord = new CoordImpl( toNode.getCoord().getX() + dy*(0.5+Math.random()) , toNode.getCoord().getY() - dx*(0.5+Math.random()) ) ;
//			toNode.setCoord( newCoord ) ;
//
//			newCoord = new CoordImpl( fromNode.getCoord().getX() + dy*(0.5+Math.random()) , fromNode.getCoord().getY() - dx*(0.5+Math.random()) ) ;
//			fromNode.setCoord( newCoord ) ;
//		}

	}

	private void createPTLink(Network net, int intId, BasicNode fromBasicNode, BasicNode toBasicNode, String type){
		//-> use this unique method and eliminate the last two
		Id id =  new IdImpl(intId);
		Link  link = net.getFactory().createLink(id, fromBasicNode.getId(), toBasicNode.getId() );
		link.setLength(CoordUtils.calcDistance(fromBasicNode.getCoord(), toBasicNode.getCoord()));
		link.setType(type);
	}

	public void writeNet(Network net, String fileName){
		System.out.println("writing pt network...");
		new NetworkWriter(net, fileName).write();
		System.out.println("done.");
	}
	
	public void createDetachedTransfers (NetworkLayer net, double distance){
		//--> move this to class Linkfactory
		int x=0;
		String strId;
		for (Node node: net.getNodes().values()){
			for (Node nearNode : net.getNearestNodes(node.getCoord(), distance)){
				PTNode ptn1 = (PTNode) node;
				PTNode ptn2 = (PTNode) nearNode;
				//if(!ptn1.getIdStation().equals(ptn2.getIdStation())){   
					if (!node.getCoord().equals(nearNode.getCoord())){  //Validate this in the station validator!      
						if (!node.getOutNodes().containsValue(nearNode)){
							strId= "DT" + ++x;
							createPTLink(net, strId, ptn1, ptn2, "DetTransfer");
							//strId= "DT" + ++x;
							//createPTLink(net, strId, ptn2, ptn1, "DetTransfer");
						}
					//}
				}
			}	
		}
	}

	/**
	 * Set the next standard link of a detTransfer link. 
	 * This was the first intent to get valid paths 
	 */
	@Deprecated
	public void setDetNextLinks (NetworkLayer net, PTTimeTable2 ptTimeTable){
		List <Link> eliminateList = new ArrayList<Link>();
		for (Link link: net.getLinks().values()){
			if (link.getType().equals("DetTransfer")){
				Link nextLink= null;
				int numStandards =0;
				for (Link outLink : link.getToNode().getOutLinks().values()) {
					if (outLink.getType().equals("Standard")){
						numStandards++;
						nextLink=outLink;
					}
				}
				if (numStandards>1)
					throw new java.lang.NullPointerException(link.getId() + "DetLink has no valid outLinks");				
				
				if (nextLink!=null){
					ptTimeTable.putNextDTLink(link.getId(), nextLink);
				}else{
					eliminateList.add(link);
				}
				
			}
		}

		System.out.println("eliminate" + eliminateList.size());
		for (Link link:eliminateList){
			net.removeLink(link);
		}
	}

	/**
	 * Displays a quick visualization of links with from- and to- nodes 
	 */
	public static void printLinks(NetworkLayer ptNetworkLayer) {
		//-> unify with first version of ptNetFactory
		for (Link l : ptNetworkLayer.getLinks().values()) {
			System.out.print("\n(" ); 
			System.out.print(l.getFromNode().getId().toString()); 
			System.out.print( ")----" ); 
			System.out.print( l.getId().toString() ); 
			System.out.print( "--->("); 
			System.out.print( l.getToNode().getId().toString() ); 
			System.out.print( ")   " + l.getType() );
		}
	}
	
}
