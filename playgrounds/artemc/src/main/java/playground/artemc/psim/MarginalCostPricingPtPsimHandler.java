package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by artemc on 15/10/15.
 */
public class MarginalCostPricingPtPsimHandler  implements PersonEntersVehicleEventHandler {
	
	EventsManager eventsManager;
	double marginalUtilityOfMoney;
	MoneyEventHandler moneyEventHandler;
	
	public SortedMap<Id<Person>, Double> getPerson2capacityDelayToll() {
		return person2capacityDelayToll;
	}
	
	public SortedMap<Id<Person>, Double> getPerson2transferDelayToll() {
		return person2transferDelayToll;
	}
	
	private SortedMap<Id<Person>, Double> person2capacityDelayToll = new TreeMap<Id<Person>, Double>();
	private SortedMap<Id<Person>, Double> person2transferDelayToll = new TreeMap<Id<Person>, Double>();
	
	private HashMap<Integer, Double> avgTransferDelayExternality;
	private HashMap<Integer, Double> avgCapacityDelayExternality;

	private static final Logger log = Logger.getLogger(MarginalCostPricingPtPsimHandler.class);

	public MarginalCostPricingPtPsimHandler(EventsManager eventsManager,  HashMap<Integer, Double> avgTransferDelayExternality,  HashMap<Integer, Double> avgCapacityDelayExternality, double marginalUtilityOfMoney, MoneyEventHandler moneyEventHandler){
		this.avgTransferDelayExternality = avgTransferDelayExternality;
		this.avgCapacityDelayExternality = avgCapacityDelayExternality;
		this.eventsManager = eventsManager;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.moneyEventHandler = moneyEventHandler;

		log.info("Initializing internalization of PT travel time delay caused by dwelling process and capacity constraints.");
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if(event.getVehicleId().toString().contains("dummy") && !event.getPersonId().toString().contains("pt")){
			
			int bin = (int) (event.getTime() / 300.0);
			double capacityDelayToll = 0.0;
			double transferDelayToll = 0.0;
			
			if(avgCapacityDelayExternality.containsKey(bin)) {
				capacityDelayToll = avgCapacityDelayExternality.get(bin);
			}
			if(avgTransferDelayExternality.containsKey(bin)) {
				transferDelayToll = avgTransferDelayExternality.get(bin);
			}


			if (person2transferDelayToll.containsKey(event.getPersonId())) {
				person2transferDelayToll.put(event.getPersonId(), person2transferDelayToll.get(event.getPersonId()) + transferDelayToll);
				moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), transferDelayToll));
			} else {
				person2transferDelayToll.put(event.getPersonId(), transferDelayToll);
				moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), transferDelayToll));
			}

			if(person2capacityDelayToll.containsKey(event.getPersonId())){
				person2capacityDelayToll.put(event.getPersonId(), person2capacityDelayToll.get(event.getPersonId()) + capacityDelayToll);
			    moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), capacityDelayToll));
			}
			else {
				person2capacityDelayToll.put(event.getPersonId(), capacityDelayToll);
				moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), capacityDelayToll));

			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		
		
	}
	
}

