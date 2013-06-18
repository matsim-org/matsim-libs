package org.matsim.contrib.matsim4urbansim.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class CreateTestPopulation {
	
	public static Population createTestPopulation(int nPersons){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		
		for(int i=0;i<nPersons;i++){
			Person person = population.getFactory().createPerson(new IdImpl(i));
			((PersonImpl)person).setAge(30);
			Plan plan = population.getFactory().createPlan();
			
			Activity home = population.getFactory().createActivityFromCoord("home", new CoordImpl(0, 100));
			home.setEndTime(8.*3600);
			Activity work = population.getFactory().createActivityFromCoord("work", new CoordImpl(200,100));
			work.setEndTime(17.*3600);
			Activity home2 = population.getFactory().createActivityFromCoord("home", new CoordImpl(0,100));
			home2.setEndTime(24.*3600);
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			
			plan.addActivity(home);
			plan.addLeg(leg);
			plan.addActivity(work);
			plan.addLeg(leg);
			plan.addActivity(home2);
			
			person.addPlan(plan);
			population.addPerson(person);
		}
		
		return population;
	}

}
