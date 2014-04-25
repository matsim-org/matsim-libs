//package playground.julia.distribution.withScoringFast;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.events.ActivityEndEvent;
//import org.matsim.api.core.v01.events.ActivityStartEvent;
//import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
//import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
//
//import playground.julia.distribution.EmActivity;
//
//public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{
//
//	HashMap<Id, ArrayList<ActivityStartEvent>> person2asevent = new HashMap<Id, ArrayList<ActivityStartEvent>>();
//	HashMap<Id, ArrayList<ActivityEndEvent>> person2aeevent = new HashMap<Id, ArrayList<ActivityEndEvent>>();
//	HashMap<Double, Double[][]> duration = new HashMap<Double, Double[][]>(); // time interval, xbin, ybin: # of persons x time spent there
//	
//	public Map<Double, Double[][]> getDuration(){
//		return duration;
//	}
//
//	@Override
//	public void reset(int iteration) {
//		this.person2asevent.clear();// = new HashMap<Id, ArrayList<ActivityStartEvent>>();
//		this.person2aeevent.clear(); // = new HashMap<Id, ArrayList<ActivityEndEvent>>();
//		duration = new HashMap<Double, Double[][]>();
//	}
//
//	@Override
//	public void handleEvent(ActivityEndEvent event) {
//		Id personId = event.getPersonId();
//		ArrayList<ActivityEndEvent> events;
//		if(person2aeevent.containsKey(personId)){
//			events = person2aeevent.get(personId);
//		}else{
//			events = new ArrayList<ActivityEndEvent>();
//		}
//		events.add(event);
//		person2aeevent.put(personId, events);		
//	}
//
//	@Override
//	public void handleEvent(ActivityStartEvent event) {
//		Id personId = event.getPersonId();
//		ArrayList<ActivityStartEvent> events;
//		if(person2asevent.containsKey(personId)){
//			events = person2asevent.get(personId);
//		}else{
//			events = new ArrayList<ActivityStartEvent>();
//		}
//		events.add(event);
//		person2asevent.put(personId, events);
//	}
//
//	public void calculateDuration(Map<Id,Integer> link2xbins, Map<Id,Integer> link2ybins, Double simulationEndTime, Double timeBinSize, int noOfXbins, int noOfYBins){
//		// combine act start events with act leave events to em activities
//		// without emission values - store person id, time and x,y-cell
//		
//		Logger logger = Logger.getLogger(IntervalHandler.class);
//		logger.info("starting duration calculation");
//		
//		for(Id personId: person2asevent.keySet()){
//			//TODO? sort by time?
//			//TODO gibt es act end events ohne enntsprechendes start event?
//			
//			for(ActivityStartEvent ase: person2asevent.get(personId)){
//				Double startOfActivity = ase.getTime();				
//				
//				if (link2xbins.get(ase.getLinkId())!=null && link2ybins.get(ase.getLinkId())!=null) {
//					int xBin = link2xbins.get(ase.getLinkId());
//					int yBin = link2ybins.get(ase.getLinkId());
//					// find corresponding act end event
//					ActivityEndEvent aee = findCorrespondingActivityEndEvent(
//							ase, person2aeevent.get(personId));
//					Double endOfActivity;
//					if (aee == null) {
//						endOfActivity = simulationEndTime;
//					} else {
//						endOfActivity = aee.getTime();
//					}
//					// add duration
//					// go through timebins
//					Double currentTimeBin = Math.ceil(startOfActivity/timeBinSize)*timeBinSize;
//					if(currentTimeBin<=0.0)currentTimeBin=timeBinSize;
//					
//					while(currentTimeBin<=simulationEndTime){
//						// is this interval relevant?
//						if (startOfActivity<= currentTimeBin && endOfActivity >= currentTimeBin-timeBinSize) {
//							if (!duration.containsKey(currentTimeBin)) {
//								duration.put(currentTimeBin,new Double[noOfXbins+1][noOfYBins+1]);
//							}
//							Double previousDuration = duration.get(currentTimeBin)[xBin][yBin];
//							if (previousDuration == null){
//								duration.get(currentTimeBin)[xBin][yBin] = 0.0;
//								previousDuration = duration.get(currentTimeBin)[xBin][yBin];
//							}
//							
//							Double durationOfActivityInsideTimeBin = Math.min(endOfActivity, currentTimeBin);
//							durationOfActivityInsideTimeBin -= Math.max(startOfActivity, currentTimeBin	- timeBinSize);
//							//previousDuration += durationOfActivityInsideTimeBin;
//							duration.get(currentTimeBin)[xBin][yBin] += durationOfActivityInsideTimeBin;
//						}
//						currentTimeBin+=timeBinSize;
//						
//					}
//					
//				}
//				
//			}
//			// TODO do i miss the first activity?
//			
//		}	
//	}
//
//	private ActivityEndEvent findCorrespondingActivityEndEvent(
//			ActivityStartEvent ase, ArrayList<ActivityEndEvent> arrayList) {
//		if (arrayList !=null) {
//			Double startTime = ase.getTime();
//			Double currTime = Double.MAX_VALUE;
//			ActivityEndEvent currEvent = null;
//			for (ActivityEndEvent event : arrayList) {
//				if (event.getTime() < currTime && event.getTime() > startTime) {
//					currTime = event.getTime();
//					currEvent = event;
//				}
//			}
//			return currEvent;
//		}
//		return null;
//	}
//
//
//	
//
//}
