package playground.mmoyo.input;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTCase2.PTStation;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.PTRouter.PTLine;

public class PTNetworkFactory2 {
	public Map <Id,Double> linkTravelTimeMap = new TreeMap <Id,Double>();

	public PTNetworkFactory2(){
		super();
	}
	
	public NetworkLayer createNetwork(String inFileName, PTTimeTable2 ptTimeTable, String OutFileName){
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
	
	private NetworkLayer readNetFile(String inFileName){
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
	//This should be the new vrsion
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
	
		
	private PTTimeTable2 readTimeTable(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimeTable){
		PTNode ptLastNode = null;
		for (PTLine ptLine :  ptTimeTable.getPtLineList()) {
			//Test code
			/*
			System.out.println(ptLine.getId().toString());
			System.out.println(ptLine.getDirection());
			System.out.println(ptLine.getDepartures().toString());
			System.out.println(ptLine.getMinutes().toString() + "\n");
			*/
			//Create a map with travel times for every standard link
			int indexMin=0;
			double travelTime=0;
			double lastTravelTime=0;
			boolean first=true;
			
			for (String strIdNode : ptLine.getRoute()) {
				PTNode ptNode = ((PTNode)ptNetworkLayer.getNode(strIdNode));
				if (ptNode==null)
					throw new java.lang.NullPointerException("Node does not exist:" + strIdNode); 
	
				ptNode.setIdPTLine(ptLine.getId());
				double min = ptLine.getMinutes().get(indexMin);
				travelTime=min-lastTravelTime;
				if (!first){
					for (Link link : (ptNode.getInLinks().values())) {
						if (link.getFromNode().equals(ptLastNode)){
							linkTravelTimeMap.put(link.getId(), travelTime);
							//-->30 march check if this temporary stuff improves the performance
							link.setCapacity(travelTime);
						}
					}
				}
				ptLastNode= ((PTNode)ptNetworkLayer.getNode(strIdNode));
				lastTravelTime= min;
				first=false;
				indexMin++;
			}//for interator String
		}//for interator ptline
	
		//Calculates the travel time of each link and stores these data in ptTimeTable
		ptTimeTable.calculateTravelTimes(ptNetworkLayer);  //??
		ptTimeTable.setMaps(linkTravelTimeMap);
		return ptTimeTable;
	}
	
	/*
	//-Move to Class PtNode
	public String getNodeBaseId999(String strId){
		String baseID = strId;
		if (baseID.charAt(0)=='_' || baseID.charAt(0)=='~')
			baseID= baseID.substring(1,baseID.length());
		if(Character.isLetter(baseID.charAt(baseID.length()-1))) 	//example of possible node values at intersection:   999, _999, 999b, _999b
			baseID= baseID.substring(0,baseID.length()-1);
		return baseID;
	}
	*/
	private void createTransferLinks(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimeTable) {
		PTStation stationMap = new PTStation(ptTimeTable);
		
		stationMap.print();
		
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
		if (fromNode==null)
			throw new java.lang.NullPointerException("fromNode does not exist in link:" + strIdLink); 

		if (toNode==null)
			throw new java.lang.NullPointerException("toNode does not exist in link:" + strIdLink); 

		Id idLink = new IdImpl(strIdLink);
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		//Values for nFor the time being these values are irrelevant 
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";
		net.createLink(idLink, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type);
	}
	
	public void writeNet(NetworkLayer net, String fileName){
		System.out.println("writing pt network...");
		new NetworkWriter(net, fileName).write();
		System.out.println("done.");
	}
	
	public void CreateDetachedTransfers (NetworkLayer net, double distance, PTTimeTable2 ptTimeTable){
		int x=0;
		String strId;
		
		for (Node node: net.getNodes().values()){
			for (Node nearNode : net.getNearestNodes(node.getCoord(), distance)){
				if(!node.getCoord().equals(nearNode.getCoord())){
					if (!areConected(node, nearNode)){
						PTNode ptn1 = (PTNode) node;
						PTNode ptn2 = (PTNode) nearNode;

						strId= "DT" + ++x;
						createPTLink(net, strId, ptn1, ptn2, "DetTransfer");
						
						strId= "DT" + ++x;
						createPTLink(net, strId, ptn2, ptn1, "DetTransfer");
					}
				}
			}
		} 
	
	}

	//Returns true if a link exists between two nodes
	public boolean areConected(Node n1, Node n2 ){
		//--> find out what kind of links join the nodes
		for (Id id : n1.getOutNodes().keySet()){
			if (n2.getId().equals(id))
				return true;
		}
		return false;
	}
	
	public static void printLinks(NetworkLayer ptNetworkLayer) {
		//Displays a quick visualization of links with from- and to- nodes
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
