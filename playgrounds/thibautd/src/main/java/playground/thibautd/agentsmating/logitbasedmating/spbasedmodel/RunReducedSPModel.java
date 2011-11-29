/* *********************************************************************** *
 * project: org.matsim.*
 * RunReducedSPModel.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.thibautd.agentsmating.logitbasedmating.basic.PlatformBasedModeChooserFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.PlatformBasedModeChooser;
import playground.thibautd.agentsmating.logitbasedmating.utils.SimpleLegTravelTimeEstimatorFactory;
import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithJointTripsWriterHandler;
import playground.thibautd.utils.MoreIOUtils;

/**
 * Executable class which runs the whole mating procedure.
 * <br>
 * <br>
 * Usage: <tt>RunReducedSPModel configFile outputDir</tt>
 *
 * <br>
 * <br>
 * The config must be valid for a run, as an iteration is
 * lanched to get travel times estimates.
 * The population must correspond to the <u>converged</u> state,
 * as departure and arrival times from the population are interpreted as
 * <i>desired</i> departure and arrival.
 * <br>
 * A {@link ReducedModelParametersConfigGroup} can be set as well, if the
 * model parameters are to be changed from the defaults.
 *
 * @author thibautd
 */
public class RunReducedSPModel {
	public static void main(final String[] args) {
		String configFileName = args[0];
		String outputPath = args[1];

		MoreIOUtils.initOut( outputPath );

		Config config = new Config();
		ReducedModelParametersConfigGroup configGroup =
			new ReducedModelParametersConfigGroup();
		config.addModule( ReducedModelParametersConfigGroup.NAME , configGroup );
		ConfigUtils.loadConfig( config , configFileName );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		// run a mobsim iteration to obtain travel time estimates
		consolidatePopulation( scenario );

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		Controler controler = new Controler( scenario );
		controler.run();

		ChoiceModel model = new ReducedSPModel(
				configGroup,
				scenario,
				new SimpleLegTravelTimeEstimatorFactory(
					PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
					PlanomatConfigGroup.RoutingCapability.fixedRoute,
					(PlansCalcRoute) controler.createRoutingAlgorithm(),
					scenario.getNetwork(),
					controler.getTravelTimeCalculator(),
					new DepartureDelayAverageCalculator(
								scenario.getNetwork(),
								controler.getConfig().travelTimeCalculator().getTraveltimeBinSize())),
				controler.getLeastCostPathCalculatorFactory().createPathCalculator(
					scenario.getNetwork(),
					controler.getTravelCostCalculatorFactory().createTravelCostCalculator(
						controler.getTravelTimeCalculator(),
						config.planCalcScore()),
					controler.getTravelTimeCalculator())
					);

		PlatformBasedModeChooser modeChooser =
			(new PlatformBasedModeChooserFactory()).createModeChooser(
				(ScenarioImpl) scenario,
				model);

		modeChooser.process();
		Map<Id, List<Id>> cliques = modeChooser.getCliques();

		PopulationWriter popWriter = new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork(),
				((ScenarioImpl) scenario).getKnowledges());
		popWriter.setWriterHandler(new PopulationWithJointTripsWriterHandler(
					scenario.getNetwork(),
					((ScenarioImpl) scenario).getKnowledges()));
		popWriter.write( outputPath+"/populationWithModeSet.xml.gz" );

		(new CliquesWriter(cliques)).writeFile( outputPath+"/cliques.xml.gz");
	}

	/**
	 * sets end times if only duration is set
	 * sets coord to the link coord if it is not set
	 */
	private static void consolidatePopulation(final Scenario scen) {
		Map<Id, ? extends Link> net = scen.getNetwork().getLinks();

		for (Person person : scen.getPopulation().getPersons().values()) {
			double now = 0;
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity act = (Activity) element;
					
					if (act.getEndTime() < 0) {
						now += act.getMaximumDuration();
						act.setEndTime( now );
					}
					else {
						now = act.getEndTime();
					}

					if (act.getCoord() == null) {
						((ActivityImpl) act).setCoord( net.get( act.getLinkId() ).getCoord() );
					}
				}
			}
		}
	}
}

