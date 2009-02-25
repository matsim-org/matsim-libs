package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.basic.v01.IdImpl;

import playground.mmoyo.PTRouter.*;
import org.matsim.utils.misc.Time;

public class PTTimeTable2{
	private PTLinesReader2 ptLinesReader = new PTLinesReader2();
	private List<PTLine> ptLineList = new ArrayList<PTLine>();
	private Map<Id,Double> linkTravelTimeMap = new TreeMap<Id,Double>();
	private Map<Id,double[]> nodeDeparturesMap = new TreeMap<Id,double[]>();
	private static Time time;
	
	public PTTimeTable2(String TimeTableFile){
		ptLinesReader.readFile(TimeTableFile);
		this.ptLineList = ptLinesReader.ptLineList;
	}

	public void setMaps(Map<Id,Double> linkTravelTimeMap){
		this.linkTravelTimeMap = linkTravelTimeMap;
	}
	
	public void calculateTravelTimes(NetworkLayer networklayer){
		Map<Id,Double> minuteMap = new TreeMap<Id,Double>();
		
		for (Iterator<PTLine> iter = ptLineList.iterator(); iter.hasNext();){
			PTLine ptLine = iter.next();
											
			int x= 0;
			for (Iterator<String> iter2 = ptLine.getRoute().iterator(); iter2.hasNext();){
				//Create a map with nodes-minutes after departure
				Id idNode = new IdImpl(iter2.next().toString());
				double minAfterDep = new Double(ptLine.getMinutes().get(x));
				minuteMap.put(idNode, minAfterDep);

				//Fills the map with arrivals for every node
				//Double[] departuresArray =ptLine.getDepartures().toArray(new Double[ptLine.getDepartures().size()]);
				double[] departuresArray =new double[ptLine.getDepartures().size()];					
				int y=0;
				for (Iterator<String> iter3 = ptLine.getDepartures().iterator(); iter3.hasNext();){
					String h= iter3.next();
					//Seconds past midnight
					departuresArray[y]=  time.parseTime(h) + (minAfterDep*60);
					y++;
				}
				nodeDeparturesMap.put(idNode, departuresArray);
				x++;
			
				/*
				PTNode ptNode = (PTNode)networklayer.getNode(idNode);
				Link[] arrlink= ptNode.getOutLinks().values().toArray(new Link[1]);
				Link link = arrlink[0];
				//System.out.println(link.getFromNode().getId() + "--" + link.getId() + "-->" + link.getToNode().getId());
				*/
				//if (link.getType().equals("Standard")){
				//Id idFromNode = link.getFromNode().getId();
				//Id idToNode = link.getToNode().getId();
				//double travelTime=minuteMap.get(idToNode)- minuteMap.get(idFromNode); 
				//linkTravelTimeMap.put(link.getId(), travelTime);
				//}
			}
		}
		
		/*
		//Creates the map with link-travel time
		for (Link link : networklayer.getLinks().values()) {
			if (link.getType().equals("Standard")){
				Id idFromNode = link.getFromNode().getId();
				Id idToNode = link.getToNode().getId();
				double travelTime=minuteMap.get(idToNode)- minuteMap.get(idFromNode); 
				linkTravelTimeMap.put(link.getId(), travelTime);
			}
		}
		networklayer =null;
		*/
	}
	
	public double GetTravelTime(Link link){
		return linkTravelTimeMap.get(link.getId()); 
	}

	//minutes
	public double GetTransferTime(Link link, double time){
		Id idToNode= link.getToNode().getId();
		double nextDeparture = nextDeparture(idToNode,time); 
		double transferTime= 0;
		if (nextDeparture>=time){
			transferTime= nextDeparture-time;
		}else{
			//count seconds between 01:00 am and firt departure  
			transferTime= (86400-time)+ nodeDeparturesMap.get(idToNode)[0];
		}
		return transferTime;///return transferTime/60;  //Seconds!
	}
	
	public List<PTLine> getPtLineList() {
		return ptLineList;
	}
	
	//in minutes
	public double nextDeparture(Id idPTNode,  double dblTime){//,
		//return dblTime +1;
		
		double[]arrDep= nodeDeparturesMap.get(idPTNode);
		int x=0;
		while (arrDep[x]<dblTime){
			x++;
			if (x==arrDep.length){ //
				return arrDep[0];
			}
		}
		return arrDep[x];
	}
	
	//in minutes
	public double nextDepartureB(Id idPTNode,  double dblTime){//,
		//return dblTime +1;
		double[]arrDep= nodeDeparturesMap.get(idPTNode);
		int index =  Arrays.binarySearch(arrDep, dblTime);  
		if (index<0){
			index = -index;
			if (index <= arrDep.length)index--; else index=-1;	
		}else{
			if (index < (arrDep.length-1))index++; else index=-1;	
		}
		
		if (index== -1) {
			//TODO Define what to do here!!
		}
		return arrDep[index];
		//System.out.println("Next departuere: "+ nextDep);
	}
		
		
}

/*
/*******************************
 * Time Methods


//Converts a string in format hh:mm into integer representing seconds after the midnight
private double strTimeToDbl(String strDeparture){
	return time.parseTime(strDeparture);
}

//Converts integers representing a time into format "hh:mm"
private String TimeToString(double dblDeparture){
	return time.writeTime(dblDeparture, "HH:mm");
}
*/