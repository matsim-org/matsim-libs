package playground.julia.distribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class ResponsibilityUtils {

	public void addExposureAndResponsibilityLinkwise(
			ArrayList<EmCarTrip> carTrips,
			Map<Double, ArrayList<EmPerLink>> emissionPerLink,
			ArrayList<ExposureEvent> exposure,
			ArrayList<ResponsibilityEvent> responsibility, double timeBinSize, double simulationEndTime) {
		
		  		for(EmCarTrip ect: carTrips){
				Id linkId = ect.getLinkId();
				Double startTime = ect.getStartTime();
				Double endTime = ect.getEndTime();
				Id exposedPersonId = ect.getPersonId();
				
				// TODO Benjamin fragen: Annahme: fahrzeit auf links ist so kurz, dass sie nicht in mehrere time bins fallen
				Double endOfTimeInterval = Math.ceil(ect.getStartTime()/ timeBinSize)* timeBinSize;
				
				// all responsibility events for this activity
				ArrayList<ResponsibilityEvent> currentREvents = new ArrayList<ResponsibilityEvent>();
				currentREvents.addAll(generateResponsibilityEventsForLink(emissionPerLink.get(endOfTimeInterval), linkId, startTime, endTime));
				
				
				// calculate exposure
				Double exposureValue =0.0;
				
				for(ResponsibilityEvent re: currentREvents){
					exposureValue+=re.getExposureValue();
				}
				
				String actType = "car on link " + ect.getLinkId().toString();
				ExposureEvent exposureEvent = new ExposureEvent(exposedPersonId, startTime, endTime, exposureValue, actType);
				exposure.add(exposureEvent);
				
				responsibility.addAll(currentREvents);
			}
		
	}

	private ArrayList<ResponsibilityEvent> generateResponsibilityEventsForLink(
			ArrayList<EmPerLink> emissionPerLinkOfCurrentTimeBin, Id linkId, Double startTime,
			Double endTime) {
		
		ArrayList<ResponsibilityEvent> rEvents = new ArrayList<ResponsibilityEvent>();
		
		for(EmPerLink epl: emissionPerLinkOfCurrentTimeBin){
			if(epl.getLinkId().equals(linkId)){
				String location = "link " + linkId.toString();
				ResponsibilityEvent ree = new ResponsibilityEvent(epl.getPersonId(), startTime, endTime, epl.getConcentration(), location);
				rEvents.add(ree);
			}
		}
		return rEvents;
	}

	public void addExposureAndResponsibilityBinwise(
			ArrayList<EmActivity> activities,
			Map<Double, ArrayList<EmPerBin>> emissionPerBin,
			ArrayList<ExposureEvent> exposure,
			ArrayList<ResponsibilityEvent> responsibility, Double timeBinSize, Double simulationEndTime) {
		
		
		for(EmActivity ema: activities){
			
			// activity location
			int xBin = ema.getXBin();
			int yBin = ema.getYBin();
			
			// all responsibility events for this activity
			ArrayList<ResponsibilityEvent> currentREvents = new ArrayList<ResponsibilityEvent>();
			
			// split activity according to time bins 
			Double startTime = ema.getStartTime();
			Double endTime = ema.getEndTime();
			
			// number of time bins matching activity time
			int firstTimeBin = (int) Math.ceil(startTime/timeBinSize);
			if(firstTimeBin==0)firstTimeBin=1;
			int lastTimeBin;
			if(ema.getEndTime()>0.0){
					lastTimeBin = (int) Math.ceil(endTime/timeBinSize);
			}else{
					lastTimeBin = (int) Math.ceil(simulationEndTime/timeBinSize);
			}
			
			// calculate responsibility events
			// case distinction - number of time bins
			if (firstTimeBin<lastTimeBin){
				//first bin
				Double firstStartTime = startTime;
				Double firstEndTime = firstTimeBin * timeBinSize;
				currentREvents.addAll(generateResponsibilityEventsForCell(emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, firstStartTime, firstEndTime));
				// inner time bins
				for (int i = firstTimeBin + 1; i < lastTimeBin; i++) {
					Double currentStartTime = (i-1)*timeBinSize; // TODO check!
					Double currentEndTime = i*timeBinSize;
					currentREvents.addAll(generateResponsibilityEventsForCell(emissionPerBin.get(i*timeBinSize), i, xBin, yBin, currentStartTime, currentEndTime));
				}
				// last bin
				Double lastStartTime = (lastTimeBin-1)*timeBinSize;
				Double lastEndTime = endTime;
				currentREvents.addAll(generateResponsibilityEventsForCell(emissionPerBin.get(lastTimeBin*timeBinSize), lastTimeBin, xBin, yBin, lastStartTime, lastEndTime));
				
			}else{ // activity entirely in one interval
				currentREvents.addAll(generateResponsibilityEventsForCell(emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, startTime, endTime));
			}
			
			// calculate exposure
			
			Double exposureValue =0.0;
			
			for(ResponsibilityEvent re: currentREvents){
				exposureValue+=re.getExposureValue();
			}
			
			ExposureEvent exposureEvent = new ExposureEvent(ema.getPersonId(), startTime, endTime, exposureValue, ema.getActivityType());
			exposure.add(exposureEvent);
			
			responsibility.addAll(currentREvents);
			
			
		}
		
	}

	private ArrayList<ResponsibilityEvent> generateResponsibilityEventsForCell(
			ArrayList<EmPerBin> emissionPerBinOfCurrentTimeBin, int firstTimeBin,
			int xBin, int yBin, Double startTime, Double endTime) {
		
		ArrayList<ResponsibilityEvent> rEvents= new ArrayList<ResponsibilityEvent>();
		
		for(EmPerBin epb: emissionPerBinOfCurrentTimeBin){
			if(epb.getXbin().equals(xBin) && epb.getYbin().equals(yBin)){
				String location = "x = " + epb.getXbin().toString() + ", y = " + epb.getYbin();
				ResponsibilityEvent ree = new ResponsibilityEvent(epb.getPersonId(), startTime, endTime, epb.getConcentration(), location);
				rEvents.add(ree);
			}
		}
		return rEvents;
	}


	


}
