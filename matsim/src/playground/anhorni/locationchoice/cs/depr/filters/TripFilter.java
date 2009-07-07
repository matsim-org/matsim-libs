package playground.anhorni.locationchoice.cs.depr.filters;


import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;

public abstract class TripFilter {
	
	protected List<ChoiceSet> choiceSets = new Vector<ChoiceSet>();
	private final static Logger log = Logger.getLogger(TripFilter.class);
		
	public List<ChoiceSet> apply(PopulationImpl population, String mode) {	
		Iterator<PersonImpl> person_it = population.getPersons().values().iterator();
		while (person_it.hasNext()) {
			PersonImpl person = person_it.next();		
			// Person only has one plan at this stage
			PlanImpl plan = person.getSelectedPlan();		
			filterPlan(plan, mode);
		}	
		log.info("Number of "+ mode + " trips :  " + this.choiceSets.size());
		return this.choiceSets;
	}
	
	protected abstract boolean filterPlan(final PlanImpl plan, String mode);
	
}
