/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStatsManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.stats.algorithms.BasicPlanStats;
import org.matsim.stats.algorithms.PlanScoreTrajectory;
import org.matsim.stats.algorithms.PlanStatsI;
import org.matsim.writer.MatsimWriter;

/**
 * @author laemmel
 */
public class PlanStatsManager  {

	public static final String STATS_MODULE = "stats";
	public static final String STATS_FILE= "statsOutFile";
	public static final String STATS_MANAGER_ACTIVE = "generateStats";

	public int minIteration;
	public int maxIteration;
	private final int iters;
	private HashMap<Id, ArrayList<PlanStatsI>> stats = null;

	public PlanStatsManager(){
		this.stats = new HashMap<Id, ArrayList<PlanStatsI>>();
		this.minIteration = Gbl.getConfig().controler().getFirstIteration();
		this.maxIteration = Gbl.getConfig().controler().getLastIteration();
		this.iters = 1 + this.maxIteration - this.minIteration;
	}

	public void run(final Plans population, final int iteration){
		for (Person pers : population.getPersons().values())
			run(pers,iteration);
	}

	public void run(final Person person, final int iteration) {
		run(person.getSelectedPlan(), iteration);
	}

	public void run(final Plan plan, final int iteration) {
		if (plan.firstPlanStatsAlgorithm == null){
			initPlan(plan);
		}
		plan.firstPlanStatsAlgorithm.run(plan.getScore(),  iteration);

	}

	public void print(final Plans population){
		for (Person pers : population.getPersons().values())
			print(pers);
	}

	public void print(final Person person) {
		Id persId = person.getId();
		for (PlanStatsI plan : this.stats.get(persId)){
			System.out.print("stats " + persId);
			plan.print();
			System.out.println();
		}
	}

	public void writeStats(final Plans population){

		StatsWriter wrt = new StatsWriter();
		wrt.openFile(Gbl.getConfig().getParam(STATS_MODULE, STATS_FILE));
		for (Person pers : population.getPersons().values()) {
			Id persId = pers.getId();
			for (PlanStatsI plan : this.stats.get(persId)){
				String str = persId + plan.printStr() + "\n";
				wrt.write(str);
			}
		}
		wrt.close();
	}

	public void initPlan(final Plan plan){
		plan.firstPlanStatsAlgorithm = new BasicPlanStats(new PlanScoreTrajectory(this.iters, this.minIteration));
		Id persId = plan.getPerson().getId();

		ArrayList<PlanStatsI> plans  = this.stats.get(persId);
		if (this.stats.get(persId) == null){
			plans = new ArrayList<PlanStatsI>();
			this.stats.put(persId,plans);
		}
		plans.add(plan.firstPlanStatsAlgorithm);
	}

	//a simple writer to dump the stats to file
	private static class StatsWriter extends MatsimWriter{

		@Override
		public void close(){
			try {
				super.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void openFile(final String filename){
			try {
				super.openFile(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write(final String str) {
			try {
				this.writer.write(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
