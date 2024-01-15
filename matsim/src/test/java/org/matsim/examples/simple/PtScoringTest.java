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

package org.matsim.examples.simple;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

/**
 * @author nagel
 *
 */

public class PtScoringTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void test_PtScoringLineswitch(TypicalDurationScoreComputation typicalDurationScoreComputation) {
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch"), "config.xml"));
		ScoringConfigGroup pcs = config.scoring() ;

		if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams params : pcs.getActivityParams()){
				params.setTypicalDurationScoreComputation(typicalDurationScoreComputation);
			}
		}

		pcs.setWriteExperiencedPlans(true);

		Controler controler = new Controler(config);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);

        EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();

		for (Event event : allEvents) {
			System.out.println(event.toString());
		}



		double typicalDuration_s = pcs.getActivityParams("home").getTypicalDuration().seconds();
		double priority = 1. ;

//		double zeroUtilityDurationHome_s = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration_s);
		ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder( pcs.getActivityParams("home") ) ;
		ActivityUtilityParameters params = builder.build() ;
		double zeroUtilityDurationHome_s = params.getZeroUtilityDuration_h() * 3600. ;


		double homeAct1End = 18060. ;
		double stop1Arr = 18076. ;
		double ptIA1ActEnd = stop1Arr ;
		double enterVeh = 18315. ;
		double leaveVeh = 18423. ;
		double ptIA2ActEnd = leaveVeh ;
		double home2Arr = 18439 ;

		double homeAct2End = 18900. ;
		double stop2Arr = 18918. ;
		double ptIA3ActEnd = stop2Arr ;
		double enterVeh2 = 19216. ;
		double leaveVeh2 = 19243. ;
		double ptIA4ActEnd = leaveVeh2 ;
		double stop3Arr = 19275. ;
		double ptIA5ActEnd = stop3Arr ;
		double enterVeh3 = 19816 ;
		double leaveVeh3 = 19843.;
		double ptIA6ActEnd = leaveVeh3 ;
		double home3Arr = 19866 ;

		double score = pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop1Arr-homeAct1End)/3600. ;
		System.out.println("score after walk: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt interact: " + score ) ;

		// yyyy wait is not separately scored!!
		//			score += pcs.getMarginalUtlOfWaitingPt_utils_hr() * timeTransitWait/3600. ;
		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh-ptIA1ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh-enterVeh)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt interact: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (home2Arr-ptIA2ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		final double duration = homeAct2End-home2Arr;
		double tmpScore = (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s
				* Math.log(duration/zeroUtilityDurationHome_s) ;
		if ( tmpScore < 0 ) {
			System.out.println("home2score< 0; replacing ... ") ;
			double slope = (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s / zeroUtilityDurationHome_s ;
			tmpScore = zeroUtilityDurationHome_s * slope ;
		}
		score += tmpScore ;
		System.out.println("score after home act: " + score ) ;

		// ======

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop2Arr-homeAct2End)/3600. ;
		System.out.println("score after walk: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh2-ptIA3ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh2-enterVeh2)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop3Arr-ptIA4ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		score += pcs.getUtilityOfLineSwitch() ;
		System.out.println("score after line switch: " + score ) ;

		// ------

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh3-ptIA5ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh3-enterVeh3)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (home3Arr-ptIA6ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		score += (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s
				* Math.log((homeAct1End-home3Arr+24.*3600)/zeroUtilityDurationHome_s) ;
		System.out.println("score after home act: " + score ) ;

		Scenario sc = controler.getScenario();
		Population pop = sc.getPopulation() ;
		for ( Person pp : pop.getPersons().values() ) {
			// (there is only one person, but we need to get it)

			System.out.println(" score: " + pp.getSelectedPlan().getScore() ) ;

			if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
				Assertions.assertEquals(-21.280962467387187, pp.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON ) ;
			}
			else{
				Assertions.assertEquals(27.468448990195423, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}

		}

	}

	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void test_PtScoringLineswitchAndPtConstant(TypicalDurationScoreComputation typicalDurationScoreComputation) {
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch"), "config.xml"));
		ScoringConfigGroup pcs = config.scoring() ;

		if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform))
			for(ActivityParams params : pcs.getActivityParams()){
				params.setTypicalDurationScoreComputation(typicalDurationScoreComputation);
		}

		pcs.setWriteExperiencedPlans(true);
		pcs.getModes().get(TransportMode.pt).setConstant(1.);

		Controler controler = new Controler(config);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);

        EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();

		for (Event event : allEvents) {
			System.out.println(event.toString());
		}

		double typicalDuration_s = pcs.getActivityParams("home").getTypicalDuration().seconds();
		double priority = 1. ;

//		double zeroUtilityDurationHome_s = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration_s);
		ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder( pcs.getActivityParams("home") ) ;
		ActivityUtilityParameters params = builder.build() ;
		double zeroUtilityDurationHome_s = params.getZeroUtilityDuration_h() * 3600. ;

		double homeAct1End = 18060. ;
		double stop1Arr = 18076. ;
		double ptIA1ActEnd = stop1Arr ;
		double enterVeh = 18316. ;
		double leaveVeh = 18423. ;
		double ptIA2ActEnd = leaveVeh ;
		double home2Arr = 18439 ;

		double homeAct2End = 18900. ;
		double stop2Arr = 18918. ;
		double ptIA3ActEnd = stop2Arr ;
		double enterVeh2 = 19216. ;
		double leaveVeh2 = 19243. ;
		double ptIA4ActEnd = leaveVeh2 ;
		double stop3Arr = 19275. ;
		double ptIA5ActEnd = stop3Arr ;
		double enterVeh3 = 19816 ;
		double leaveVeh3 = 19843.;
		double ptIA6ActEnd = leaveVeh3 ;
		double home3Arr = 19866 ;

		double score = pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop1Arr-homeAct1End)/3600. ;
		System.out.println("score after walk: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt interact: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getConstant();
		System.out.println("score after addition of pt constant: " + score ) ;

		// yyyy wait is not separately scored!!
		//			score += pcs.getMarginalUtlOfWaitingPt_utils_hr() * timeTransitWait/3600. ;
		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh-ptIA1ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh-enterVeh)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt interact: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (home2Arr-ptIA2ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		double tmpScore = (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s
				* Math.log((homeAct2End-home2Arr)/zeroUtilityDurationHome_s) ;
		if ( tmpScore < 0 ) {
			System.out.println("home2score< 0; replacing ... ") ;
			double slope = (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s / zeroUtilityDurationHome_s ;
			tmpScore = zeroUtilityDurationHome_s * slope ;
		}
		score += tmpScore ;
		System.out.println("score after home act: " + score ) ;

		// ======

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop2Arr-homeAct2End)/3600. ;
		System.out.println("score after walk: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getConstant();
		System.out.println("score after addition of pt constant: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh2-ptIA3ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh2-enterVeh2)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (stop3Arr-ptIA4ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		score += pcs.getUtilityOfLineSwitch() ;
		System.out.println("score after line switch: " + score ) ;

		// ------

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (enterVeh3-ptIA5ActEnd)/3600. ;
		System.out.println("score after wait: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (leaveVeh3-enterVeh3)/3600. ;
		System.out.println("score after travel pt: " + score ) ;

		// (pt interaction activity)
		System.out.println("score after pt int act: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (home3Arr-ptIA6ActEnd)/3600. ;
		System.out.println("score after walk: " + score ) ;

		score += (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s
				* Math.log((homeAct1End-home3Arr+24.*3600)/zeroUtilityDurationHome_s) ;
		System.out.println("score after home act: " + score ) ;

		Scenario sc = controler.getScenario();
		Population pop = sc.getPopulation() ;
		for ( Person pp : pop.getPersons().values() ) {
			// (there is only one person, but we need to get it)

			System.out.println(" score: " + pp.getSelectedPlan().getScore() ) ;

			if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
//				Assert.assertEquals(89.14608279715044, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
				Assertions.assertEquals(-19.280962467387187, pp.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON ) ;
			}
			else{
				Assertions.assertEquals(29.468448990195423, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}

		}

	}

	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void test_PtScoring_Wait(TypicalDurationScoreComputation typicalDurationScoreComputation) {
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-simple"), "config.xml"));
		ScoringConfigGroup pcs = config.scoring();

		if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams params : pcs.getActivityParams()){
				params.setTypicalDurationScoreComputation(typicalDurationScoreComputation);
			}
		}

		pcs.setWriteExperiencedPlans(true);
		pcs.setMarginalUtlOfWaitingPt_utils_hr(-18.0) ;

		Controler controler = new Controler(config);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);

        EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();

		for (Event event : allEvents) {
			System.out.println(event.toString());
		}

		double typicalDuration_s = pcs.getActivityParams("home").getTypicalDuration().seconds();
		double priority = 1. ;

//		double zeroUtilityDurationHome_s = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration_s);
		ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder( pcs.getActivityParams("home") ) ;
		ActivityUtilityParameters params = builder.build() ;
		double zeroUtilityDurationHome_s = params.getZeroUtilityDuration_h() * 3600. ;


		double timeTransitWalk = 18089.-18060. ;
		double timeTransitWait = 18302.-18089. ;
		double timeTransitInVeh = 18423. - 18302. ;
		double timeTransitWalk2 = 18446. - 18423. ;
		double timeHome = 18060. + 24.*3600 - 18446 ;

		double score = pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (timeTransitWalk/3600.) ;
		System.out.println("score: " + score ) ;

		// (pt interaction activity)
		System.out.println("score: " + score ) ;

		System.out.println("marginalUtlOfWaitPt: " + pcs.getMarginalUtlOfWaitingPt_utils_hr() ) ;

		score += pcs.getMarginalUtlOfWaitingPt_utils_hr() * timeTransitWait/3600. ;
		//			score += pcs.getTravelingPt_utils_hr() * timeTransitWait/3600. ;
		// yyyy wait is not separately scored!!
		System.out.println("score: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * timeTransitInVeh/3600. ;
		System.out.println("score: " + score ) ;

		// (pt interaction activity)
		System.out.println("score: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * timeTransitWalk2/3600. ;
		System.out.println("score: " + score ) ;

		score += (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s * Math.log(timeHome/zeroUtilityDurationHome_s) ;
		System.out.println("final score: " + score ) ;

		Scenario sc = controler.getScenario();
		Population pop = sc.getPopulation() ;
		for ( Person pp : pop.getPersons().values() ) {
			// (there is only one person, but we need to get it)

			System.out.println("agent score: " + pp.getSelectedPlan().getScore() ) ;

			if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
				Assertions.assertEquals(89.13108279715044, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}
			else{
				Assertions.assertEquals(137.1310827971504, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}
		}

	}

	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void test_PtScoring(TypicalDurationScoreComputation typicalDurationScoreComputation) {
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-simple"), "config.xml"));
		ScoringConfigGroup pcs = config.scoring() ;

		if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform))
		for(ActivityParams params : pcs.getActivityParams()){
			params.setTypicalDurationScoreComputation(typicalDurationScoreComputation);
		}

		pcs.setWriteExperiencedPlans(true);

		Controler controler = new Controler(config);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);

        EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();

		for (Event event : allEvents) {
			System.out.println(event.toString());
		}

		double typicalDuration_s = pcs.getActivityParams("home").getTypicalDuration().seconds();
		double priority = 1. ;

//		double zeroUtilityDurationHome_s = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration_s);
		ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder( pcs.getActivityParams("home") ) ;
		ActivityUtilityParameters params = builder.build() ;
		double zeroUtilityDurationHome_s = params.getZeroUtilityDuration_h() * 3600. ;


		double timeTransitWalk = 18089.-18060. ;
		double timeTransitWait = 18302.-18089. ;
		double timeTransitInVeh = 18423. - 18302. ;
		double timeTransitWalk2 = 18446. - 18423. ;
		double timeHome = 18060. + 24.*3600 - 18446 ;

		double score = pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (timeTransitWalk/3600.) ;
		System.out.println("score: " + score ) ;

		// (pt interaction activity)
		System.out.println("score: " + score ) ;

		//			score += pcs.getMarginalUtlOfWaitingPt_utils_hr() * timeTransitWait/3600. ;
		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * timeTransitWait/3600. ;
		// yyyy wait is not separately scored!!
		System.out.println("score: " + score ) ;

		score += pcs.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * timeTransitInVeh/3600. ;
		System.out.println("score: " + score ) ;

		// (pt interaction activity)
		System.out.println("score: " + score ) ;

		score += pcs.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * timeTransitWalk2/3600. ;
		System.out.println("score: " + score ) ;

		score += (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s * Math.log(timeHome/zeroUtilityDurationHome_s) ;
		System.out.println("score: " + score ) ;

		Scenario sc = controler.getScenario();
		Population pop = sc.getPopulation() ;
		for ( Person pp : pop.getPersons().values() ) {
			// (there is only one person, but we need to get it)

			System.out.println(" score: " + pp.getSelectedPlan().getScore() ) ;

			if(typicalDurationScoreComputation.equals(TypicalDurationScoreComputation.uniform)){
				Assertions.assertEquals(89.87441613048377, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}
			else{
				Assertions.assertEquals(137.87441613048375, pp.getSelectedPlan().getScore(),MatsimTestUtils.EPSILON ) ;
			}


		}

	}

}
