package playground.anhorni.locationchoice.cs.depr.filters;


import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;

public abstract class TripFilter {
	
	protected List<ChoiceSet> choiceSets = new Vector<ChoiceSet>();
	private final static Logger log = Logger.getLogger(TripFilter.class);
		
	public List<ChoiceSet> apply(Population population, String mode) {	
		Iterator<Person> person_it = population.iterator();
		while (person_it.hasNext()) {
			Person person = person_it.next();		
			// Person only has one plan at this stage
			Plan plan = person.getSelectedPlan();		
			filterPlan(plan, mode);
		}	
		log.info("Number of "+ mode + " trips :  " + this.choiceSets.size());
		return this.choiceSets;
	}
	
	protected abstract boolean filterPlan(final Plan plan, String mode);
	
}
