/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.agarwalamit.utils.plans;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Activity locations are taken exactly same as the initial person's plan for cloned persons' plans.
 * In general, this does not seems right. However, just for computational performance, it may look ok. 
 * @author amit
 */

public class PersonsCloner {

	public PersonsCloner(String inputPlans) {
		this.sc = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		this.random = MatsimRandom.getRandom();
	}

	private final Scenario sc;
	private final Random random;

	public static void main(String[] args) {
		String plansFile = "../../../../repos/runs-svn/patnaIndia/run106/inputs/SelectedPlansOnly.xml";
		PersonsCloner pc = new PersonsCloner(plansFile);
		pc.clonePersons(10);
	}

	/**
	 * This will clone the persons by mutating the activity duration within (-1/2,+1/2)h.
	 */
	public void clonePersons(final int cloningFactor){
		Population pop = sc.getPopulation();
		List<Person> persons = new ArrayList<>(pop.getPersons().values());
		for (Person p : persons) {
			for(int cf = 1; cf < cloningFactor ; cf++) {
				Id<Person> pOutId = Id.createPersonId( p.getId().toString().concat("_").concat(String.valueOf(cf)) );
				Person pOut = pop.getFactory().createPerson( pOutId  );
				pop.addPerson(pOut);
				for (Plan plan : p.getPlans()){
					Plan planOut = pop.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					for ( PlanElement pe : pes){
						if(pe instanceof Leg) {
							Leg leg = (Leg) pe;
							Leg legOut = pop.getFactory().createLeg(leg.getMode());
							planOut.addLeg(legOut);
						} else {
							Activity actIn = (Activity)pe;
							Activity actOut = pop.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
							actOut.setEndTime( actIn.getEndTime() - 1800 + random.nextDouble()*1800 );
							planOut.addActivity(actOut);
						}
					}
					pOut.addPlan(planOut);
				}
			}
		}
	}
	
	public Scenario getScenario(){
		return this.sc;
	}
}