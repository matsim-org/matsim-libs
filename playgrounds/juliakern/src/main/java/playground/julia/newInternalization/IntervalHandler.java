package playground.julia.newInternalization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

// TODO den grid tools alte durations uebergeben - ueberpruefen

public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	HashMap<Double, Double[][]> duration = new HashMap<Double, Double[][]>();
	Map<Id,Integer> link2xbins;
	Map<Id,Integer> link2ybins;
	private Double timeBinSize;
	private Double simulationEndTime;
	private int noOfxCells;
	private int noOfyCells;
	private Set<Id> recognisedPersons;

	
	public IntervalHandler(Double timeBinSize, Double simulationEndTime, int noOfxCells, int noOfyCells, 
			Map<Id,Integer> link2xbins, Map<Id,Integer> link2ybins){
		this.timeBinSize=timeBinSize;
		this.simulationEndTime = simulationEndTime;
		this.noOfxCells = noOfxCells;
		this.noOfyCells = noOfyCells;
		this.link2xbins = link2xbins; //TODO probably not initialized
		this.link2ybins = link2ybins;
		if(link2xbins.isEmpty())System.out.println("---- emptyx");
		if(link2ybins.isEmpty())System.out.println("---- emptyy");
		recognisedPersons = new HashSet<Id>();
	}
	
	@Override
	public void reset(int iteration) {
		recognisedPersons = new HashSet<Id>();
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
			
		Id linkId = event.getLinkId();
		if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
		int xCell = link2xbins.get(linkId); 
		int	yCell = link2ybins.get(linkId);

		Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;
		
		
		if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;
		
		
		Double timeWithinCurrentInterval = new Double(event.getTime()-currentTimeBin+timeBinSize);
		if(recognisedPersons.contains(event.getPersonId())){	
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
		}else{ // person not yet recognised
			recognisedPersons.add(event.getPersonId());
			Double tb = new Double(timeBinSize);
			// time bins prior to events time bin
			while(tb < currentTimeBin){
				duration.get(tb)[xCell][yCell] += timeBinSize;
				tb += timeBinSize;
			}
			// time bin of event
			duration.get(currentTimeBin)[xCell][yCell]+=timeWithinCurrentInterval;
		}
//			System.out.println(network.getLinks().size());
//			System.out.println(event.getLinkId());
//			System.out.println(network.getLinks().get(event.getLinkId()));
//			System.out.println(network.getLinks().get(event.getLinkId()).getCoord());
//			System.out.println(network.getLinks().get(event.getLinkId()).getCoord().getX());
//			xCell = gt.mapXCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getX(), noOfxCells);
//			yCell = gt.mapYCoordToBin(network.getLinks().get(event.getLinkId()).getCoord().getY(), noOfyCells);
		

	}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		Id linkId = event.getLinkId();
		
		if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
		if(!recognisedPersons.contains(event.getPersonId()))recognisedPersons.add(event.getPersonId());
		
		int	xCell = link2xbins.get(linkId);
		int	yCell = link2ybins.get(linkId);
		
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
	}

	public HashMap<Double, Double[][]> getDuration() {
		return duration;
	}

}
