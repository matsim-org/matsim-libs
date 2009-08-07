/* *********************************************************************** *
 * project: org.matsim.*
 * DatFileMaker.java
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

package playground.mfeil.MDSAM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;


/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class SimilarityInitializer {

	protected final PopulationImpl population;
	protected List<List<Double>> sims;
	protected static final Logger log = Logger.getLogger(SimilarityInitializer.class);
	


	public SimilarityInitializer(final PopulationImpl population) {
		this.population=population;
	}
	
	public List<List<Double>> getSimilarityOfPlans () {
		UniSAM sim = new UniSAM ();
		this.sims = new ArrayList<List<Double>>();
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			this.sims.add(new ArrayList<Double>());
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				if (plan.equals(person.getSelectedPlan())) {
					this.sims.get(this.sims.size()-1).add(0.0);
					continue;
				}
		/*		System.out.println("origPlan");
				for (int i=0;i<person.getSelectedPlan().getPlanElements().size();i+=2){
					System.out.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(i))).getType()+" ");
				}
				System.out.println();
				System.out.println("comparePlan");
				for (int i=0;i<plan.getPlanElements().size();i+=2){
					System.out.print(((ActivityImpl)(plan.getPlanElements().get(i))).getType()+" ");
				}
				System.out.println();*/
				this.sims.get(this.sims.size()-1).add(sim.run(person.getSelectedPlan(), plan));
			}
		}
		return this.sims;
	}

}

