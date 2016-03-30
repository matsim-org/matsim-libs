package playground.dhosse.scenarios.bhls.example;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

public class CreateExamplePopulation {

	private static final double carShare = 0.7;
	/*
	 * 
	 *		A		B		C		Z
	 * A	0		2000	2000	4000
	 * B	2000	0		2000	4000
	 * C	2000	2000	0		4000
	 * D	2000	2000	2000	4000
	 * Z	4000	4000	4000	0
	 * 
	 */
	public static void createAgents(Scenario scenario){
		
		Population population = scenario.getPopulation();
		
		addAgents(population, "A", "B", Id.createLinkId("a0"), Id.createLinkId("2b"), 2000);
		addAgents(population, "A", "Z", Id.createLinkId("a0"), Id.createLinkId("4z"), 4000);
		
		addAgents(population, "B", "A", Id.createLinkId("b2"), Id.createLinkId("0a"), 2000);
		addAgents(population, "B", "Z", Id.createLinkId("b2"), Id.createLinkId("4z"), 4000);
		
		addAgents(population, "Z", "A", Id.createLinkId("z4"), Id.createLinkId("0a"), 4000);
		addAgents(population, "Z", "B", Id.createLinkId("z4"), Id.createLinkId("2b"), 4000);
		
	}
	
	private static void addAgents(Population population, String home, String work, Id<Link> from, Id<Link >to, int amount){
		
		String mode =  MatsimRandom.getLocalInstance().nextDouble() <= carShare ? TransportMode.car : "pt";
		
		for(int i = 0; i < amount; i++){
			
			Person p = population.getFactory().createPerson(Id.createPersonId(home + "_" + work + "_" + i));
			Plan plan = population.getFactory().createPlan();
			
			//home
			Activity firstHome = population.getFactory().createActivityFromLinkId("home", from);
			firstHome.setEndTime(6 * 3600 + MatsimRandom.getLocalInstance().nextInt(3601));
			plan.addActivity(firstHome);
			
			Leg leg = population.getFactory().createLeg(mode);
			plan.addLeg(leg);
			
			//work
			Activity workAct = population.getFactory().createActivityFromLinkId("work", to);
			workAct.setMaximumDuration(8 * 3600);
			plan.addActivity(workAct);
			
			Leg leg2 = population.getFactory().createLeg(mode);
			plan.addLeg(leg2);
			
			//home
			Activity secondHome = population.getFactory().createActivityFromLinkId("home", from);
			secondHome.setEndTime(24 * 3600);
			plan.addActivity(secondHome);
			
			p.addPlan(plan);
			p.setSelectedPlan(plan);
			population.addPerson(p);
			
		}
		
	}

}
