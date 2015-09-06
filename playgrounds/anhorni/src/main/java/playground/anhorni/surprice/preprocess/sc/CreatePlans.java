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

package playground.anhorni.surprice.preprocess.sc;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


public class CreatePlans {	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private ObjectAttributes incomes = new ObjectAttributes();
	private ObjectAttributes preferences = new ObjectAttributes();
	private Random rnd = new Random(221177);
		
	private final static Logger log = Logger.getLogger(CreatePlans.class);
		
	public static void main(final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		CreatePlans creator = new CreatePlans();
		creator.run(args[0]);
	}
	
	public void run(String outPath) {
		int nbrPersons = 500;
		for (int i = 0; i < nbrPersons; i++) {
			PersonImpl person = PersonImpl.createPerson(Id.create(i, Person.class));
			this.scenario.getPopulation().addPerson(person);
			PersonUtils.createAndAddPlan(person, true);
			Plan plan = person.getSelectedPlan();
			
			int offset = 1; //rnd.nextInt(600);
			ActivityImpl homeAct = ((PlanImpl) plan).createAndAddActivity("home");
			homeAct.setEndTime(6.0 * 3600.0 + offset);
			
			homeAct.setFacilityId(Id.create(1, ActivityFacility.class));
			homeAct.setCoord(new CoordImpl(-100.0, 0.0));
			homeAct.setLinkId(Id.create(1, Link.class));
			
			((PlanImpl) plan).createAndAddLeg("car");
						
			ActivityImpl act;
			
			if (i < nbrPersons/2.0) act = ((PlanImpl) plan).createAndAddActivity("work");
			else act = ((PlanImpl) plan).createAndAddActivity("leisure");
			
			act.setStartTime(6.0 * 3600.0 + 100.0 + 1.0 * offset);
			
			act.setFacilityId(Id.create(2, ActivityFacility.class));
			act.setCoord(new CoordImpl(2100.0, 0.0));
			act.setLinkId(Id.create(5, Link.class));
			
			
			if (i % 2 == 0) {
				incomes.putAttribute(person.getId().toString(), "income", new Double(0.2));
				preferences.putAttribute(person.getId().toString(), "dudm", new Double(0.1));
			} else {
				incomes.putAttribute(person.getId().toString(), "income",new Double(0.1));
				preferences.putAttribute(person.getId().toString(), "dudm", new Double(1.0));
			}
			person.createDesires("desires");
			person.getDesires().putActivityDuration("home", 8.0 * 3600.0);	
			person.getDesires().putActivityDuration("work", 8.0 * 3600.0);
			person.getDesires().putActivityDuration("leisure", 8.0 * 3600.0);
		}
		this.write(outPath);
	}
	
	private void write(String outPath) {
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(preferences);
		attributesWriter.writeFile(outPath + "/preferences.xml");
		
		ObjectAttributesXmlWriter incomesWriter = new ObjectAttributesXmlWriter(incomes);
		incomesWriter.writeFile(outPath + "/incomes.xml");
		
		new PopulationWriter(
				this.scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outPath + "/mon/plans.xml.gz");
	}
}
