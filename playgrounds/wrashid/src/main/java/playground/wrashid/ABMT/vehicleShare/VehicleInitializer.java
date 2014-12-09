package playground.wrashid.ABMT.vehicleShare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;



/**
 * 
 * @author wrashid
 *
 */
public class VehicleInitializer implements IterationStartsListener {

	protected static final Logger log = Logger.getLogger(VehicleInitializer.class);
	
	public static HashMap<Plan, Boolean> hasElectricVehicle = new HashMap<Plan, Boolean>();
	
	public static HashMap<Id, Boolean> personHasElectricVehicle;
	
	public VehicleInitializer(){
		prepareForNewIteration();
	}
	
	public static void prepareForNewIteration(){
		personHasElectricVehicle = new HashMap<Id, Boolean>();
	}
	
	public static boolean hasCarLeg(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return true;
				}
			}
		}
		return false;
	}

	public static void initialize(Plan plan) {
		if (hasCarLeg(plan)) {
			if (!hasElectricVehicle.containsKey(plan)) {
				hasElectricVehicle.put(plan, MatsimRandom.getRandom().nextBoolean());
			}
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int evCount = 0;
		int cvCount = 0;
		int newKeysAdded = 0;
		int existingKeyUsed = 0;
		int numberOfPlansRemovedFromHM = 0;
		HashSet<Plan> allCurrentPlans = new HashSet<Plan>();
        for (Person person : event.getControler().getScenario().getPopulation().getPersons()
				.values()) {
			
			if (person.getId().toString().equalsIgnoreCase("111106347")){
				System.out.println();
			}
			
			if (hasCarLeg(person.getSelectedPlan())) {
				if (!hasElectricVehicle.containsKey(person.getSelectedPlan())) {
					hasElectricVehicle.put(person.getSelectedPlan(),
							MatsimRandom.getRandom().nextBoolean());
					newKeysAdded++;
				} else {
					existingKeyUsed++;
				}

				if (hasElectricVehicle.get(person.getSelectedPlan())) {
					evCount++;
					personHasElectricVehicle.put(person.getId(), true);
				} else {
					cvCount++;
					personHasElectricVehicle.put(person.getId(), false);
				}

				for (Plan plan : person.getPlans()) {
					allCurrentPlans.add(plan);
				}
			}
		}

		LinkedList<Plan> removePlans = new LinkedList<Plan>();
		for (Plan plan : hasElectricVehicle.keySet()) {
			if (!allCurrentPlans.contains(plan)) {
				removePlans.add(plan);
			}
		}

		for (Plan plan1 : removePlans) {
			hasElectricVehicle.remove(plan1);
			numberOfPlansRemovedFromHM++;
		}

	
		log.info("iteration: " + event.getIteration());

		log.info("numberOfPlansRemovedFromHM: "
				+ numberOfPlansRemovedFromHM);
		log.info("evCount: " + evCount);
		log.info("cvCount: " + cvCount);
		log.info("hasElectricVehicle.size(): "
				+ hasElectricVehicle.size());
		log.info("newKeysAdded: " + newKeysAdded);
		log.info("existingKeyUsed: " + existingKeyUsed);
		log.info("");
}
}
