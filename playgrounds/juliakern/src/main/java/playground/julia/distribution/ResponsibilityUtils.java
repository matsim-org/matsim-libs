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
		// TODO Auto-generated method stub
		
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
				currentREvents.addAll(generateResponsibilityEvents(emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, firstStartTime, firstEndTime));
				// inner time bins
				for (int i = firstTimeBin + 1; i < lastTimeBin; i++) {
					Double currentStartTime = (i-1)*timeBinSize; // TODO check!
					Double currentEndTime = i*timeBinSize;
					currentREvents.addAll(generateResponsibilityEvents(emissionPerBin.get(i*timeBinSize), i, xBin, yBin, currentStartTime, currentEndTime));
				}
				// last bin
				Double lastStartTime = (lastTimeBin-1)*timeBinSize;
				Double lastEndTime = endTime;
				currentREvents.addAll(generateResponsibilityEvents(emissionPerBin.get(lastTimeBin*timeBinSize), lastTimeBin, xBin, yBin, lastStartTime, lastEndTime));
				
			}else{ // activity entirely in one interval
				currentREvents.addAll(generateResponsibilityEvents(emissionPerBin.get(firstTimeBin*timeBinSize), firstTimeBin, xBin, yBin, startTime, endTime));
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

	private ArrayList<ResponsibilityEvent> generateResponsibilityEvents(
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

	/*
	 * 		for(EmCarTrip ect: carTrips){
			Id linkId = ect.getLinkId();
			Double duration = ect.getDuration();
			
			// exposure
			// TODO Benjamin fragen: Annahme: fahrzeit auf links ist so kurz, dass sie nicht in mehrere time bins fallen
			Double endOfTimeInterval = Math.ceil(ect.getStartTime()/ timeBinSize)* timeBinSize;
			Id exposedPersonId = ect.getPersonId();
			Double personalExposure = reut.getExposureOnLink(linkId, endOfTimeInterval)* duration * pollutionFactorOutdoor;
			
			ExposureEvent eevent = new ExposureEvent(exposedPersonId, ect.getStartTime(), ect.getEndTime(), personalExposure, "car");
			exposure.add(eevent);
			
			// responsibility
			ArrayList<ResponsibilityEvent> revents = reut.getResponsibilityOnLink(linkId, endOfTimeInterval, duration);
			responsibility.addAll(revents);
		}
	 */
	
	
//	public Double getExposureOnLink(Id linkId, Double endOfTimeInterval) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public ArrayList<ResponsibilityEvent> getResponsibilityOnLink(Id linkId,
//			Double endOfTimeInterval, Double duration) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public Double getExposureInCell(int xBin, int yBin, int firstTimeBin) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
