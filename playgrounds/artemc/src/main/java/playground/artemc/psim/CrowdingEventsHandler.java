package playground.artemc.psim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.Vehicle;
import playground.artemc.crowding.VehicleStateAdministrator;
import playground.artemc.crowding.events.CrowdedPenaltyEvent;
import playground.artemc.crowding.events.CrowdedPenaltyEventHandler;
import playground.artemc.crowding.events.PersonCrowdednessEvent;
import playground.artemc.crowding.events.PersonCrowdednessEventHandler;

import java.util.HashMap;

/**
 * Created by artemc on 13/10/15.
 */
public class CrowdingEventsHandler implements CrowdedPenaltyEventHandler, PersonEntersVehicleEventHandler{

	public HashMap<Integer, Integer> getTimeBinToPassengers() {
		return timeBinToPassengers;
	}

	public HashMap<Integer, Double> getTimeBinToPenalty() {
		return timeBinToPenalty;
	}

	public HashMap<Integer, Double> getTimeBinToExternality() {
		return timeBinToExternality;
	}

	private HashMap<Integer, Integer> timeBinToPassengers = new HashMap<Integer, Integer>();
	private HashMap<Integer, Double> timeBinToPenalty = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> timeBinToExternality = new HashMap<Integer, Double>();

	private HashMap<Id<Person>, Double> personToBoardingTime = new HashMap<Id<Person>, Double>();

	double sum = 0.0;

	@Override
	public void reset(int iteration) {
		timeBinToPassengers = new HashMap<Integer, Integer>();
		timeBinToPenalty = new HashMap<Integer, Double>();
		timeBinToExternality = new HashMap<Integer, Double>();
	}


	@Override
	public void handleEvent(CrowdedPenaltyEvent event) {

		if(!event.getPersonId().toString().contains("pt")){
			CrowdedPenaltyEvent cpEvent = (CrowdedPenaltyEvent) event;

			//int bin = (int) (event.getTime() / 300.0);
			int bin = (int) (personToBoardingTime.get(event.getPersonId()) / 300.0);

			if (timeBinToPenalty.containsKey(bin)) {
				timeBinToPenalty.put(bin, timeBinToPenalty.get(bin) + cpEvent.getPenalty());
				timeBinToExternality.put(bin, timeBinToExternality.get(bin) + cpEvent.getExternality());
			} else {
				timeBinToPenalty.put(bin, Double.valueOf(cpEvent.getPenalty()));
				timeBinToExternality.put(bin, Double.valueOf(cpEvent.getExternality()));
			}

			sum = sum +  Double.valueOf(event.getPenalty()) + Double.valueOf(event.getExternality());
		}

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		if(!event.getPersonId().toString().contains("pt") && event.getVehicleId().toString().contains("bus")){
			int bin = (int) (event.getTime() / 300.0);
			if (timeBinToPassengers.containsKey(bin)) {
				timeBinToPassengers.put(bin, (timeBinToPassengers.get(bin) + 1));
			}
			else{
				timeBinToPassengers.put(bin, 1);
			}

			if(personToBoardingTime.containsKey(event.getPersonId())){
				personToBoardingTime.remove(event.getPersonId());
			}

			personToBoardingTime.put(event.getPersonId(),event.getTime());
		}
	}
}
