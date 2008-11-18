package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.Link;
import org.matsim.network.NetworkWriter;
import org.matsim.utils.geometry.Coord;

import playground.mmoyo.PTRouter.*;

public class PTNetworkFactory2 {
	public Map <Id,Double> linkTravelTimeMap = new TreeMap <Id,Double>();
	//private Map<Id,double[]> nodeDeparturesMap = new TreeMap<Id,double[]>();
	
	public PTNetworkFactory2(){
		super();
	}
	
	public NetworkLayer createNetwork(String inFileName, PTTimeTable2 ptTimeTable, String OutFileName){
		NetworkLayer ptNetworkLayer;

		//PTLinesReader2 ptLinesReader = new PTLinesReader2();
		NetworkFactory networkFactory = new NetworkFactory();
		ptNetworkLayer= new NetworkLayer(networkFactory);
		
		//Create a temporal network with normal Nodes
		NetworkLayer tempNet= new NetworkLayer(networkFactory);
		new MatsimNetworkReader(tempNet).readFile(inFileName);
		
		//Create the PTNetwork with PTNodes
		List<PTNode> ptNodeList = new ArrayList<PTNode>();
		for (Node node: tempNet.getNodes().values()){
			ptNodeList.add(new PTNode(new IdImpl(node.getId().toString()),node.getCoord(),node.getType()));
		}
	
		//add ptNodes
		for (Iterator<PTNode> iter = ptNodeList.iterator(); iter.hasNext();) {
			PTNode ptNode= iter.next();	
			ptNetworkLayer.getNodes().put(ptNode.getId(),ptNode);
		}

		//Add Links
		for (Link l: tempNet.getLinks().values()){
			createPTLink(ptNetworkLayer, l.getId().toString(), l.getFromNode().getId().toString(), l.getToNode().getId().toString(), "Standard");
		}
		tempNet= null;
		
		//Read all nodes
		Map<String, ArrayList<String>> IntersectionMap = new TreeMap<String, ArrayList<String>>();
		PTNode ptLastNode= null;
		for (Iterator<PTLine> iterPTLines = ptTimeTable.getPtLineList().iterator(); iterPTLines.hasNext();) {
			PTLine ptLine = iterPTLines.next();
			//Test code
			//System.out.println(ptLine.getId().toString());
			//System.out.println(ptLine.getDirection());
			//System.out.println(ptLine.getDepartures().toString());
			//System.out.println(ptLine.getMinutes().toString() + "\n");
			
			//Create a map with travel times for every link
			int indexMin=0;
			double travelTime=0;
			double lastTravelTime=0;
			boolean check=false;
			for (Iterator<String> iter = ptLine.getRoute().iterator(); iter.hasNext();) {
				String strIdNode = iter.next();
				PTNode ptNode = ((PTNode)ptNetworkLayer.getNode(strIdNode));
				ptNode.setIdPTLine(ptLine.getId());

				double min = ptLine.getMinutes().get(indexMin);
				travelTime=min-lastTravelTime;
				if (check){
					for (Link link : (ptNode.getInLinks().values())) {
						if (link.getFromNode().equals(ptLastNode)){
							linkTravelTimeMap.put(link.getId(), travelTime);
						}
					}
				}
				ptLastNode= ((PTNode)ptNetworkLayer.getNode(strIdNode));
				lastTravelTime= min;
				check=true;
				indexMin++;
				///////////////////////////////////////////////////////////////////////
				//example of possible node values at intersection:   999, _999, 999b, _999b
				if(Character.isLetter(strIdNode.charAt(strIdNode.length()-1))){
					String keyNode = strIdNode;
					if (keyNode.charAt(0)=='_'){
						keyNode = keyNode.substring(1, keyNode.length()-1);
					}
					if(Character.isLetter(keyNode.charAt(keyNode.length()-1))){
						keyNode= keyNode.substring(0,keyNode.length()-1);
					}
	    			if (!IntersectionMap.containsKey(keyNode)){
	    				ArrayList<String> ch = new ArrayList<String>();
	    				IntersectionMap.put(keyNode, ch);
	    				IntersectionMap.get(keyNode).add(keyNode);
	    				IntersectionMap.get(keyNode).add("_" + keyNode);
	    			}// if IntersectionMap
	    			IntersectionMap.get(keyNode).add(strIdNode);
				}//if Character

				/*
				//correct code at 12:11    10/Oct/200
				if(Character.isLetter(strIdNode.charAt(strIdNode.length()-1))){
					String keyNode= strIdNode.substring(0,strIdNode.length()-1);
	    			if (!IntersectionMap.containsKey(keyNode)){
	    				ArrayList<String> ch = new ArrayList<String>();
	    				IntersectionMap.put(keyNode, ch);
	    				IntersectionMap.get(keyNode).add(keyNode);
	    			}// if IntersectionMap
	    			IntersectionMap.get(keyNode).add(strIdNode);
				}//if Character
				*/
			
			}//for interator String
		}//for interator ptline

		/*****************************************************************************
		*Calculates the travel time of each link and stores these data in ptTimeTable
		******************************************************************************/
		ptTimeTable.calculateTravelTimes(ptNetworkLayer);  //??
		ptTimeTable.setMaps(linkTravelTimeMap);
		
		/*********************************
		 *Create Transfer Links
		 *********************************/
		int maxLinkKey=0;
		Iterator it = IntersectionMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			List chList1 = (ArrayList) pairs.getValue();
			List chList2 = (ArrayList) pairs.getValue();

			if (chList1.size() > 1) {
				for (Iterator<String> iter1 = chList1.iterator(); iter1.hasNext();) {
					String idNode1 = iter1.next();
					// Create links between children nodes lines
					for (Iterator<String> iter2 = chList2.iterator(); iter2.hasNext();) {
						String idNode2 = iter2.next();
						boolean createTransferLink=true;
						if ((idNode1.charAt(0)=='_' && idNode1.substring(1, idNode1.length()).equals(idNode2))) {
							createTransferLink=false;
						}
						if ((idNode2.charAt(0)=='_' && idNode2.substring(1, idNode2.length()).equals(idNode1))) {
							createTransferLink=false;
						}
						if (idNode1.equals(idNode2)){
							createTransferLink=false;
						}
						if (createTransferLink) {
							maxLinkKey++;
							createPTLink(ptNetworkLayer, "T"+ String.valueOf(maxLinkKey), idNode1, idNode2,"Transfer");
						}
					}//for iter2
				}// for iter1
			}// if chlist
		}// while
		it = null;
		/*******************/
	
		
		//write the conplete PTNetwork (with transfers) into the definitive PT Network File
		System.out.println("writing pt network...");
		new NetworkWriter(ptNetworkLayer, OutFileName).write();
		System.out.println("done.");
		
		
		
		return ptNetworkLayer;
	}//Create Ptnetwork
	
	
	
	
	public PTNode CreateWalkingNode(NetworkLayer ptNetworkLayer,IdImpl idNode, Coord coord) {
		PTNode ptNode = new PTNode(idNode, coord, "Walking");
		ptNode.setIdPTLine(new IdImpl("Walk"));
		ptNetworkLayer.getNodes().put(idNode, ptNode);
		return (PTNode)ptNetworkLayer.getNode(idNode);
	}
	
	public List <IdImpl> CreateWalkingLinks(NetworkLayer ptNetworkLayer, PTNode ptNode, PTNode[]nearNodes, boolean to){
		List<IdImpl> NewWalkLinks = new ArrayList<IdImpl>();
		String idLink;
		PTNode fromPTNode;
		PTNode toPTNode;
		
		for (int x= 0; x<nearNodes.length;x++){
			if (to==true){
				fromPTNode= ptNode;
				toPTNode= nearNodes[x];
				idLink= "W" + String.valueOf(x);
			}else{
				fromPTNode=nearNodes[x];
				toPTNode=  ptNode;
				idLink= "WW" + String.valueOf(x);
			}
			createWalkingLink(ptNetworkLayer, idLink, fromPTNode, toPTNode, "Walking");
			NewWalkLinks.add(new IdImpl(idLink));
		}//for
		return NewWalkLinks;
	}
	
	public void removeWalkinkLinks(NetworkLayer ptNetworkLayer,List<IdImpl> WalkingLinkList){
		//Removes temporal links at the end of the ruting process
		for (Iterator<IdImpl> iter = WalkingLinkList.iterator(); iter.hasNext();) {
			ptNetworkLayer.removeLink(ptNetworkLayer.getLink(iter.next()));
		}
	}
	
	private void createPTLink(NetworkLayer ptNetworkLayer, String idLink, String from, String to, String ptType ){
		String length = "1";
		String freespeed= "1";
		String capacity = "1";
		String permlanes = "1";
		String origid = "0";
		ptNetworkLayer.createLink(idLink, from, to, length, freespeed, capacity, permlanes, origid, ptType);
	}
	
	public void createWalkingLink(NetworkLayer ptNetworkLayer, String idLink, PTNode fromPTNode , PTNode toPTNode, String ptType ){
		String idFromNode = fromPTNode.getId().toString();
		String idToNode = toPTNode.getId().toString();
		String length = Double.toString(fromPTNode.getCoord().calcDistance(toPTNode.getCoord()));

		//For the time being these values are irrelevant in PTsimulation
		String freespeed= "1";
		String capacity = "1";
		String permlanes = "1";
		String origid = "0";
		ptNetworkLayer.createLink(idLink, idFromNode, idToNode, length, freespeed, capacity, permlanes, origid, ptType);
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
