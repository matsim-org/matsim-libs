package playground.kturner.freightKt.analyse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;


class TripEventHandler  implements ActivityEndEventHandler, LinkEnterEventHandler, 
PersonDepartureEventHandler, PersonArrivalEventHandler {

	public class VehicleTypeSpezificCapabilities{
		private double fixCosts;
		private double costsPerMeter;
		private double costsPerSecond;
		private double fuelConsumtion;
		private double emissionsPerMeter; //TODO: Wird nicht mit eingelesen in CarrierVehicleTypeReader -> Eigenen Reader und eigene Klassen bauen; Zur Not erstmal per Hand setzen...
		private int capacity;
		
		VehicleTypeSpezificCapabilities(double fixCosts, double costsPerMeter,
				double costsPerSecond, double fuelConsumtion, double emissionsPerMeter, int capacity) {
			this.fixCosts = fixCosts;
			this.costsPerMeter = costsPerMeter;
			this.costsPerSecond = costsPerSecond;
			this.fuelConsumtion = fuelConsumtion;
			this.emissionsPerMeter = emissionsPerMeter;
			this.capacity = capacity;
		}

		double getFixCosts() {
			return fixCosts;
		}

		double getCostsPerMeter() {
			return costsPerMeter;
		}

		double getCostsPerSecond() {
			return costsPerSecond;
		}

		double getCapacity() {
			return capacity;
		}	
		
		double getFuelConsumtion() {
			return fuelConsumtion;
		}	
		
		double getEmissionsPerMeter() {
			return emissionsPerMeter;
		}	
	}

	private final static Logger log = Logger.getLogger(TripEventHandler.class);

	private Scenario scenario;
	private CarrierVehicleTypes vehicleTypes;

	private Map<Id<VehicleType>, VehicleTypeSpezificCapabilities> vehTypId2Capabilities = new HashMap<Id<VehicleType>, VehicleTypeSpezificCapabilities>();

	
	Scenario getScenario() {
		return scenario;
	}

	CarrierVehicleTypes getVehicleTypes() {
		return vehicleTypes;
	}

	Map<Id<VehicleType>, VehicleTypeSpezificCapabilities> getVehTypId2Capabilities() {
		return vehTypId2Capabilities;
	}

	private Map<Id<Person>,Integer> personId2currentTripNumber = new HashMap<Id<Person>, Integer>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2travelTime = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Double> driverId2totalDistance = new HashMap<Id<Person>,Double>();

	
	public TripEventHandler(Scenario scenario, CarrierVehicleTypes vehicleTypes) {
		this.scenario = scenario;
		this.vehicleTypes = vehicleTypes;
		readVehicleTypeCapabilities();
	}

	private void readVehicleTypeCapabilities() {
		for (CarrierVehicleType vehType : vehicleTypes.getVehicleTypes().values()){
		//Emissionswerte nicht über Eigenschaften einlesbar :(	
			double emissionsPerMeter = -99999.0;
			switch (vehType.getId().toString()) {
			case "heavy40t" : emissionsPerMeter = 0.917;
				break;
			case "heavy26t" : emissionsPerMeter =	0.786;
				break;
			case "heavy26t_frozen" : emissionsPerMeter = 0.786;
				break;
			case "medium18t" : emissionsPerMeter = 0.655;
				break;
			case "medium18telectro" : emissionsPerMeter = 0.433;
				break;
			case "light8t" : emissionsPerMeter = 0.524;
				break;
			case "light8telectro" : emissionsPerMeter = 0.346;
				break;
			case "light8t_frozen" : emissionsPerMeter = 0.524;
				break;
			case "light8telectro_frozen" : emissionsPerMeter = 0.524;
			}
				
			VehicleTypeSpezificCapabilities vehTypeCapabilities = 
					new VehicleTypeSpezificCapabilities(vehType.getVehicleCostInformation().fix, 
							vehType.getVehicleCostInformation().perDistanceUnit, 
							vehType.getVehicleCostInformation().perTimeUnit, 
							vehType.getEngineInformation().getGasConsumption(),
							emissionsPerMeter,
							vehType.getCarrierVehicleCapacity());
			
			vehTypId2Capabilities.put(vehType.getId(), vehTypeCapabilities);
		}
		
	}

	@Override
	public void reset(int iteration) {
		personId2currentTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2travelTime.clear();
		personId2tripNumber2amount.clear();
		driverId2totalDistance.clear();
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkLength = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		
		if(driverId2totalDistance.containsKey(event.getVehicleId())){
			driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),driverId2totalDistance.get(Id.createPersonId(event.getVehicleId())) + linkLength);
		} else {
			driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),linkLength);
		}

		// updating the trip Length
		int tripNumber = personId2currentTripNumber.get(event.getVehicleId());
		double distanceBefore = personId2tripNumber2tripDistance.get(event.getVehicleId()).get(tripNumber);
		double updatedDistance = distanceBefore + linkLength;
		Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getVehicleId());
		tripNumber2tripDistance.put(tripNumber, updatedDistance);
		personId2tripNumber2tripDistance.put(Id.createPersonId(event.getVehicleId()), tripNumber2tripDistance);		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personId2currentTripNumber.containsKey(event.getPersonId())) {
			// the following trip is at least the person's second trip
			personId2currentTripNumber.put(event.getPersonId(), personId2currentTripNumber.get(event.getPersonId()) + 1);
			
			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
			tripNumber2departureTime.put(personId2currentTripNumber.get(event.getPersonId()), event.getTime());
			personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
			
			Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getPersonId());
			tripNumber2tripDistance.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
			personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
				
			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
			tripNumber2amount.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
			personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
	
		} else {
			// the following trip is the person's first trip
			personId2currentTripNumber.put(event.getPersonId(), 1);
			
			Map<Integer,Double> tripNumber2departureTime = new HashMap<Integer, Double>();
			tripNumber2departureTime.put(1, event.getTime());
			personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
			
			Map<Integer,Double> tripNumber2tripDistance = new HashMap<Integer, Double>();
			tripNumber2tripDistance.put(1, 0.0);
			personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
			
			Map<Integer,Double> tripNumber2amount = new HashMap<Integer, Double>();
			tripNumber2amount.put(1, 0.0);
			personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
		}
		
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		Map<Integer, Double> tripNumber2travelTime;
		if (this.personId2tripNumber2travelTime.containsKey(event.getPersonId())) {
			tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

		} else {
			tripNumber2travelTime = new HashMap<Integer, Double>();
		}
		
		int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());
		tripNumber2travelTime.put(currentTripNumber, event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber));
		this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
	}
	
	public Map<Id<Person>,List<Double>> getPersonId2listOfDistances(String carrierIdString) {
		Map<Id<Person>,List<Double>> personId2listOfDistances = new HashMap<Id<Person>, List<Double>>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<Double>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}else{
				}
			}
			personId2listOfDistances.put(personId, distances);
		}
		return personId2listOfDistances;
	}
	
	//new KT
	public Map<Id<Person>,Double> getPersonId2TourDistances(String carrierIdString) {
		Map<Id<Person>,Double> personId2listOfTourDistances = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					if (personId2listOfTourDistances.containsKey(personId)){
						personId2listOfTourDistances.put(personId, personId2listOfTourDistances.get(personId) + distance);
					} else {
						personId2listOfTourDistances.put(personId, distance);
					}
				}else{
					//do nothing
				}
			}
		}
		return personId2listOfTourDistances;
	}
	
	public Map<Id<Person>, Double> getPersonId2TravelTimes(String carrierIdString) {
		Map<Id<Person>,Double> personId2listOfTravelTimes = new HashMap<Id<Person>, Double>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			for(int i : personId2tripNumber2travelTime.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double travelTime = personId2tripNumber2travelTime.get(personId).get(i);
					if (personId2listOfTravelTimes.containsKey(personId)){
						personId2listOfTravelTimes.put(personId, personId2listOfTravelTimes.get(personId) + travelTime);
					} else {
						personId2listOfTravelTimes.put(personId, travelTime);
					}
				} else {
					//do nothing
				}
			}
		}
		return personId2listOfTravelTimes;
	}
	
	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres FEhrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	public Map<Id<VehicleType>,Double> getVehTypId2TourDistances(Id<VehicleType> vehTypeId) {
		System.out.println("Distance für fzgTyp soll ermittelt werden " + vehTypeId.toString());
		Map<Id<VehicleType>,Double> vehTypeId2TourDistances = new HashMap<Id<VehicleType>, Double>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
					if (vehTypeId.toString().endsWith("frozen") == personId.toString().contains("frozen")) {//keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
						double distance = personId2tripNumber2tripDistance.get(personId).get(i);
						if (vehTypeId2TourDistances.containsKey(vehTypeId)){
							vehTypeId2TourDistances.put(vehTypeId, vehTypeId2TourDistances.get(vehTypeId) + distance);
							System.out.println("Aktuelle Distance für Person " + personId.toString() + " ; " + "_" +vehTypeId.toString() + "_" + "added: " + distance);
						} else {
							vehTypeId2TourDistances.put(vehTypeId, distance);
							System.out.println("Distance für Person " + personId.toString() + " ; " + "_" +vehTypeId.toString() + "_" + "added: " + distance);
						}
					}
				}else{
					//do nothing
				}
			}
		}
		return vehTypeId2TourDistances;
	}
	
	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres FEhrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	public Map<Id<VehicleType>, Double> getVehTypId2TravelTimes(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Double> vehTypeId2TravelTimes = new HashMap<Id<VehicleType>, Double>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			for(int i : personId2tripNumber2travelTime.get(personId).keySet()){
				if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
					if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
						if (vehTypeId.toString().endsWith("frozen") == personId.toString().contains("frozen")) {//keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
							double travelTime = personId2tripNumber2travelTime.get(personId).get(i);
							if (vehTypeId2TravelTimes.containsKey(vehTypeId)){
								vehTypeId2TravelTimes.put(vehTypeId, vehTypeId2TravelTimes.get(vehTypeId) + travelTime);
							} else {
								vehTypeId2TravelTimes.put(vehTypeId, travelTime);
							}
						}
					}
				} else {
					//do nothing
				}
			}
		}
		return vehTypeId2TravelTimes;
	}
	
	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres FEhrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	public Map<Id<VehicleType>, Integer> getVehTypId2VehicleNumber(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Integer> vehTypeId2VehicleNumber = new HashMap<Id<VehicleType>, Integer>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
				if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
					if (vehTypeId.toString().endsWith("frozen") == personId.toString().contains("frozen")) {//keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
						if (vehTypeId2VehicleNumber.containsKey(vehTypeId)){
							vehTypeId2VehicleNumber.put(vehTypeId, vehTypeId2VehicleNumber.get(vehTypeId) +1);
						} else {
							vehTypeId2VehicleNumber.put(vehTypeId, 1);
						}
					}
				}
			} else {
					//do nothing
				}
		}
		return vehTypeId2VehicleNumber;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
	}


}
