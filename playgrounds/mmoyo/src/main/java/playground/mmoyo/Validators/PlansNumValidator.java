package playground.mmoyo.Validators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import playground.mmoyo.utils.DataLoader;

/**Validates that all persons in a population has the same number of plans*/
public class PlansNumValidator {
	private final static Logger log = Logger.getLogger(PlansNumValidator.class);
	final Population pop;
	
	public PlansNumValidator(final Population pop){
		this.pop = pop;
	}
	
	public void run (final int numPlans){
		final String numError = "Agent has a different number of plans: ";
		final String equalError = "Agent has a similar plans: ";
		final String sep = " ";
		for(Person person: this.pop.getPersons().values()){
			log.info(person.getId() + sep + person.getPlans().size());

			if (person.getPlans().size() != numPlans){
				log.error(numError + person.getId());
			}
			
			for (int i=0; i< person.getPlans().size()-2 ; i++){
				Plan currPlan= person.getPlans().get(i);
				for (int j=i+1; j< person.getPlans().size()-1 ; j++){
					Plan compPlan= person.getPlans().get(j);
					if (currPlan == compPlan){
						log.error(equalError + person.getId());
					}
				}
			}
		}
		
	}
	
	public static void main(String[] args) {
		String popFilePath= "../../input/rou_mut/ro_mut10_0_240__8_5_720__6_0_1200.xml.gz";
		Population pop = new DataLoader().readPopulation(popFilePath);
		new PlansNumValidator(pop).run(4);

	}

}
