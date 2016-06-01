package playground.benjamin.scenarios.munich.exposure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	SortedMap<Double, Double[][]> duration = new TreeMap<Double, Double[][]>();
	Map<Id<Link>,Integer> link2xbins;
	Map<Id<Link>,Integer> link2ybins;
	private Double timeBinSize;
	private Double simulationEndTime;
	private int noOfxCells;
	private int noOfyCells;
	private Set<Id<Person>> recognisedPersons;


	public IntervalHandler(Double timeBinSize, Double simulationEndTime, int noOfxCells, int noOfyCells, 
			Map<Id<Link>,Integer> link2xbins, Map<Id<Link>,Integer> link2ybins){
		this.timeBinSize=timeBinSize;
		this.simulationEndTime = simulationEndTime;
		this.noOfxCells = noOfxCells;
		this.noOfyCells = noOfyCells;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		recognisedPersons = new HashSet<Id<Person>>();
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		recognisedPersons = new HashSet<Id<Person>>();
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

		Id<Link> linkId = event.getLinkId();
		if(link2xbins.get(linkId)!=null && link2ybins.get(linkId)!=null){
			int xCell = link2xbins.get(linkId); 
			int	yCell = link2ybins.get(linkId);

			Double currentTimeBin = Math.ceil(event.getTime()/timeBinSize)*timeBinSize;

			if(currentTimeBin<timeBinSize) currentTimeBin=timeBinSize;

			Double timeWithinCurrentInterval = new Double(event.getTime()-currentTimeBin+timeBinSize);
			if(recognisedPersons.contains(event.getPersonId())){	
				// time interval of activity
				double prevDuration = duration.get(currentTimeBin)[xCell][yCell];
				
				double updatedDuration = prevDuration - timeBinSize + timeWithinCurrentInterval;
				duration.get(currentTimeBin)[xCell][yCell] = updatedDuration;
				
//				if (prevDuration>timeBinSize) { // this looks completely illogical because prevDuration is sum of actDurations in that time bin for all persons amit Oct'15
//					prevDuration = prevDuration - timeWithinCurrentInterval;
//					duration.get(currentTimeBin)[xCell][yCell] = prevDuration;
//				}
				currentTimeBin += timeBinSize;

				// later time intervals
				while(currentTimeBin <= simulationEndTime){
					
					double prevDurationL = duration.get(currentTimeBin)[xCell][yCell];
					duration.get(currentTimeBin)[xCell][yCell] = prevDurationL - timeBinSize;
					
//					if (prevDurationL>timeBinSize) {
//						prevDurationL = prevDurationL - timeBinSize;
//						duration.get(currentTimeBin)[xCell][yCell] = prevDurationL;
//					}
					
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
				duration.get(currentTimeBin)[xCell][yCell] += timeWithinCurrentInterval;
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		Id<Link> linkId = event.getLinkId();

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

	public SortedMap<Double, Double[][]> getDuration() {
		return duration;
	}

}
