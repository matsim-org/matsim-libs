package playground.dhosse.prt.analysis;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;

public class PrtEventsHandler implements ActivityEndEventHandler,
	ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	LinkEnterEventHandler {
	
	Map<Id<Person>,Double> travelDistancesWalkPerPerson = new HashMap<Id<Person>,Double>();
	Map<Id<Person>,Double> travelDistancesPrtPerPerson= new HashMap<Id<Person>,Double>();
	
	Map<Id<Person>,Double> travelTimesPerPersonWalk = new HashMap<Id<Person>,Double>();
	Map<Id<Person>,Double> personId2BeginWalkLeg = new HashMap<Id<Person>, Double>();
	Map<Id<Person>,Double> travelTimesPerPersonPrt = new HashMap<Id<Person>,Double>();
	Map<Id<Person>,Double> personId2BeginPrtLeg = new HashMap<Id<Person>,Double>();
	
	Map<Id<Person>,Double> waitingTimesPerPerson = new HashMap<Id<Person>,Double>();
	Map<Id<Person>,Double> personId2BeginWaitingTime = new HashMap<Id<Person>,Double>();
	
	Map<Id<Vehicle>,Double> travelDistancesPerVehicle = new HashMap<Id<Vehicle>,Double>();
	Map<Id<Vehicle>,List<Id<Person>>> vehicleIds2PassengerIds = new HashMap<Id<Vehicle>,List<Id<Person>>>();
	Map<Id<Vehicle>,Integer> vehicleIds2PassengerCounts = new HashMap<Id<Vehicle>,Integer>();
	Map<Id<Vehicle>,Double> travelTimesPerVehicle = new HashMap<Id<Vehicle>,Double>();
	Map<Id<Vehicle>,Double> vehicleId2BeginDriveTask = new HashMap<Id<Vehicle>,Double>();
	
	Map<Id<Vehicle>,List<Id<Person>>> currentPassengers = new HashMap<Id<Vehicle>,List<Id<Person>>>();
	
	private final Scenario scenario;
	
	public PrtEventsHandler(Scenario scenario){
		
		this.scenario = scenario;
		
		for(Person person : this.scenario.getPopulation().getPersons().values()){
			this.travelDistancesPrtPerPerson.put(person.getId(), 0.);
			this.travelDistancesWalkPerPerson.put(person.getId(), 0.);
			this.travelTimesPerPersonPrt.put(person.getId(), 0.);
			this.travelTimesPerPersonWalk.put(person.getId(), 0.);
			this.waitingTimesPerPerson.put(person.getId(), 0.);
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		
		for(Person person : this.scenario.getPopulation().getPersons().values()){
			this.travelDistancesPrtPerPerson.put(person.getId(), 0.);
			this.travelDistancesWalkPerPerson.put(person.getId(), 0.);
			this.travelTimesPerPersonPrt.put(person.getId(), 0.);
			this.travelTimesPerPersonWalk.put(person.getId(), 0.);
			this.waitingTimesPerPerson.put(person.getId(), 0.);
		}
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		/*if the person is not the driver of the car (or in this case the "virtual" driver since the vehicles operate
		unattended)*/
		if(this.scenario.getPopulation().getPersons().containsKey(event.getPersonId())){
			
			double wtime = this.waitingTimesPerPerson.get(event.getPersonId()) + 
					event.getTime() - this.personId2BeginWaitingTime.get(event.getPersonId());
			
			this.waitingTimesPerPerson.put(event.getPersonId(), wtime);
			this.personId2BeginWaitingTime.put(event.getPersonId(), 0.);
			
			Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(),Vehicle.class);
			
			if(!this.vehicleIds2PassengerIds.containsKey(vehicleId)){
			
				this.vehicleIds2PassengerIds.put(vehicleId, new ArrayList<Id<Person>>());
				
			}
			
			this.vehicleIds2PassengerIds.get(vehicleId).add(event.getPersonId());
			
			if(!this.currentPassengers.containsKey(vehicleId)){
				
				this.currentPassengers.put(vehicleId, new ArrayList<Id<Person>>());
				
			}
			
			this.currentPassengers.get(vehicleId).add(event.getPersonId());
			
			if(!this.vehicleIds2PassengerCounts.containsKey(vehicleId)){
				this.vehicleIds2PassengerCounts.put(vehicleId, 0);
			}
			
			int count = this.vehicleIds2PassengerCounts.get(vehicleId) + 1;
			this.vehicleIds2PassengerCounts.put(vehicleId, count);
			
			this.personId2BeginPrtLeg.put(event.getPersonId(), event.getTime());
			
		}
		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if(this.scenario.getPopulation().getPersons().containsKey(event.getPersonId())){
			
			if(!this.travelTimesPerPersonPrt.containsKey(event.getPersonId())){
				this.travelTimesPerPersonPrt.put(event.getPersonId(), 0.);
			}
			
			double ttime = this.travelTimesPerPersonPrt.get(event.getPersonId()) +
					event.getTime() - this.personId2BeginPrtLeg.get(event.getPersonId());
			this.travelTimesPerPersonPrt.put(event.getPersonId(), ttime);
			
			this.currentPassengers.get(event.getVehicleId()).remove(event.getPersonId());
			
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		if(this.scenario.getPopulation().getPersons().containsKey(event.getDriverId())){
			
			if(!this.travelDistancesWalkPerPerson.containsKey(event.getDriverId().toString())){
					
				this.travelDistancesWalkPerPerson.put(event.getDriverId(), 0.);
					
			}
				
			double d = this.travelDistancesWalkPerPerson.get(event.getDriverId()) +
					this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
				
			this.travelDistancesWalkPerPerson.put(event.getDriverId(), d);
				
		} else{

			Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(),Vehicle.class);
			
			if(!this.travelDistancesPerVehicle.containsKey(event.getVehicleId())){
				this.travelDistancesPerVehicle.put(vehicleId, 0.);
			}
			
			double d = this.travelDistancesPerVehicle.get(vehicleId) +
					this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			
			this.travelDistancesPerVehicle.put(vehicleId, d);
			
			if(this.currentPassengers.get(vehicleId) != null){
				
				for(Id<Person> personId : this.currentPassengers.get(vehicleId)){
				
					if(!this.travelDistancesPrtPerPerson.containsKey(personId)){
						this.travelDistancesPrtPerPerson.put(personId, 0.);
					}
					
					double distance = this.travelDistancesPrtPerPerson.get(personId) +
							this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
					
					this.travelDistancesPrtPerPerson.put(personId, distance);
					
				}
			}
			
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		Id<Vehicle> vehicleId = Id.create(event.getPersonId().toString(),Vehicle.class);
		
		if(event.getActType().equals("Waiting") && this.vehicleId2BeginDriveTask.containsKey(vehicleId)){
			
			if(!this.travelTimesPerVehicle.containsKey(vehicleId)){
				this.travelTimesPerVehicle.put(vehicleId, 0.);
			}
			
			double ttime = this.travelTimesPerVehicle.get(vehicleId) +
					event.getTime() - this.vehicleId2BeginDriveTask.get(vehicleId);
			
			this.travelTimesPerVehicle.put(vehicleId, ttime);
			
		} else if(event.getActType().equals("home")||event.getActType().equals("work")){
			
			this.personId2BeginWaitingTime.put(event.getPersonId(), 0.);
			
		}
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if(this.scenario.getPopulation().getPersons().containsKey(event.getPersonId())){
			
			if(event.getActType().equals("pt interaction")){
				
				this.personId2BeginWaitingTime.put(event.getPersonId(), event.getTime());
				
			}
			
		}
		
		//if a vehicle ends waiting, it probably has a drive task to perform
		if(event.getActType().equals("Waiting")){
			
			Id<Vehicle> vehicleId = Id.create(event.getPersonId().toString(),Vehicle.class);
			
			this.vehicleId2BeginDriveTask.put(vehicleId, event.getTime());
			
		}

	}

//	@Override
//	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
//		
//		Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(),Vehicle.class);
//		
//		this.vehicleId2BeginDriveTask.put(vehicleId, event.getTime());
//		
//	}
//
//	@Override
//	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//		
//		Id<Vehicle> vehicleId = Id.create(event.getVehicleId().toString(),Vehicle.class);
//		
//		if(!this.travelTimesPerVehicle.containsKey(vehicleId)){
//			this.travelTimesPerVehicle.put(vehicleId, 0.);
//		}
//		
//		double ttime = this.travelTimesPerVehicle.get(vehicleId) +
//				event.getTime() - this.vehicleId2BeginDriveTask.get(vehicleId);
//		
//		this.travelTimesPerVehicle.put(vehicleId, ttime);
//		
//	}

}
