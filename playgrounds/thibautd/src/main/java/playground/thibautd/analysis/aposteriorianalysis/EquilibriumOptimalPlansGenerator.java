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
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationWriter;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerModule;
import playground.thibautd.jointtripsoptimizer.replanning.selectors.PlanWithLongestTypeSelector;
import playground.thibautd.jointtripsoptimizer.run.config.CliquesConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
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
	private final JointReplanningConfigGroup configGroup;
	private final Config config;

	/**
	 * @param controler a controler, after it has been run. Parameters for the
	 * joint replanning module must correspond to the desired ones.
	 */
	public EquilibriumOptimalPlansGenerator(
			final JointControler controler) {
		this.controler = controler;
		this.config = controler.getConfig();
		this.configGroup = (JointReplanningConfigGroup)
			controler.getConfig().getModule( JointReplanningConfigGroup.GROUP_NAME );
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
		String file = directory+"/plans-with-all-joint-trips.xml.gz";
		String configFile = directory+"/untoggledConfig.xml.gz";
		log.info( "creating untoggled plans. Output to: "+file );
		writeUntoggledOptimalJointTrips( file );
		log.info( "writing corresponding config file to: "+configFile );
		writeConfigFile( configFile , file );

		file = directory+"/plans-with-best-joint-trips.xml.gz";
		configFile = directory+"/toggledConfig.xml.gz";
		log.info( "creating toggled plans. Output to: "+file );
		writeToggledOptimalJointTrips( file );
		log.info( "writing corresponding config file to: "+configFile );
		writeConfigFile( configFile , file );

		file = directory+"/plans-with-no-joint-trips.xml.gz";
		configFile = directory+"/individualConfig.xml.gz";
		log.info( "creating individual plans. Output to: "+file );
		writeIndividualTrips( file );
		log.info( "writing corresponding config file to: "+configFile );
		writeConfigFile( configFile , file );
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

	private void writeUntoggledOptimalJointTrips(final String file) {
		PlanWithLongestTypeSelector selector = new PlanWithLongestTypeSelector();
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
		}

		configGroup.setOptimizeToggle( "false" );
		optimiseSelectedPlans();
		writePopulation( file );
	}

	private void writeToggledOptimalJointTrips(final String file) {
		PlanWithLongestTypeSelector selector = new PlanWithLongestTypeSelector();
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		for (Clique clique : scenario.getCliques().getCliques().values()) {
			Plan plan = selector.selectPlan( clique );
			clique.setSelectedPlan( plan );
		}

		configGroup.setOptimizeToggle( "true" );
		optimiseSelectedPlans();
		writePopulation( file );
	}

	private void writeIndividualTrips(final String file) {
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		RemoveJointTrips.removeJointTrips( scenario.getPopulation() );

		optimiseSelectedPlans();
		writePopulation( file );

	}

	private void optimiseSelectedPlans() {
		JointPlanOptimizerModule module = new JointPlanOptimizerModule( controler );
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();

		module.prepareReplanning();
		for (Clique clique : scenario.getCliques().getCliques().values()) {
			module.handlePlan( clique.getSelectedPlan() );
		}
		module.finishReplanning();
	}

	private void writePopulation(final String file) {
		(new PopulationWriter(
				controler.getScenario().getPopulation(),
				controler.getScenario().getNetwork()) ).write( file );
	}
}

