package playground.juliakern.responsibilityOffline;
//package playground.julia.responsibilityOffline;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.apache.xerces.dom3.as.ASElementDeclaration;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.events.ActivityEndEvent;
//import org.matsim.api.core.v01.events.ActivityStartEvent;
//import org.matsim.api.core.v01.events.LinkEnterEvent;
//import org.matsim.api.core.v01.events.LinkLeaveEvent;
//import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
//import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
//import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
//import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
//import org.matsim.api.core.v01.network.Network;
//
//import playground.julia.distribution.EmActivity;
//import playground.julia.distribution.GridTools;
//
//
//public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{
//
//	HashMap<Id, ArrayList<LinkLeaveEvent>> person2llevent = new HashMap<Id, ArrayList<LinkLeaveEvent>>();
//	HashMap<Id, ArrayList<LinkEnterEvent>> person2leevent = new HashMap<Id, ArrayList<LinkEnterEvent>>();
//	HashMap<Id, ArrayList<ActivityStartEvent>> person2asevent = new HashMap<Id, ArrayList<ActivityStartEvent>>();
//	HashMap<Id, ArrayList<ActivityEndEvent>> person2aeevent = new HashMap<Id, ArrayList<ActivityEndEvent>>();
//	HashMap<Double, Double[][]> duration = new HashMap<Double, Double[][]>();
//	Map<Id,Integer> link2xbins;
//	Map<Id,Integer> link2ybins;
//	private Double timeBinSize;
//	private Double simulationEndTime;
//	private int noOfxCells;
//	private int noOfyCells;
//	int uncountedAEE =0;
//	int uncountedASE=0;
//	private GridTools gt;
//	private Network network;
//	
//	public IntervalHandler(Double timeBinSize, Double simulationEndTime, int noOfxCells, int noOfyCells, 
//			Map<Id,Integer> link2xbins, Map<Id,Integer> link2ybins, GridTools gt, Network network){
//		this.timeBinSize=timeBinSize;
//		this.simulationEndTime = simulationEndTime;
//		this.noOfxCells = noOfxCells;
//		this.noOfyCells = noOfyCells;
//		this.link2xbins = link2xbins;
//		this.link2ybins = link2ybins;
//		if(link2xbins.isEmpty())System.out.println("---- emptyx");
//		if(link2ybins.isEmpty())System.out.println("---- emptyy");
//		this.gt = gt;
//		this.network = network;
//	}
//	
//	@Override
//	public void reset(int iteration) {
//		for(int i=0; i<simulationEndTime/timeBinSize+1; i++){
//			duration.put(i*timeBinSize, new Double[noOfxCells][noOfyCells]);
//			for(int j=0; j< noOfxCells; j++){
//				for(int k=0; k< noOfyCells; k++){
//					duration.get(i*timeBinSize)[j][k]=0.0;
//				}
//			}
//		}
//	}
//
//
//	@Override
//	public void handleEvent(ActivityEndEvent event) {
//			
//		int xCell, yCell;
//		try {
//			xCell = link2xbins.get(event.getLinkId());
//			yCell = link2ybins.get(event.getLinkId());
//
//			Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
//			if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
//			Double timeWithinCurrentInterval = event.getTime()-currentTimeBin+timeBinSize;
//			
//			// time interval of activity
//			double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
//			
//			if (prevDuration>timeBinSize) {
//				prevDuration = prevDuration - timeWithinCurrentInterval;
//				duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
//			}
//			currentTimeBin += timeBinSize;
//			
//			// later time intervals
//			while(currentTimeBin <= simulationEndTime){
//				double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
//				if (prevDurationL>timeBinSize) {
//					prevDurationL = prevDurationL - timeBinSize;
//					duration.get(currentTimeBin)[xCell][yCell] = prevDurationL;
//				}
//				currentTimeBin += timeBinSize;
//			}
//		} catch (NullPointerException e) {
//			uncountedAEE ++;
////			System.out.println(network.getLinks().size());
////			System.out.println(event.getLinkId());
////			System.out.println(network.getLinks().get(event.getLinkId()));
////			System.out.println(network.getLinks().get(event.getLinkId()).getCoord());
////			System.out.println(network.getLinks().get(event.getLinkId()).getCoord().getX());
////			xCell = gt.mapXCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getX(), noOfxCells);
////			yCell = gt.mapYCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getY(), noOfyCells);
//		}
//
//	}
//
//	@Override
//	public void handleEvent(ActivityStartEvent event) {
//		
//		int xCell, yCell;
//		try {
//			xCell = link2xbins.get(event.getLinkId());
//			yCell = link2ybins.get(event.getLinkId());
//		
//			Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
//			if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
//			Double timeWithinCurrentInterval = -event.getTime()+currentTimeBin;
//			
//			// time interval of activity
//			double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
//			prevDuration = prevDuration + timeWithinCurrentInterval;
//			duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
//			currentTimeBin += timeBinSize;
//			
//			// later time intervals
//			while(currentTimeBin <= simulationEndTime){
//				double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
//				prevDurationL = prevDurationL + timeBinSize;
//				duration.get(currentTimeBin)[xCell][yCell]=prevDurationL;
//				currentTimeBin += timeBinSize;
//			}
//			} catch (NullPointerException e) {
//				uncountedASE ++;
//
//			}
//		
//	}
//
//	public HashMap<Double, Double[][]> getDuration() {
//		return duration;
//	}
//	
//	public String uncountedEvents(){
//		return("activity end events " + uncountedAEE + " uncounted start events " + uncountedASE);
//	}
//
//}
