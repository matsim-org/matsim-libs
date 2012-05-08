/* *********************************************************************** *
 * project: org.matsim.*
 * EquilibriumOptimalPlansGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scoring.ScoringFunction;

import playground.thibautd.jointtrips.config.CliquesConfigGroup;
import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.ScenarioWithCliques;
import playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;
import playground.thibautd.jointtrips.run.JointControler;
import playground.thibautd.utils.RemoveJointTrips;

/**
 * Class which optimises the plan of all cliques of more than 2 members
 * against the traffic state at the equilibrium.
 *
 * This allows to generate plans files with the "potential optimal plans" for
 * each clique, with different parameters (with and without joint trips, for example).
 *
 * This way, different kind of plans can be generated and compared, without the conceptual
 * difficulty arising from the non-uniqueness of the equilibrium state.
 *
 * @author thibautd
 */
public class EquilibriumOptimalPlansGenerator {
	private static final Logger log =
		Logger.getLogger(EquilibriumOptimalPlansGenerator.class);

	private final JointControler controler;
	// private final JointReplanningConfigGroup configGroup;
	private final Config config;

	/**
	 * @param controler a controler, after it has been run. Parameters for the
	 * joint replanning module must correspond to the desired ones.
	 */
	public EquilibriumOptimalPlansGenerator(
			final JointControler controler) {
		this.controler = controler;
		this.config = controler.getConfig();
		//this.configGroup = (JointReplanningConfigGroup)
		//	controler.getConfig().getModule( JointReplanningConfigGroup.GROUP_NAME );
	}

	/**
	 * generates and writes plan files with plans optimal according to
	 * the last state of traffic.
	 * Three plans files are generated: with all joint trips enforced, with
	 * toggle otimised, and without joint trips.
	 * Config files containing the information to load the related scenario are
	 * also produced.
	 *
	 * @param directory the outptut directory
	 */
	public void writePopulations(final String directory) {
		pruneSingletons();

		String file = directory+"/plans-with-equilibrium-joint-trips.xml.gz";
		String configFile = directory+"/equilibriumConfig.xml.gz";
		log.info( "creating equilibrium plans. Output to: "+file );
		writeOptimalPlanWithJointTrips( file );
		//writeUntoggledOptimalJointTrips( file );
		log.info( "writing corresponding config file to: "+configFile );
		writeConfigFile( configFile , file );

		file = directory+"/plans-with-no-joint-trips.xml.gz";
		configFile = directory+"/individualConfig.xml.gz";
		log.info( "creating individual plans. Output to: "+file );
		writeIndividualTrips( file );
		log.info( "writing corresponding config file to: "+configFile );
		writeConfigFile( configFile , file );

		file = directory+"/plans-with-all-joint-trips.xml.gz";
		configFile = directory+"/allConfig.xml.gz";
		if (writeUntoggledJointTrips( file )) {
			log.info( "created complete plans. Output to: "+file );
			log.info( "writing corresponding config file to: "+configFile );
			writeConfigFile( configFile , file );
		}
	}

	private void pruneSingletons() {
		Iterator<? extends Clique> iterator =
			((ScenarioWithCliques) controler.getScenario()).getCliques().getCliques().values().iterator();

		int count = 0;
		int tot = 0;
		log.info( "removing mono-agent cliques before the analysis" );

		while ( iterator.hasNext() ) {
			tot++;
			if ( iterator.next().getMembers().size() <= 1 ) {
				iterator.remove();
				count++;
			}
		}

		log.info( tot+" cliques examined" );
		log.info( count+" cliques removed" );
	}

	private void writeConfigFile(
			final String configFile,
			final String plansFile) {
		Config newConfig = new Config();

		PlansConfigGroup plans = new PlansConfigGroup();
		plans.setInputFile( plansFile );
		plans.setNetworkRouteType( config.plans().getNetworkRouteType() );

		newConfig.addModule(
				CliquesConfigGroup.GROUP_NAME,
				config.getModule( CliquesConfigGroup.GROUP_NAME ) );
		newConfig.addModule(
				NetworkConfigGroup.GROUP_NAME,
				config.network());
		newConfig.addModule(
				FacilitiesConfigGroup.GROUP_NAME,
				config.facilities());
		newConfig.addModule(
				PlansConfigGroup.GROUP_NAME,
				plans);
		// to remember the replanning parameters
		newConfig.addModule(
				JointReplanningConfigGroup.GROUP_NAME,
				config.getModule( JointReplanningConfigGroup.GROUP_NAME ));
		newConfig.addModule(
				PlanCalcScoreConfigGroup.GROUP_NAME,
				config.planCalcScore());

		(new ConfigWriter( newConfig )).write( configFile );
	}

	private void writeOptimalPlanWithJointTrips(final String file) {
		//PlanWithLongestTypeSelector selector = new PlanWithLongestTypeSelector();
		PlanSelector selector = new KeepSelected();
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		for (Clique clique : scenario.getCliques().getCliques().values()) {
			Plan plan = selector.selectPlan( clique );
			clique.setSelectedPlan( plan );

			List<Plan> unselectedPlans = new ArrayList<Plan>( clique.getPlans() );
			unselectedPlans.remove( plan );

			for ( Plan currentPlan : unselectedPlans ) {
				if (currentPlan != plan) {
					clique.removePlan( currentPlan );
				}
			}
			if (clique.getPlans().size() > 1) throw new RuntimeException( clique.getPlans().size()+"" );
		}

		//configGroup.setOptimizeToggle( "false" );
		optimiseSelectedPlans();
		writePopulation( file );
	}


	private boolean writeUntoggledJointTrips(final String file) {
		//ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		//for (Clique clique : scenario.getCliques().getCliques().values()) {
		//	JointPlan plan = (JointPlan) clique.getSelectedPlan();

		//	if (plan.getJointTripPossibilities() == null) {
		//		return false;
		//	}

		//	JointTripPossibilitiesUtils.includeAllJointTrips( plan );
		//}

		////configGroup.setOptimizeToggle( "false" );
		//optimiseSelectedPlans();
		//writePopulation( file );
		//return true;
		return false;
	}

	//private void writeToggledOptimalJointTrips(final String file) {
	//	//PlanSelector selector = new PlanWithLongestTypeSelector();
	//	//ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

	//	//for (Clique clique : scenario.getCliques().getCliques().values()) {
	//	//	Plan plan = selector.selectPlan( clique );
	//	//	clique.setSelectedPlan( plan );
	//	//}

	//	configGroup.setOptimizeToggle( "true" );
	//	optimiseSelectedPlans();
	//	writePopulation( file );
	//}

	private void writeIndividualTrips(final String file) {
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		RemoveJointTrips.removeJointTrips( scenario.getPopulation() );

		optimiseSelectedPlans();
		writePopulation( file );
	}

	private void optimiseSelectedPlans() {
		// JointPlanOptimizerModule module = new JointPlanOptimizerModule( controler );
		JointTimeModeChooserModule module = new JointTimeModeChooserModule( controler );
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		module.prepareReplanning();
		for (Clique clique : scenario.getCliques().getCliques().values()) {
			module.handlePlan( clique.getSelectedPlan() );

			for (Plan plan : ((JointPlan) clique.getSelectedPlan()).getIndividualPlans().values()) {
				ScoringFunction scoringFunction = controler.getScoringFunctionFactory().createNewScoringFunction( plan );
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						scoringFunction.handleActivity( (Activity) pe );
					}
					if (pe instanceof Leg) {
						scoringFunction.handleLeg( (Leg) pe );
					}
				}
				scoringFunction.finish();
				plan.setScore( scoringFunction.getScore() );
			}
		}
		module.finishReplanning();
	}

	private void writePopulation(final String file) {
		(new PopulationWriter(
				controler.getScenario().getPopulation(),
				controler.getScenario().getNetwork()) ).write( file );
	}
}

