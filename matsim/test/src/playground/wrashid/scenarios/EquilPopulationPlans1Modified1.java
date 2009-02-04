package playground.wrashid.scenarios;

import java.util.ArrayList;

import org.matsim.mobsim.deqsim.util.testable.PopulationModifier;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	Population population=null;
	
	public Population getPopulation(){
		return population;
	} 
	

	public Population modifyPopulation(Population population) {
		// modify population: we need act end time (plan has only duration)
		this.population=population;
		Person p=population.getPerson("1");
		Plan plan= p.getSelectedPlan();
		ArrayList<Object> actsLegs =plan.getActsLegs();
		((Leg)actsLegs.get(1)).setDepartureTime(360);
		((Leg)actsLegs.get(3)).setDepartureTime(900); // this requires immediate departure after arrival
		((Leg)actsLegs.get(5)).setDepartureTime(2000);
		return population;
	}
	
	
	
	
}
