package playground.dhosse.frequencyBasedPt.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateTestPopulation {
	
	public static Population createTestPopulation(Network network, int nPersons){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        PopulationFactory factory = scenario.getPopulation().getFactory();
		Population population = scenario.getPopulation();

		final double x = -10;
		Coord homeCoord = new Coord(x, (double) 150);
		Coord workCoord = new Coord((double) 610, (double) 150);
		
		for(int i=0;i<nPersons;i++){
			
			Person p = factory.createPerson(Id.create("p_"+i, Person.class));
			Plan plan = factory.createPlan();
			
			Activity home = factory.createActivityFromCoord("h", homeCoord);
			home.setEndTime(6*3600);
			plan.addActivity(home);
			Leg leg = factory.createLeg(TransportMode.pt);
			plan.addLeg(leg);
			Activity work = factory.createActivityFromCoord("w", workCoord);
			work.setEndTime(18*3600);
			plan.addActivity(work);
			plan.addLeg(leg);
			plan.addActivity(home);
			
			p.addPlan(plan);
			
			population.addPerson(p);
			
		}
		
		return population;
		
	}

}
