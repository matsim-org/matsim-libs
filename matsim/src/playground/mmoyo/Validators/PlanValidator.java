package playground.mmoyo.Validators;

import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;


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