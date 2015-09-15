/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.PLOC;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MultiDaysGenerator {
	
	private Random randomNumberGenerator;
	private SinglePlanGenerator singlePlanGenerator;
	private Population staticPopulation;
	private ScenarioImpl scenarioWriteOut;
	private NetworkImpl network;
	private boolean temporalVar;
		
	public MultiDaysGenerator(Random randomNumberGenerator, Population staticPopulation, ScenarioImpl scenarioWriteOut,
			NetworkImpl network, boolean temporalVar, ObjectAttributes personAttributes) {
		this.randomNumberGenerator = randomNumberGenerator;
		for (int i = 0; i < 1000; i++) {
			this.randomNumberGenerator.nextDouble();
		}
		this.staticPopulation = staticPopulation;
		this.scenarioWriteOut = scenarioWriteOut;
		this.network = network;
		this.temporalVar = temporalVar;
		singlePlanGenerator = new SinglePlanGenerator(scenarioWriteOut.getActivityFacilities());
	}
	
	public void generatePlans(int runId) {
		List<Integer> keyList = new Vector<Integer>();
		for (Id id : staticPopulation.getPersons().keySet()) {
			keyList.add(Integer.parseInt(id.toString()));
		}
		for (int day = 0; day < 5; day++) {
			double limit = 0.0;
			if (temporalVar) {
				limit = MultiplerunsControler.share[day];
			}
			else {
				limit = this.getAverageShare();
			}
			Collections.shuffle(keyList, randomNumberGenerator);
			
			this.generatePlan(keyList, limit, day);
			
			String path = Create3TownsScenario.outputFolder + "/runs/run" + runId + "/day" + day;
			new File(path).mkdirs();
			this.writePlansAndFacs(path + "/plans.xml", path + "/facilities.xml");
			scenarioWriteOut.getPopulation().getPersons().clear();
		}	
	}
	
	private void generatePlan(List<Integer> keyList, double limit, int day) {
		int cnt = 0;
		for (Integer id : keyList) {
			Person p = staticPopulation.getPersons().get(Id.create(id, Person.class));

			// copy person -------
			Person pTmp = PersonImpl.createPerson(Id.create(id, Person.class));
			// copy person -------
			
			boolean worker = false;
			if (cnt < limit * keyList.size()) {
				worker = true;
				cnt++;
			}
			boolean shortWorker;
			// actually on Friday all workers are short workers for the scenario taking into account temporal correlations
			if (this.temporalVar && day == 4) {
				shortWorker = true;
			}
			// for the average scenario distribute the Friday short workers over all days
			// only guaranteed to work correctly if work shares decrease during the week
			else if (!this.temporalVar && cnt < MultiplerunsControler.share[4] / 5.0 * keyList.size()){
				shortWorker = true;
			}
			else {
				shortWorker = false;
			}
			PlanImpl plan = singlePlanGenerator.generatePlan(worker, shortWorker, p);
			pTmp.addPlan(plan);
			scenarioWriteOut.getPopulation().addPerson(pTmp);
		}
	}
			
	public void writePlansAndFacs(String plansFile, String facsFile) {	
		new PopulationWriter(scenarioWriteOut.getPopulation(), network).write(plansFile);
		new FacilitiesWriter(this.scenarioWriteOut.getActivityFacilities()).write(facsFile);
	}
	
	private double getAverageShare() {
		// find the average share of workers
		double averageShare = 0.0;
		for (int i = 0; i < MultiplerunsControler.share.length; i++) {
			averageShare += (MultiplerunsControler.share[i] / 5.0);
		}
		return averageShare;
	}
}
