package playground.wrashid.parkingChoice.scoring;

import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.list.Lists;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingChoice.events.ParkingArrivalEvent;
import playground.wrashid.parkingChoice.events.ParkingDepartureEvent;
import playground.wrashid.parkingChoice.handler.ParkingArrivalEventHandler;
import playground.wrashid.parkingChoice.handler.ParkingDepartureEventHandler;

//TODO: I could just collect the walking distances/time which needs to be deduced from score here.
//TODO: => make use of this wisly, as this is invoked a lot of times...
//TODO: in a similar handler I could log all the data during the simulation

public class ParkingScoreCollector implements ParkingArrivalEventHandler,ParkingDepartureEventHandler {

	private HashMap<Id, Double> firstParkingDepartureTime;
	
	private HashMap<Id, Double> currentArrivalTime;
	
	private DoubleValueHashMap<Id> sumOfParkingDurations;
	
	private boolean finishHandlingCalled;
	
	private LinkedListValueHashMap<Id,Double> walkingTimesAtParkingArrival;
	private LinkedListValueHashMap<Id,Double> walkingTimesAtParkingDeparture;

	private final Controler controler;
	
	public ParkingScoreCollector(Controler controler) {
		this.controler = controler;
		reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		firstParkingDepartureTime=new HashMap<Id, Double>();
		currentArrivalTime=new HashMap<Id, Double>();
		walkingTimesAtParkingArrival=new LinkedListValueHashMap<Id, Double>();
		walkingTimesAtParkingDeparture=new LinkedListValueHashMap<Id, Double>();
		sumOfParkingDurations=new DoubleValueHashMap<Id>();
		finishHandlingCalled=false;
	}

	
	public Set<Id> getPersonIdsWhoUsedCar(){
		if (!finishHandlingCalled){
			DebugLib.stopSystemAndReportInconsistency("finish handling must be called before calling this method");
		}
		
		return currentArrivalTime.keySet();
	}
	
	@Override
	public void handleEvent(ParkingArrivalEvent event) {
		Id personId = event.getActStartEvent().getPersonId();
		currentArrivalTime.put(personId,event.getActStartEvent().getTime());
	
		updateWalkingTime(event, personId);
	}

	private void updateWalkingTime(ParkingArrivalEvent event, Id personId) {
		Coord activityCoord=getActivityCoordinate(event.getActStartEvent().getFacilityId(), event.getActStartEvent().getLinkId());
		double walkingTime=GeneralLib.getDistance(event.getParking().getCoord(),activityCoord)/controler.getConfig().plansCalcRoute().getWalkSpeed();
		walkingTimesAtParkingArrival.put(personId, walkingTime);
	}

	
	
	private Coord getActivityCoordinate(Id facilityId, Id linkId){
		Coord activityCoord=null;
		
		if (facilityId!=null){
			activityCoord=controler.getFacilities().getFacilities().get(facilityId).getCoord();
		} else if (linkId!=null) {
			activityCoord=controler.getNetwork().getLinks().get(linkId).getCoord();
		} else {
			DebugLib.stopSystemAndReportInconsistency("facilityId and linkId of activity can't both be null!");
		}
		
		return activityCoord;
	}
	

	

	@Override
	public void handleEvent(ParkingDepartureEvent event) {
		Id personId = event.getAgentDepartureEvent().getPersonId();
		if (firstDepartureTimeNotInitializedYet(personId)){
			initializeFirstDepartureTime(event, personId);
		} else {
			sumOfParkingDurations.incrementBy(personId, GeneralLib.getIntervalDuration(currentArrivalTime.get(personId), event.getAgentDepartureEvent().getTime()));
		}
		
		// attention: the first departure event is here at first position
		updateWalkingTime(event, personId);
	}

	private void updateWalkingTime(ParkingDepartureEvent event, Id personId) {
		Coord activityCoord=controler.getNetwork().getLinks().get(event.getAgentDepartureEvent().getLinkId()).getCoord();
		double walkingTime=GeneralLib.getDistance(event.getParking().getCoord(),activityCoord)/controler.getConfig().plansCalcRoute().getWalkSpeed();
		walkingTimesAtParkingDeparture.put(personId, walkingTime);
	}

	private void initializeFirstDepartureTime(ParkingDepartureEvent event, Id personId) {
		double depTime = event.getAgentDepartureEvent().getTime();
		firstParkingDepartureTime.put(personId, depTime);
	}

	private boolean firstDepartureTimeNotInitializedYet(Id personId) {
		return !firstParkingDepartureTime.containsKey(personId);
	}
	
	public void finishHandling(){
		finishHandlingCalled=true;
		
		for (Id personId:currentArrivalTime.keySet()){
			Double arrivalTime = currentArrivalTime.get(personId);
			Double departureTime = firstParkingDepartureTime.get(personId);
			Double parkingInterval=GeneralLib.getIntervalDuration(arrivalTime,departureTime);
			sumOfParkingDurations.incrementBy(personId, parkingInterval);
		}
		
		
	}
	
	public double getSumOfWalkingTimes(Id personId){
		double walkingTimes=0;
		
		walkingTimes+=Lists.getSum(walkingTimesAtParkingArrival.get(personId));
		walkingTimes+=Lists.getSum(walkingTimesAtParkingDeparture.get(personId));
		
		return walkingTimes;
	}

	public double getSumOfParkingDurations(Id personId){
		return sumOfParkingDurations.get(personId);
	}

}
