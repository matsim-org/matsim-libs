package playground.mmoyo.Validators;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;


public class PlanValidator {

	public PlanValidator() {
	}

	public int PlanCounter(NetworkLayer net, String plansFile){
		
		Population population = new PopulationImpl(false);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, net );
		plansReader.readFile(plansFile);
		return population.getPersons().values().size();
	}

	public void CountSuceesfullTrips(Population population){
		for (Person person : population.getPersons().values()){
			for (Plan plan: person.getPlans()){
				//--> validate the paths of each plan with path validator
			}
		}
			
	}
	
	
}