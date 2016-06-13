package playground.artemc.psim;

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
 * Created by artemc on 14/10/15.
 */
public class CrowdingPsimHandler implements PersonEntersVehicleEventHandler {

	EventsManager eventsManager;
	double marginalUtilityOfMoney;
	MoneyEventHandler moneyEventHandler;

	public SortedMap<Id<Person>, Double> getPerson2crowdingDisutility() {
		return person2crowdingDisutility;
	}

	public SortedMap<Id<Person>, Double> getPerson2crowdingToll() {
		return person2crowdingToll;
	}

	private SortedMap<Id<Person>, Double> person2crowdingDisutility = new TreeMap<Id<Person>, Double>();
	private SortedMap<Id<Person>, Double> person2crowdingToll = new TreeMap<Id<Person>, Double>();

	private HashMap<Integer, Double> avgCrowdingExternality;
	private HashMap<Integer, Double> avgCrowdingDisutility;

	private static boolean internalizationOfComfortDisutility = false;

	public CrowdingPsimHandler(EventsManager eventsManager, HashMap<Integer, Double> avgCrowdingDisutility,  HashMap<Integer, Double> avgCrowdingExternality, double marginalUtilityOfMoney, MoneyEventHandler moneyEventHandler, boolean internalizationOfComfortDisutility){
		this.avgCrowdingDisutility = avgCrowdingDisutility;
		this.avgCrowdingExternality = avgCrowdingExternality;
		this.eventsManager = eventsManager;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.moneyEventHandler = moneyEventHandler;
		this.internalizationOfComfortDisutility = internalizationOfComfortDisutility;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getPersonId().toString().equals("4745_8")){
			System.out.println();
		}

		if(event.getVehicleId().toString().contains("dummy") && !event.getPersonId().toString().contains("pt")){

			int bin = (int) (event.getTime() / 300.0);
			double crowdingDisutility = 0.0;
			double crowdingToll = 0.0;

			if(avgCrowdingDisutility.containsKey(bin)) {
				crowdingDisutility = - avgCrowdingDisutility.get(bin);
			}
			if(avgCrowdingExternality.containsKey(bin)) {
				crowdingToll = - avgCrowdingExternality.get(bin) / marginalUtilityOfMoney;
			}


			if(person2crowdingDisutility.containsKey(event.getPersonId())){
				person2crowdingDisutility.put(event.getPersonId(), person2crowdingDisutility.get(event.getPersonId()) + crowdingDisutility);
			}
			else{
				person2crowdingDisutility.put(event.getPersonId(), crowdingDisutility);

			}

			if(internalizationOfComfortDisutility) {
				if (person2crowdingToll.containsKey(event.getPersonId())) {
					person2crowdingToll.put(event.getPersonId(), person2crowdingToll.get(event.getPersonId()) + crowdingToll);
					moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), crowdingToll));
				} else {
					person2crowdingToll.put(event.getPersonId(), crowdingToll);
					moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), crowdingToll));
				}
			}

//			if(avgCrowdingCost.containsKey(bin)){
//				double crowdingCharge = - avgCrowdingCost.get(bin) / marginalUtilityOfMoney;
//				//eventsManager.processEvent(new PersonMoneyEvent(86400.0,event.getPersonId(), crowdingCharge));
//				moneyEventHandler.handleEvent(new PersonMoneyEvent(event.getTime(),event.getPersonId(), crowdingCharge));
//			}
		}
	}

	@Override
	public void reset(int iteration) {


	}

}
