package playground.wrashid.ABMT.vehicleShare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.controler.Controler;

import java.util.HashMap;

public class TollsManager implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {

	public static DoubleValueHashMap<Id> tollDisutilities;
	public static DoubleValueHashMap<Id> tollTimeOfEntry;
	public static DoubleValueHashMap<Id> tollTimeOfExit;	
	
	// personId, linkId
	public HashMap<Id, Id> previousLinks;

	private Network network;

	private Controler controler;

	public TollsManager(Controler controler) {
        this.network = controler.getScenario().getNetwork();
		this.controler = controler;
	}

	@Override
	public void reset(int iteration) {
		tollDisutilities = new DoubleValueHashMap<Id>();
		tollTimeOfEntry = new DoubleValueHashMap<Id>();
		tollTimeOfExit = new DoubleValueHashMap<Id>();
		previousLinks = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!event.getPersonId().toString().contains("pt")) {
			double tollDisutility;
			
//			if (!VehicleInitializer.hasElectricVehicle.containsKey(event.getPersonId())){
//				Plan plan=controler.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
//				System.out.println(VehicleInitializer.hasElectricVehicle.containsKey(event.getPersonId()));
//			}
            Person person= controler.getScenario().getPopulation().getPersons().get(event.getPersonId());
			
			if (!VehicleInitializer.hasElectricVehicle.containsKey(person.getSelectedPlan())){
				VehicleInitializer.initialize(person.getSelectedPlan());
			}
			
			if (VehicleInitializer.hasElectricVehicle.get(person.getSelectedPlan())) {
				tollDisutility = EVCVScoringFunction.evCosts.getPaidTollCost();
			} else {
				tollDisutility = EVCVScoringFunction.cvCosts.getPaidTollCost();
			}

			Coord coordinatesQuaiBridgeZH = new Coord(683423.0, 246819.0);
			Link prevLink = network.getLinks().get(previousLinks.get(event.getPersonId()));
			Link currentLink = network.getLinks().get(event.getLinkId());
			double radiusInMeters = GlobalTESFParameters.tollAreaRadius;
			
			if (GeneralLib.getDistance(coordinatesQuaiBridgeZH, currentLink) < radiusInMeters && GeneralLib.getDistance(coordinatesQuaiBridgeZH, prevLink) > radiusInMeters){
				tollTimeOfEntry.put(event.getPersonId(), event.getTime());				
			}
			
			if (GeneralLib.getDistance(coordinatesQuaiBridgeZH, currentLink) > radiusInMeters && GeneralLib.getDistance(coordinatesQuaiBridgeZH, prevLink) < radiusInMeters){
				tollTimeOfExit.put(event.getPersonId(), event.getTime());				
			}
			
			if (GeneralLib.getDistance(coordinatesQuaiBridgeZH, currentLink) < radiusInMeters
					&& GeneralLib.getDistance(coordinatesQuaiBridgeZH, prevLink) > radiusInMeters
					&& ((GlobalTESFParameters.morningTollStart < event.getTime() && GlobalTESFParameters.morningTollEnd > event.getTime()) 
							|| (GlobalTESFParameters.eveningTollStart < event.getTime() && GlobalTESFParameters.eveningTollEnd > event.getTime())
						)
				) {
				tollDisutilities.put(event.getPersonId(), tollDisutility);

			}
			previousLinks.put(event.getPersonId(), event.getLinkId());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		previousLinks.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)) {
			previousLinks.put(event.getPersonId(), event.getLinkId());
		}
	}

}
