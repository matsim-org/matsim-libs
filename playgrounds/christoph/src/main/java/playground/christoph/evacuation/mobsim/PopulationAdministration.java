/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationAdministration.java
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

package playground.christoph.evacuation.mobsim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

/*
 * Administrates a population and defines which agents are in panic
 * and which households evacuate.
 */
public class PopulationAdministration implements BeforeMobsimListener, AfterMobsimListener {

	private static final String newLine = "\n";

	private static final String panicFileName = "panicPeople.txt.gz";
	private static final String nonPanicFileName = "nonPanicPeople.txt.gz";
	private static final String participatingHouseholdsFileName = "participatingHouseholds.txt.gz";
	private static final String nonParticipatingHouseholdsFileName = "nonParticipatingHouseholds.txt.gz";
	
	private final Scenario scenario;
	private final Set<Id> panicPeople;
	private final Set<Id> participatingHouseholds;
	private final Random random;
	
	private BufferedWriter panicPeopleWriter;
	private BufferedWriter nonPanicPeopleWriter;
	private BufferedWriter participatingHouseholdsWriter;
	private BufferedWriter nonParticipatingHouseholdsWriter;

	private String panicFile;
	private String nonPanicFile;
	private String participatingHouseholdsFile;
	private String nonParticipatingHouseholdsFile;
	
	public PopulationAdministration(Scenario scenario) {
		this.scenario = scenario;
		this.panicPeople = new HashSet<Id>();
		this.participatingHouseholds = new HashSet<Id>();
		this.random = MatsimRandom.getLocalInstance();
	}
	
	public void selectParticipatingHouseholds(double share) {
		participatingHouseholds.clear();
		
		for (Id id : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().keySet()) {
			if (this.random.nextDouble() <= share) participatingHouseholds.add(id);
		}
	}
	
	public void selectPanicPeople(double share) {
		panicPeople.clear();
		
		for (Id id : scenario.getPopulation().getPersons().keySet()) {
			if (this.random.nextDouble() <= share) panicPeople.add(id);
		}
	}
	
	public boolean isPersonInPanic(Id personId) {
		return panicPeople.contains(personId);
	}
	
	public void writeFiles() {
		
	}
	
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		panicFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), panicFileName);
		nonPanicFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), nonPanicFileName);
		participatingHouseholdsFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), participatingHouseholdsFileName);
		nonParticipatingHouseholdsFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), nonParticipatingHouseholdsFileName);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			panicPeopleWriter = IOUtils.getBufferedWriter(panicFile);
			nonPanicPeopleWriter = IOUtils.getBufferedWriter(nonPanicFile);
			participatingHouseholdsWriter = IOUtils.getBufferedWriter(participatingHouseholdsFile);
			nonParticipatingHouseholdsWriter = IOUtils.getBufferedWriter(nonParticipatingHouseholdsFile);
			
			writeHeaders();
			writeRows();

			panicPeopleWriter.flush();
			panicPeopleWriter.close();

			nonPanicPeopleWriter.flush();
			nonPanicPeopleWriter.close();

			participatingHouseholdsWriter.flush();
			participatingHouseholdsWriter.close();
			
			nonParticipatingHouseholdsWriter.flush();
			nonParticipatingHouseholdsWriter.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeHeaders() {
		try {
			panicPeopleWriter.write("personId");
			panicPeopleWriter.write(newLine);

			nonPanicPeopleWriter.write("personId");
			nonPanicPeopleWriter.write(newLine);

			participatingHouseholdsWriter.write("householdId");
			participatingHouseholdsWriter.write(newLine);

			nonParticipatingHouseholdsWriter.write("householdId");
			nonParticipatingHouseholdsWriter.write(newLine);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeRows() {
		try {
			for (Id id : this.scenario.getPopulation().getPersons().keySet()) {
				if (this.panicPeople.contains(id)) {
					panicPeopleWriter.write(id.toString());
					panicPeopleWriter.write(newLine);
				} else {
					nonPanicPeopleWriter.write(id.toString());
					nonPanicPeopleWriter.write(newLine);					
				}
			}
				
			for (Id id : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().keySet()) {
				if (this.participatingHouseholds.contains(id)) {
					participatingHouseholdsWriter.write(id.toString());
					participatingHouseholdsWriter.write(newLine);
				} else {
					nonParticipatingHouseholdsWriter.write(id.toString());
					nonParticipatingHouseholdsWriter.write(newLine);					
				}
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
}