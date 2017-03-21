/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.VehicleUtils;

/**
 * @author amit
 */

public class PatnaBikeTrackConnectionControler {

	private static String dir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/";
	private static String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
	private static final int initialStabilizationIterations = 100;

	private static int numberOfConnectors = 499;
	private static int updateConnectorsAfterIteration = 2;
	private static double reduceLinkLengthBy = PatnaUtils.BIKE_TRACK_LEGNTH_REDUCTION_FACTOR;
	private static boolean useBikeTravelTime = true;

	private static boolean modeChoiceUntilLastIteration = true;

	private static boolean isRunningOnCluster = false;

	public static void main(String[] args) {

		if(args.length>0){
			dir= args[0];
			numberOfConnectors = Integer.valueOf(args[1]);
			updateConnectorsAfterIteration = Integer.valueOf(args[2]);
			bikeTrack = args[3];
			reduceLinkLengthBy = Double.valueOf(args[4]);
			useBikeTravelTime = Boolean.valueOf(args[5]);

			modeChoiceUntilLastIteration = Boolean.valueOf(args[6]);

			isRunningOnCluster = true;
		}

		String regularNet = dir+"/input/network.xml.gz";
		String outBikeTrackConnectorFile = dir+"/input/networkWiBikeTrackAndConnectors.xml.gz";

		Set<String> allowedModes = new HashSet<>(Arrays.asList("bike"));
		BikeTrackNetworkWithConnectorsWriter trackNetworkWithConnectorsWriter = new BikeTrackNetworkWithConnectorsWriter(regularNet);
		trackNetworkWithConnectorsWriter.processBikeTrackFile(bikeTrack, reduceLinkLengthBy, allowedModes);
		trackNetworkWithConnectorsWriter.writeNetwork(outBikeTrackConnectorFile);

		Scenario scenario = prepareScenario(outBikeTrackConnectorFile);

		scenario.getConfig().controler().setOutputDirectory(dir+"bikeTrackConnectors_"+numberOfConnectors+"_"+updateConnectorsAfterIteration+"_"+
				reduceLinkLengthBy+"_"+useBikeTravelTime+"_"+modeChoiceUntilLastIteration+"/");
		BikeConnectorControlerListener bikeConnectorControlerListener = new BikeConnectorControlerListener(numberOfConnectors, updateConnectorsAfterIteration, initialStabilizationIterations);

		final Controler controler = new Controler(scenario);

		TerminationCriterion terminationCriterion = new MyTerminationCriteria(bikeConnectorControlerListener);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// plotting modal share over iterations
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				// adding pt fare system based on distance
				this.addEventHandlerBinding().to(PtFareEventHandler.class);

				// it has been observed that, bike travel time routes more agents on bike track than using default settings in
				// travel time calculator config group. Amit Dec 16.
				addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);

				addControlerListenerBinding().toInstance(bikeConnectorControlerListener);
				bind(TerminationCriterion.class).toInstance(terminationCriterion);
			}
		});

		// for PT fare system to work, make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// add income dependent scoring function factory
		addScoringFunction(controler);

		controler.run();

		String outputDir = controler.getScenario().getConfig().controler().getOutputDirectory();

		String outputEventsFile = outputDir+"/output_events.xml.gz";
		if(new File(outputEventsFile).exists()) {
			new File(outputDir+"/analysis/").mkdir();
			// write some default analysis
			String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
			ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
			mtta.run();
			mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
			msc.run();
			msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");
		}

		//StatsWriter.run(outputDir);
	}

	private static Scenario prepareScenario(final String networkFile) {
		Config config = ConfigUtils.createConfig();

		String inputDir = dir+"/input/";
		String configFile = inputDir + "configBaseCaseCtd.xml";

		ConfigUtils.loadConfig(config, configFile);

		config.network().setInputFile(networkFile);
//		config.network().setInputFile(inputDir+"network.xml.gz");
		// time dependent network for network change events
		config.network().setTimeVariantNetwork(true);

		String inPlans ;
		if(isRunningOnCluster) inPlans = inputDir+"/baseCaseOutput_plans.xml.gz";
		else inPlans = inputDir+"samplePlansForTesting.xml";

		config.plans().setInputFile( inPlans );
		config.plans().setInputPersonAttributeFile(inputDir+"output_personAttributes.xml.gz");
		config.vehicles().setVehiclesFile(null); // see below for vehicle type info

		//==
		// after calibration;  departure time is fixed for urban; check if time choice is not present
		config.strategy().setFractionOfIterationsToDisableInnovation(1.0); // let all the innovations (except mode choice) go on until last iteration.
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			ss.setDisableAfter(-1);
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
			} else if ( ! modeChoiceUntilLastIteration && ss.getStrategyName().equals(DefaultStrategy.ChangeTripMode.toString())) {
				ss.setDisableAfter(config.controler().getFirstIteration() + initialStabilizationIterations);
			}
		}

		//==
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setDumpDataAtEnd(true);

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.vspExperimental().setWritingOutputEvents(true);

		config.travelTimeCalculator().setSeparateModes(true);
		config.travelTimeCalculator().setAnalyzedModes(String.join(",", PatnaUtils.ALL_MAIN_MODES));

		Scenario scenario = ScenarioUtils.loadScenario(config);

		String vehiclesFile = inputDir+"output_vehicles.xml.gz"; // following is required to extract only vehicle types and not vehicle info. Amit Nov 2016
		VehicleUtils.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

		// no vehicle info should be present if using VehiclesSource.modeVEhicleTypesFromVehiclesData
		if ( scenario.getConfig().qsim().getVehiclesSource()==QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData  &&
                !scenario.getVehicles().getVehicles().isEmpty()) {
			throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
					QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
		}
		return scenario;
	}

	private static void addScoringFunction(final Controler controler){
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );
			@Inject
			Network network;
			@Inject
			Population population;
			@Inject
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject
			ScenarioConfigGroup scenarioConfig;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				ScoringParameters.Builder builder = new ScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final ScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		});
	}

	private static class MyTerminationCriteria implements TerminationCriterion{

		private final BikeConnectorControlerListener bikeConnectorControlerListener;

		MyTerminationCriteria(final BikeConnectorControlerListener bikeConnectorControlerListener) {
			this.bikeConnectorControlerListener = bikeConnectorControlerListener;
		}

		@Override
		public boolean continueIterations(int iteration) {
			return !this.bikeConnectorControlerListener.isTerminating();
		}
	}
}