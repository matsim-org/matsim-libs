/* *********************************************************************** *
 * project: org.matsim.*
 * Text.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans;

import javax.management.RuntimeErrorException;

import junit.framework.TestCase;

import org.matsim.core.basic.v01.IdImpl;

import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.impl.PlainActivityImpl;
import playground.johannes.plans.plain.impl.PlainLegImpl;
import playground.johannes.plans.plain.impl.PlainPersonImpl;
import playground.johannes.plans.plain.impl.PlainPlanImpl;
import playground.johannes.plans.plain.impl.PlainPopulationImpl;
import playground.johannes.plans.plain.impl.PlainRouteImpl;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Population;
import playground.johannes.plans.view.impl.PersonView;
import playground.johannes.plans.view.impl.PopulationView;

/**
 * @author illenberger
 *
 */
public class Test extends TestCase {

	private long baseMem;
	
	public void test() {
		baseMem = getMemory();
		
		PlainPopulationImpl population = new PlainPopulationImpl();
		
		for(int n_p = 0; n_p < 100000; n_p++) {
			PlainPersonImpl person = new PlainPersonImpl(new IdImpl(String.valueOf(n_p)));
			population.addPerson(person);
			
			for(int n_pl = 0; n_pl < 5; n_pl++) {
				PlainPlanImpl plan = new PlainPlanImpl();
				
				for (int n_pe = 0; n_pe < 5; n_pe++) {
					if (n_pe % 2 == 0) {
						PlainActivityImpl act = new PlainActivityImpl();
						plan.addPlanElement(act);
					} else {
						PlainLegImpl leg = new PlainLegImpl();
						plan.addPlanElement(leg);
						
						PlainRouteImpl route = new PlainRouteImpl();
						leg.setRoute(route);
					}
				}
			}
		}
		
		printMemoryUsage();
		
		Population popView = new PopulationView(population);
		printMemoryUsage();
		System.out.println(String.format("Population consists of %1$s persons.", popView.getPersons().size()));
//		for(Person p : popView.getPersons().values()) {
//			for(Plan plan : p.getPlans()) {
//				for(PlanElement e : plan.getPlanElements()) {
//					
//				}
//			}
//		}
		printMemoryUsage();
		
		long time = System.currentTimeMillis();
		PlainPersonImpl person = new PlainPersonImpl(new IdImpl("99999999"));
		Person personView = new PersonView(person);
		popView.addPerson(personView);
		System.out.println(String.format("Population consists of %1$s persons.", popView.getPersons().size()));
		System.out.println(String.format("Synchronizing took %1$s msecs.", (System.currentTimeMillis()-time)));
		
		
		PlainPersonImpl p2 = new PlainPersonImpl(new IdImpl("123456789"));
		time = System.currentTimeMillis();
		population.addPerson(p2);
		for(int i = 0; i < 10; i++) {
			PlainPerson p = population.getPersons().get(new IdImpl(i));
			population.removePerson(p);
				
			
			
		}
		
		System.out.println(String.format("Population consists of %1$s persons.", popView.getPersons().size()));
		System.out.println(String.format("Synchronizing took %1$s msecs.", (System.currentTimeMillis()-time)));
	}
	
	public long getMemory() {
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		return usedMem;
	}
	
	public void printMemoryUsage() {
		System.out.println(String.format("Memory usage: %1$s Bytes", getMemory()-baseMem));
	}
}
