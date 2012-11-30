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

/**
 * 
 */
package org.matsim.examples.simple;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.functions.CharyparNagelScoringUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.util.Assert;

/**
 * @author nagel
 *
 */
public class PtScoringTest {

		@Rule public MatsimTestUtils utils = new MatsimTestUtils();
		
		@Test
		public void test_PtScoring() {
			Config config = this.utils.loadConfig("test/scenarios/pt-simple/config.xml");
			
			config.planCalcScore().setWriteExperiencedPlans(true);
			
			config.otfVis().setDrawTransitFacilities(false) ;
			
			Controler controler = new Controler(config);
			controler.setOverwriteFiles(true);
			controler.setCreateGraphs(false);

			// this is here for debugging, but the class needs to be moved out of matsim first because
			// inside matsim it cannot connect to otfvis
//			controler.setMobsimFactory(new MobsimFactory(){
//				@Override
//				public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
//					QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
//					
//					OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
//					OTFClientLive.run(sc.getConfig(), server);
//					
//					return qSim ;
//				}}) ;


			controler.run();
			
			PlanCalcScoreConfigGroup pcs = config.planCalcScore() ;
			double typicalDuration_s = pcs.getActivityParams("home").getTypicalDuration() ;
			double priority = 1. ;
			
			double zeroUtilityDurationHome_s = CharyparNagelScoringUtils.computeZeroUtilityDuration(priority, typicalDuration_s);
			
			double timeTransitWalk = 18089.-18060. ;
			// (the pt interaction act takes 1sec)
			double timeTransitWait = 18302.-18090. ;
			double timeTransitInVeh = 18423. - 18302. ;
			// (the pt interaction act takes 1sec)
			double timeTransitWalk2 = 18447. - 18424. ;
			double timeHome = 18060. + 24.*3600 - 18447 ;
			
			double score = pcs.getTravelingWalk_utils_hr() * (timeTransitWalk/3600.) ;
			System.out.println("score: " + score ) ;

			// (pt interaction activity) 
			System.out.println("score: " + score ) ;
			
//			score += pcs.getMarginalUtlOfWaitingPt_utils_hr() * timeTransitWait/3600. ;
			score += pcs.getTravelingPt_utils_hr() * timeTransitWait/3600. ;
			// yyyy wait is not separately scored!!
			System.out.println("score: " + score ) ;
			
			score += pcs.getTravelingPt_utils_hr() * timeTransitInVeh/3600. ;
			System.out.println("score: " + score ) ;
			
			// (pt interaction activity) 
			System.out.println("score: " + score ) ;

			score += pcs.getTravelingWalk_utils_hr() * timeTransitWalk2/3600. ;
			System.out.println("score: " + score ) ;

			score += (pcs.getPerforming_utils_hr()/3600.) * typicalDuration_s * Math.log(timeHome/zeroUtilityDurationHome_s) ;
			System.out.println("score: " + score ) ;
			
			Scenario sc = controler.getScenario();
			Population pop = sc.getPopulation() ;
			for ( Person pp : pop.getPersons().values() ) {
				// (there is only one person, but we need to get it)
				
				System.out.println(" score: " + pp.getSelectedPlan().getScore() ) ;
				Assert.equals(89.85649384696622, pp.getSelectedPlan().getScore() ) ;
			}

		}
		
}
