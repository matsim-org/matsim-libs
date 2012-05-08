/* *********************************************************************** *
 * project: org.matsim.*
 * JointControlerUtils.java
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
package playground.thibautd.jointtrips.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

import playground.thibautd.jointtrips.config.CliquesConfigGroup;
import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.jointtrips.config.JointTripsMutatorConfigGroup;
import playground.thibautd.jointtrips.population.CliquesXmlReader;
import playground.thibautd.jointtrips.population.DriverRoute;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.PassengerRoute;
import playground.thibautd.jointtrips.population.PopulationWithJointTripsReader;
import playground.thibautd.jointtrips.population.ScenarioWithCliques;
import playground.thibautd.jointtrips.run.JointControler;
import playground.thibautd.scoring.CarPoolingLegScoringFunction;
import playground.thibautd.scoring.KtiLikeActivitiesScoringFunctionFactory;

/**
 * Helper class to create a fully configured {@link JointControler} from a config file,
 * or to only load the {@link Config} or the {@link ScenarioWithCliques}
 *
 * @author thibautd
 */
public class JointControlerUtils {
	private JointControlerUtils() {}

	/**
	 * Creates a {@link JointControler} instance from a configFile.
	 * Some of the loading methods are workarounds, and may not work with all
	 * scenarios. Particularly, "special" settings as households, transit, vehicles,
	 * lanes or signal systems are not handled.
	 *
	 * @return a JointControler instance, ready for running.
	 */
	public static Controler createControler(final String configFile) {
		Config config = createConfig(configFile);

		ScenarioWithCliques scenario = createScenario(config);

		Controler controler = new JointControler(scenario);
		setScoringFunction(controler);

		return controler;
	}

	/**
	 * binds to createScenario(createConfig(configFile));
	 *
	 * @param configFile the path to the configFile
	 * @return a ready to use scenario
	 */
	public static ScenarioWithCliques createScenario(final String configFile) {
		return createScenario(createConfig(configFile));
	}

	/**
	 * @param config a loaded config
	 * @return a ready to use scenario
	 */
	public static ScenarioWithCliques createScenario(final Config config) {
		ScenarioWithCliques scenario = new ScenarioWithCliques(config);
		tuneScenario( scenario );

		//(new ScenarioLoaderImpl(scenario)).loadScenario();
		// ScenarioUtils.loadScenario(scenario);
		// Cannot load full joint information when using default loader: load manually.
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		loader.loadActivityFacilities();
		// TODO: adapt to new state
		(new PopulationWithJointTripsReader(scenario)).readFile(config.plans().getInputFile());

		try {
			new CliquesXmlReader(scenario).parse();
		} catch (Exception e) {
			throw new RuntimeException("Problem while importing clique information", e);
		}

		return scenario;
	}

	private static void tuneScenario(final Scenario sc) {
		ModeRouteFactory rFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory();
		rFactory.setRouteFactory(
				JointActingTypes.DRIVER,
				new RouteFactory() {
					@Override
					public Route createRoute(
						final Id s,
						final Id e) {
						return new DriverRoute( s , e );
					}
				});
		rFactory.setRouteFactory(
				JointActingTypes.PASSENGER,
				new RouteFactory() {
					@Override
					public Route createRoute(
						final Id s,
						final Id e) {
						return new PassengerRoute( s , e );
					}
				});
	}

	/**
	 * @param configFile the path to the config file
	 * @return a loaded config, including proper setting of joint trips specific groups
	 */
	public static Config createConfig(final String configFile) {
		JointReplanningConfigGroup jointConfigGroup = new JointReplanningConfigGroup();
		CliquesConfigGroup cliquesConfigGroup = new CliquesConfigGroup();
		JointTripsMutatorConfigGroup mutatorConfigGroup = new JointTripsMutatorConfigGroup();
		JointTimeModeChooserConfigGroup tmcConfigGroup = new JointTimeModeChooserConfigGroup();

		Config config = ConfigUtils.createConfig();

		// /////////////////////////////////////////////////////////////////////
		// initialize the config before passing it to the controler
		config.addCoreModules();
		config.addModule(JointReplanningConfigGroup.GROUP_NAME, jointConfigGroup);
		config.addModule(CliquesConfigGroup.GROUP_NAME, cliquesConfigGroup);
		config.addModule(JointTripsMutatorConfigGroup.GROUP_NAME, mutatorConfigGroup);
		config.addModule(JointTimeModeChooserConfigGroup.GROUP_NAME, tmcConfigGroup);

		//read the config file
		ConfigUtils.loadConfig(config, configFile);

		return config;
	}

	/**
	 * In case facilities are defined, sets the scoring function factory to
	 * {@link KtiLikeActivitiesScoringFunctionFactory}
	 */
	private static void setScoringFunction(final Controler controler) {
		ActivityFacilities facilities = controler.getFacilities();
		int nFacilities = facilities.getFacilities().size();

		if (nFacilities > 0) {
			Config config = controler.getConfig();
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup = 
				config.planCalcScore();

			// TODO: choose from some config group?
			ScoringFunctionFactory factory =
				// new CharyparNagelOpenTimesScoringFunctionFactory(
				new KtiLikeActivitiesScoringFunctionFactory(
						planCalcScoreConfigGroup,
						controler.getScenario());
				//new HerbieBasedScoringFunctionFactory(
				//		config,
				//		controler.getScenario());
			controler.setScoringFunctionFactory(factory);
			controler.addControlerListener( CarPoolingLegScoringFunction.getInformationLogger() );
		}
		else {
			controler.setScoringFunctionFactory(
					new CharyparNagelScoringFunctionFactory(
						controler.getConfig().planCalcScore(),
						controler.getScenario().getNetwork()) );
		}
	}
}

