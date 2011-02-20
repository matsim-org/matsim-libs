package playground.anhorni.LEGO.miniscenario.create;

import java.util.Collections;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.knowledges.KnowledgeImpl;

public class ChainModifier {
	private final static Logger log = Logger.getLogger(ChainModifier.class);
	private ScenarioImpl scenario;
	
	private Vector<PersonImpl> persons = new Vector<PersonImpl>();
	private Vector<Plan> plans = new Vector<Plan>();
	
	public void modify(ScenarioImpl scenario) {
		this.scenario = scenario;
		collectModifiablePersonsAndPlans();
		removeOldPlans();
		assignNewPlans();
	}
		
	private void collectModifiablePersonsAndPlans() {
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {			
			// no children and no border crossing traffic
			if (((PersonImpl) p).getAge() > 17 && Integer.parseInt(p.getId().toString()) < 1000000000) {
				persons.add(((PersonImpl) p));
				plans.add(p.getSelectedPlan());
				
				this.createKnowledge((PersonImpl) p);
			}
			counter++;			
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
		log.info("Persons to modify: " + this.persons.size());
	}
	
	private void createKnowledge(PersonImpl p) {
		// home
		KnowledgeImpl knowledge = this.scenario.getKnowledges().getFactory().createKnowledge(p.getId(), "knowledge");
		ActivityImpl activity = (ActivityImpl)p.getSelectedPlan().getPlanElements().get(0);
		ActivityFacilityImpl facility = (ActivityFacilityImpl) this.scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
		ActivityOptionImpl actOpt = new ActivityOptionImpl(activity.getType(), facility);
		knowledge.addActivityOption(actOpt);
		
		// work
		
		
		// education
	}
	
	private void removeOldPlans() {
		log.info("removing plans ...");
		int counter = 0;
		int nextMsg = 1;
		for (PersonImpl p : persons) {
			p.getPlans().clear();
			counter++;			
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
	}
	
	private void assignNewPlans() {
		log.info("assigning new plans ...");
		int counter = 0;
		int nextMsg = 1;
		
		Collections.shuffle(this.plans);
		
		for (PersonImpl p : persons) {
			Plan plan = this.plans.firstElement();
			p.addPlan(plan);
			p.setSelectedPlan(plan);
			counter++;			
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
	}
	
	// need a gravity model here!
	private void correctFixedDestinations() {
		
	}
	
	// just take the closest. we do destination choice anyway!
	private void correctDiscretionaryDestinations() {
		
	}
	
}
