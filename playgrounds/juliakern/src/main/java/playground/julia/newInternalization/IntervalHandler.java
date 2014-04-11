package playground.julia.newInternalization;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;


// TODO + homelocation at start of day
// TODO den grid tools alte durations uebergeben

public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	HashMap<Double, Double[][]> duration = new HashMap<Double, Double[][]>();
	Map<Id,Integer> link2xbins;
	Map<Id,Integer> link2ybins;
	private Double timeBinSize;
	private Double simulationEndTime;
	private int noOfxCells;
	private int noOfyCells;

	
	public IntervalHandler(Double timeBinSize, Double simulationEndTime, int noOfxCells, int noOfyCells, 
			Map<Id,Integer> link2xbins, Map<Id,Integer> link2ybins){
		this.timeBinSize=timeBinSize;
		this.simulationEndTime = simulationEndTime;
		this.noOfxCells = noOfxCells;
		this.noOfyCells = noOfyCells;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		if(link2xbins.isEmpty())System.out.println("---- emptyx");
		if(link2ybins.isEmpty())System.out.println("---- emptyy");
	}
	
	@Override
	public void reset(int iteration) {
		for(int i=0; i<simulationEndTime/timeBinSize+1; i++){
			duration.put(i*timeBinSize, new Double[noOfxCells][noOfyCells]);
			for(int j=0; j< noOfxCells; j++){
				for(int k=0; k< noOfyCells; k++){
					duration.get(i*timeBinSize)[j][k]=0.0;
				}
			}
		}
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {
			
		int xCell = link2xbins.get(event.getLinkId());
		int	yCell = link2ybins.get(event.getLinkId());

		Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
		if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
		Double timeWithinCurrentInterval = event.getTime()-currentTimeBin+timeBinSize;
			
		// time interval of activity
		double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
			
		if (prevDuration>timeBinSize) {
			prevDuration = prevDuration - timeWithinCurrentInterval;
			duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
		}
		currentTimeBin += timeBinSize;
			
		// later time intervals
		while(currentTimeBin <= simulationEndTime){
			double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
			if (prevDurationL>timeBinSize) {
				prevDurationL = prevDurationL - timeBinSize;
				duration.get(currentTimeBin)[xCell][yCell] = prevDurationL;
			}
			currentTimeBin += timeBinSize;
		}
		
//			System.out.println(network.getLinks().size());
//			System.out.println(event.getLinkId());
//			System.out.println(network.getLinks().get(event.getLinkId()));
//			System.out.println(network.getLinks().get(event.getLinkId()).getCoord());
//			System.out.println(network.getLinks().get(event.getLinkId()).getCoord().getX());
//			xCell = gt.mapXCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getX(), noOfxCells);
//			yCell = gt.mapYCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getY(), noOfyCells);
		

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
	
		int	xCell = link2xbins.get(event.getLinkId());
		int	yCell = link2ybins.get(event.getLinkId());
		
		Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
		if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
		Double timeWithinCurrentInterval = -event.getTime()+currentTimeBin;
			
		// time interval of activity
		double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
		prevDuration = prevDuration + timeWithinCurrentInterval;
		duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
		currentTimeBin += timeBinSize;
			
		// later time intervals
		while(currentTimeBin <= simulationEndTime){
			double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
			prevDurationL = prevDurationL + timeBinSize;
			duration.get(currentTimeBin)[xCell][yCell]=prevDurationL;
			currentTimeBin += timeBinSize;
		}
	}

	public HashMap<Double, Double[][]> getDuration() {
		return duration;
	}

}
