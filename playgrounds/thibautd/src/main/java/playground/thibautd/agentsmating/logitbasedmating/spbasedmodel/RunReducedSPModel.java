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

import herbie.running.config.HerbieConfigGroup;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.thibautd.agentsmating.logitbasedmating.basic.PlatformBasedModeChooserFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.PlatformBasedModeChooser;
import playground.thibautd.agentsmating.logitbasedmating.utils.SimpleLegTravelTimeEstimatorFactory;
import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtrips.population.PopulationWithJointTripsWriterHandler;
import playground.thibautd.utils.MoreIOUtils;
import playground.thibautd.utils.TransitActRemoverCorrectTravelTime;

/**
 * Executable class which runs the whole mating procedure.
 * <br>
 * <br>
 * Usage: <tt>RunReducedSPModel configFile outputDir [controler]</tt>
 *
 * <br>
 * <br>
 * The config must be valid for a run, as an iteration is
 * lanched to get travel times estimates. File in the output directory
 * will be overriden.
 * The population must correspond to the <u>converged</u> state,
 * as departure and arrival times from the population are interpreted as
 * <i>desired</i> departure and arrival.
 * <br>
 * A {@link ReducedModelParametersConfigGroup} can be set as well, if the
 * model parameters are to be changed from the defaults.
 * <br><br>
 * 
 * The optional <tt>controler</tt> parameter can take the values "core" (default) or
 * "herbie".
 *
 * @author thibautd
 */
public class RunReducedSPModel {
	private static final Logger log =
		Logger.getLogger(RunReducedSPModel.class);

	private static enum ControlerType {
		CORE, HERBIE};

	public static void main(final String[] args) {
		try {
			exec( args );
		}
		catch (Exception e) {
			log.error( "got an exception: ", e );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	private static void exec(final String[] args) {
		String configFileName = args[0];
		String outputPath = args[1];
		ControlerType controlerType = ControlerType.CORE;

		if (args.length > 2) {
			if ( args[ 2 ].toLowerCase().equals( "core" ) ) {
				controlerType = ControlerType.CORE;
			}
			if ( args[ 2 ].toLowerCase().equals( "herbie" ) ) {
				controlerType = ControlerType.HERBIE;
			}
			else throw new IllegalArgumentException( "unknown controler "+args[ 2 ] );
		}

		MoreIOUtils.initOut( outputPath );

		// //////////////////////// load data...
		Config config = new Config();
		ReducedModelParametersConfigGroup configGroup =
			new ReducedModelParametersConfigGroup();

		config.addModule( ReducedModelParametersConfigGroup.NAME , configGroup );

		switch (controlerType) {
			case HERBIE:
				config.addModule(
						HerbieConfigGroup.GROUP_NAME,
						new  HerbieConfigGroup() );
				break;
		}

		ConfigUtils.loadConfig( config , configFileName );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		consolidatePopulation( scenario );
		// //////////////////////// load data... DONE

		// //////////////////////// run an iteration...
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		Controler controler;
		
		switch (controlerType) {
			case CORE:
				controler = new Controler( scenario );
				break;
			case HERBIE:
				controler = new CustomHerbieControler( scenario );
				break;
			default:
				throw new RuntimeException( "got unexisting controler type "+controlerType+" !?" );
		}

		// XXX: Dirty hack, aiming at avoiding exceptions while
		// scoring in the herbie controler. This is not a problem,
		// as the MATSim score is not used anywhere; but this is
		// ugly, and should be removed as soon as the scoring function
		// works again.
		controler.setScoringFunctionFactory( new FakeScoringFunctionFactory() );
		controler.setOverwriteFiles( true );
		controler.run();
		// //////////////////////// run an iteration... DONE

		// //////////////////////// run the affectation...
		ReducedSPModel model = new ReducedSPModel(
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

		modeChooser.addPlanAcceptor( new NoFreightPlanAcceptor() );
		modeChooser.addPreChoiceAlgo( new TransitActRemoverCorrectTravelTime() );
		modeChooser.process();
		Map<Id, List<Id>> cliques = modeChooser.getCliques();

		model.notifyAffectationProcedureEnd();
		// //////////////////////// run the affectation... DONE

		// //////////////////////// output files.
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

class FakeScoringFunctionFactory implements ScoringFunctionFactory {

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		return new ScoringFunctionAccumulator();
	}
}
