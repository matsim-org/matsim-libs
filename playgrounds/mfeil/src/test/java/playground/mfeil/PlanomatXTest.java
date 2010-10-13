/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.testcases.MatsimTestCase;

import playground.mfeil.FilesForTests.Initializer;


@Ignore("failing, unmaintained")
public class PlanomatXTest extends MatsimTestCase{

	private static final Logger log = Logger.getLogger(PlanomatXTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "1";
	private PlanomatX testee;
	private ScenarioImpl scenario_input;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		this.initializer = new Initializer();
		this.initializer.init(this);
		this.initializer.run();

		this.scenario_input = this.initializer.getControler().getScenario();

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario_input.getNetwork(), 900);

		LocationMutatorwChoiceSet locator = new LocationMutatorwChoiceSet (this.scenario_input.getNetwork(), this.initializer.getControler(), this.initializer.getControler().getScenario().getKnowledges());
		ActivityTypeFinder finder = new ActivityTypeFinder (this.initializer.getControler());

		this.testee = new PlanomatX (this.initializer.getControler(), locator, tDepDelayCalc, finder);
	}


	@Ignore("failing, unmaintained")
	public void testRun (){
		log.info("Running PlX testRun...");

		PlanomatXPlan plan1 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan1.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan2 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan2.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan3 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan3.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan4 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan4.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan5 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan5.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan6 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan6.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan7 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan7.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan8 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan8.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan9 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan9.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		PlanomatXPlan plan10 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan10.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());

		PlanomatXPlan [] neighbourhood = {plan1,plan2,plan3,plan4,plan5,plan6,plan7,plan8,plan9,plan10};
		int [][] infoOnNeighbourhood = new int [10][3];
		ArrayList<ActivityOptionImpl> primActs = new ArrayList<ActivityOptionImpl>();
		primActs.add(new ActivityOptionImpl("home", (ActivityFacilityImpl) this.scenario_input.getActivityFacilities().getFacilities().get(new IdImpl(1))));
		primActs.add(new ActivityOptionImpl("work", (ActivityFacilityImpl) this.scenario_input.getActivityFacilities().getFacilities().get(new IdImpl(10))));
		List<String> actTypes = new ArrayList<String>();
		actTypes.add("home");
		actTypes.add("work");
		actTypes.add("shopping");
		actTypes.add("leisure");

		/* Test of standard neighbourhood creation*/
		this.testee.createNeighbourhood(neighbourhood, infoOnNeighbourhood, actTypes, primActs);

		ArrayList<String[]> output= new ArrayList<String[]>();
		output.add(new String[]{"shopping","work",""});
		output.add(new String[]{"work","shopping",""});
		output.add(new String[]{"leisure","work","shopping"});
		output.add(new String[]{"work","leisure","shopping"});
		output.add(new String[]{"work","shopping","leisure"});
		output.add(new String[]{"work","",""});
		output.add(new String[]{"work","shopping",""});
		output.add(new String[]{"work","shopping",""});
		output.add(new String[]{"work","work",""});
		output.add(new String[]{"work","leisure",""});
		for (int i=0;i<neighbourhood.length;i++){
			for (int j=2;j<neighbourhood[i].getPlanElements().size()-2;j+=2){
				assertEquals(((ActivityImpl)(neighbourhood[i].getPlanElements().get(j))).getType(),output.get(i)[j/2-1]);
			}
		}

		/* Test of short plans neighbourhood creation*/
		PlanomatXPlan pla1 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla1.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla2 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla2.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla3 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla3.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla4 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla4.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla5 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla5.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla6 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla6.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla7 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla7.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla8 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla8.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla9 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla9.copyPlan(neighbourhood[5]);
		PlanomatXPlan pla10 = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		pla10.copyPlan(neighbourhood[5]);

		neighbourhood[0]=pla1;
		neighbourhood[1]=pla2;
		neighbourhood[2]=pla3;
		neighbourhood[3]=pla4;
		neighbourhood[4]=pla5;
		neighbourhood[5]=pla6;
		neighbourhood[6]=pla7;
		neighbourhood[7]=pla8;
		neighbourhood[8]=pla9;
		neighbourhood[9]=pla10;

		this.testee.createNeighbourhood(neighbourhood, infoOnNeighbourhood, actTypes, primActs);

		output= new ArrayList<String[]>();
		output.add(new String[]{"work"});
		output.add(new String[]{"work"});
		output.add(new String[]{"leisure","work"});
		output.add(new String[]{"work","shopping"});
		output.add(new String[]{"work","work"});
		output.add(new String[]{"work"});
		output.add(new String[]{"work"});
		output.add(new String[]{"work"});
		output.add(new String[]{"work"});
		output.add(new String[]{"work"});
		for (int i=0;i<neighbourhood.length;i++){
			for (int j=2;j<neighbourhood[i].getPlanElements().size()-2;j+=2){
				assertEquals(((ActivityImpl)(neighbourhood[i].getPlanElements().get(j))).getType(),output.get(i)[j/2-1]);
			}
		}

		log.info("done.");
	}
}
