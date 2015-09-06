package org.matsim.contrib.matrixbasedptrouter.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * 
 * This class creates a test population for junit tests.
 * 
 * @author dhosse
 *
 */

public final class CreateTestPopulation {
	
	
	/**
	 * 
	 * This method creates n persons for a test population. NOTE: It was written for the
	 * <code>AccessibilityTest</code>, all home locations are at (0,100) and all work
	 * locations are at (200,100). All legs are car legs.
	 * 
	 * @param nPersons the number of persons created for the population
	 * @return test population
	 */
	public static Population createTestPopulation(int nPersons){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		
		//create persons and add them to the population
		for(int i=0;i<nPersons;i++){
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			PersonImpl.setAge(person, 30);
			Plan plan = population.getFactory().createPlan();
			
			//create home activities at (0,100) and work activities at (200,100), all modes are by car
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

	/**
	 * 
	 * This method creates n persons for a test population to test pseudo pt.
	 * ALL home activites are at <code>homeCoord</code> and ALL work activities are
	 * at <code>workCoord</code>. All legs are pt legs.
	 * 
	 * @param nPersons the number of persons to be created
	 * @param homeCoord coordinate for ALL home activities
	 * @param workCoord coordinate for ALL work activities
	 * @return
	 */
	public static Population createTestPtPopulation(int nPersons, Coord homeCoord, Coord workCoord){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		
		//create persons and add them to the population
		for(int i=0;i<nPersons;i++){
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			PersonImpl.setAge(person, 30);
			Plan plan = population.getFactory().createPlan();
			
			//create home activities at homeCoord and work activities at workCoord, all modes are by car			
			Activity home = population.getFactory().createActivityFromCoord("home", homeCoord);
			home.setEndTime(8.*3600);
			Activity work = population.getFactory().createActivityFromCoord("work", workCoord);
			work.setEndTime(17.*3600);
			Activity home2 = population.getFactory().createActivityFromCoord("home", homeCoord);
			home2.setEndTime(24.*3600);
			Leg leg = population.getFactory().createLeg(TransportMode.pt);
			
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
