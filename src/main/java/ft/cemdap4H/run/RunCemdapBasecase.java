/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package ft.cemdap4H.run;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.ev.temperature.TemperatureChangeConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

//import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

//import ch.sbb.matsim.routing.pt.raptor.*;


/**
 * @author  jbischoff
 *
 */

public class RunCemdapBasecase {
public static void main(String[] args) {
	String runId = "vw280";
	String pct = "_0.1";
	
	String configPath = "D:\\Matsim\\Axer\\Hannover\\Base\\vw243_cadON_ptSpeedAdj.0.1\\config_0.1_changeTimes_3.xml"; 
		
	Config config = ConfigUtils.loadConfig(configPath, new CadytsConfigGroup());
	
	
	config.plans().setInputFile("D:\\\\Matsim\\\\Axer\\\\Hannover\\\\ZIM\\\\input\\\\plans\\\\finishedPlans_0.1_timeFIX_License.xml.gz");
	config.network().setInputFile("D:\\Matsim\\Axer\\Hannover\\Base\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_network.xml.gz");
	config.transit().setTransitScheduleFile("D:\\Matsim\\Axer\\Hannover\\Base\\vw275_cadytsOn_NoMuWo_Mu3600_TimeAdj_Lic0.1_2\\vw275_cadytsOn_NoMuWo_Mu3600_TimeAdj_Lic0.1_2.output_transitSchedule.xml.gz");
	config.transit().setVehiclesFile("D:\\Matsim\\Axer\\Hannover\\Base\\vw275_cadytsOn_NoMuWo_Mu3600_TimeAdj_Lic0.1_2\\vw275_cadytsOn_NoMuWo_Mu3600_TimeAdj_Lic0.1_2.output_transitVehicles.xml.gz");
//	config.qsim().setFlowCapFactor(1.15);
	config.global().setNumberOfThreads(32);
	config.parallelEventHandling().setNumberOfThreads(32);
	config.qsim().setNumberOfThreads(32);
	config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);



	
	Scenario scenario = ScenarioUtils.loadScenario(config);
	adjustPtNetworkCapacity(scenario.getNetwork(),config.qsim().getFlowCapFactor());
	
	Controler controler = new Controler(scenario);
	controler.addOverridingModule(new CadytsCarModule());
	
//	//Override cadyts params
	CadytsConfigGroup ccg = (CadytsConfigGroup) config.getModules().get(CadytsConfigGroup.GROUP_NAME);
	ccg.setStartTime(0);	
	ccg.setEndTime(24*3600);
	
	//Override Counts params
	CountsConfigGroup countsccg = (CountsConfigGroup) config.getModules().get(CountsConfigGroup.GROUP_NAME);
	countsccg.setInputFile("D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\network\\counts_H_LSA.xml");
	
	
	//RECREATE ACTIVITY PARAMS 
	{
	config.planCalcScore().getActivityParams().clear();
	// activities:
	for ( long ii = 1 ; ii <= 30; ii+=1 ) {

				config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii ).setTypicalDuration( ii*3600 ) );

				config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );

				config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );

				config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii).setTypicalDuration( ii*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );

				config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii ).setTypicalDuration( ii*3600 ) );

	}
	
	config.planCalcScore().addActivityParams( new ActivityParams( "home" ).setTypicalDuration( 14*3600 ) );
	config.planCalcScore().addActivityParams( new ActivityParams( "work").setTypicalDuration( 8*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
	config.planCalcScore().addActivityParams( new ActivityParams( "leisure" ).setTypicalDuration( 1*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
	config.planCalcScore().addActivityParams( new ActivityParams( "shopping").setTypicalDuration( 1*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );
	config.planCalcScore().addActivityParams( new ActivityParams( "other" ).setTypicalDuration( 1*3600 ) );
	config.planCalcScore().addActivityParams( new ActivityParams( "education").setTypicalDuration(8*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(18. * 3600. ) );
	}

			
	
	
	config.controler().setOutputDirectory("D:\\Matsim\\Axer\\Hannover\\Base\\"+runId+pct);
	config.controler().setRunId(runId+pct);
	config.controler().setWritePlansInterval(10);
	config.controler().setWriteEventsInterval(10);
	config.controler().setLastIteration(500); //Number of simulation iterations
	config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks );
	config.plansCalcRoute().setRoutingRandomness( 3. );

	
	
	config.strategy().setFractionOfIterationsToDisableInnovation(0.6); //Fraction to disable Innovation

	
	
	// vsp defaults
	config.qsim().setUsingTravelTimeCheckInTeleportation( true );
	config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
//	config.plansCalcRoute().setInsertingAccessEgressWalk( true );
	
	
	// tell the system to use the congested car router for the ride mode:
	controler.addOverridingModule(new AbstractModule(){
		@Override public void install() {
			addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
			addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());		}
	});
	
	controler.addOverridingModule(new AbstractModule() {
	    @Override
	    public void install() {
	        install(new SwissRailRaptorModule());
//	        bind(RaptorRouteSelector.class).to(MyCustomRouteSelector.class);
	    }
	});
	
	
	// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
	controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
		private final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( scenario );
		@Inject CadytsContext cContext;
		@Override
		public ScoringFunction createNewScoringFunction(Person person) {

			final ScoringParameters params = parameters.getScoringParameters( person );
			
			SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

			final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
			final double cadytsScoringWeight = 10. * config.planCalcScore().getBrainExpBeta();
			
			scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
			scoringFunctionAccumulator.addScoringFunction(scoringFunction );

			return scoringFunctionAccumulator;
		}
	}) ;
		
	boolean deleteRoutes = false;

	if (deleteRoutes) {
		controler.getScenario().getPopulation().getPersons().values().stream().flatMap(p -> p.getPlans().stream())
				.flatMap(pl -> pl.getPlanElements().stream()).filter(Leg.class::isInstance)
				.forEach(pe -> ((Leg) pe).setRoute(null));
	}
	
	controler.run();
	
	
}
/**
 * this is useful for pt links when only a fraction of the population is simulated, but bus frequency remains the same. 
 * Otherwise, pt vehicles may get stuck.
 */
	private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor){
		if (flowCapacityFactor<1.0){
			for (Link l : network.getLinks().values()){
				if (l.getAllowedModes().contains(TransportMode.pt)){
					l.setCapacity(l.getCapacity()/flowCapacityFactor);
				}
			}
		}
	}
}
