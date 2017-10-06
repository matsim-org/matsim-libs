/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package tutorial.programming.facilitiesAndOpenTimes;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;

/**
 * @author nagel
 *
 */
public final class RunWithFacilitiesExample {

	// NOTE: facilities has been in the matsim main distribution for a long time.  But it never has been in the core, and thus was 
	// never maintained by the core team.  The following is an attempt to piece together what seems to be used in other places.
	// You need to try out yourself what works and what doesn't, e.g. if now all activities have to take place at facilities, if
	// there is a way to define "always open", etc. kai, feb'16

	private Scenario scenario;

	void run() { 
		final Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(0);
		{		
			ActivityParams params = new ActivityParams("home") ;
			params.setTypicalDuration(12.*3600);
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
			// note no opening/closing times for the activity type (given by facility)
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("shop") ;
			params.setTypicalDuration(1.*3600);
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
			// note no opening/closing times for the activity type (given by facility)
			config.planCalcScore().addActivityParams(params);
		}

		scenario = ScenarioUtils.createScenario( config ) ;

		Coord defaultCoord = new Coord(0.0, 0.0);

		// create network
		final Network network = scenario.getNetwork() ;
		final NetworkFactory nf = network.getFactory() ;

		Node node0 = nf.createNode( Id.createNodeId("0"), defaultCoord ) ;
		network.addNode(node0);

		Node node1 = nf.createNode( Id.createNodeId("1"), new Coord(100.,0.) ) ;
		network.addNode(node1);

		{
			Link link =nf.createLink(Id.createLinkId("0-1"), node0, node1 ) ;
			network.addLink(link);
		}
		{
			Link link =nf.createLink(Id.createLinkId("1-0"), node1, node0 ) ;
			network.addLink(link);
		}

		// create facilities, activities in it and open times
		final ActivityFacilities facilities = scenario.getActivityFacilities() ;
		final ActivityFacilitiesFactory ff = facilities.getFactory();

		ActivityFacility testFacility = ff.createActivityFacility(Id.create(0, ActivityFacility.class), defaultCoord) ;
		facilities.addActivityFacility(testFacility);
		{
			ActivityOption ao = ff.createActivityOption("home") ;
			testFacility.addActivityOption(ao);
			ao.addOpeningTime(new OpeningTimeImpl(0.0 * 3600, 9999.0 * 3600));
			// yy is there a setting for "always open"?  An interval until 24*3600 almost surely means that after 24 it is closed. kai, feb'16
		}
		{
			ActivityOption ao = ff.createActivityOption("shop") ;
			testFacility.addActivityOption(ao);
			ao.addOpeningTime(new OpeningTimeImpl(9.0 * 3600, 10.0 * 3600));
		}
		// create a person that has an activity at the facility:
		final Population population = scenario.getPopulation() ;
		final PopulationFactory pf =  population.getFactory();
		{
			Person person = pf.createPerson(Id.create(1, Person.class));
			population.addPerson(person);

			Plan plan = pf.createPlan() ;
			person.addPlan(plan);
			{		
				Activity act = pf.createActivityFromCoord("home", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
				act.setEndTime(9.0 * 3600);
			}
			{		
				Activity act = pf.createActivityFromCoord("shop", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
				act.setEndTime(10.0 * 3600);
			}
			{		
				Activity act = pf.createActivityFromCoord("home", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
			}
		}
		{
			Person person = pf.createPerson(Id.create(2, Person.class));
			population.addPerson(person);

			Plan plan = pf.createPlan() ;
			person.addPlan(plan);
			{		
				Activity act = pf.createActivityFromCoord("home", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
				act.setEndTime(10.0 * 3600); // i.e. arrive at shop when it closes
			}
			{		
				Activity act = pf.createActivityFromCoord("shop", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
				act.setEndTime(11.0 * 3600);
			}
			{		
				Activity act = pf.createActivityFromCoord("home", defaultCoord ) ;
				plan.addActivity(act);
				act.setFacilityId(testFacility.getId()); 
			}
		}
		// ---
		Controler controler = new Controler( scenario ) ;

		// make the controler react to facility open times:
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				CharyparNagelOpenTimesScoringFunctionFactory factory = new CharyparNagelOpenTimesScoringFunctionFactory(scenario) ;
				this.bindScoringFunctionFactory().toInstance( factory ) ;
			}
		});

		controler.run() ;
		
		// in the following, the second person has a lower score than the first because it spends its shopping activity while
		// the shop is closed.
		for ( Person pp : population.getPersons().values() ) {
			System.out.println( pp ) ;
			System.out.println( pp.getSelectedPlan() ) ;
		}
	}
	
	public static void main( String[] args ) {
		new RunWithFacilitiesExample().run();
	}

	public Scenario getScenario() {
		return this.scenario ;
	}

}
