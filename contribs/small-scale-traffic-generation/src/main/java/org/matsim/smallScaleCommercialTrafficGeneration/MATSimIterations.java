package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.VehicleTypeBasedScoringFunctionFactory;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.dashboard.*;

import java.util.HashSet;
import java.util.Set;


class MATSimIterations{
	private static final Logger log = LogManager.getLogger( MATSimIterations.class );
	private final Double sample;

	MATSimIterations(Double sample) {
		// the sequence of constructor arguments is random.  Possibly replace by builder so that the sequence no longer matters.
		this.sample = sample;
	}

	void runMATSimIterations(Scenario scenario, Config config,
	                         GenerateSmallScaleCommercialTrafficDemand generateSmallScaleCommercialTrafficDemand) {
		log.info("Running MATSim until iteration {} after demand generation.", config.controller().getLastIteration());
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		carriers.getCarriers().clear();

		//this is necessary because integrated existing models can have additional vehicleTypes
		CarriersUtils.getOrAddCarrierVehicleTypes(scenario).getVehicleTypes().values().forEach(vehicleType -> {
			log.info("Adding vehicle type {} to scenario vehicles.", vehicleType.getId());
			if (!scenario.getVehicles().getVehicleTypes().containsKey(vehicleType.getId()))
				scenario.getVehicles().addVehicleType(vehicleType);
		});

		Set<String> modes = NetworkUtils.getModes(scenario.getNetwork());
		Set<String> subpopulations = PopulationUtils.getSubpopulationsOfPopulation(scenario.getPopulation());

		subpopulations.forEach(subpopulation -> {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(
					0.85).setSubpopulation(subpopulation));

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(
					0.1).setSubpopulation(subpopulation));

			Set<String> activityTypesPerSubpopulation = new HashSet<>(
				scenario.getPopulation().getPersons().values().stream()
					.filter(person -> PopulationUtils.getSubpopulation(person).equals(subpopulation))
					.flatMap(person -> PopulationUtils.getActivities(person.getSelectedPlan(),
						TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream())
					.map(Activity::getType)
					.toList()
			);

			ScoringConfigGroup.ScoringParameterSet scoringParameters = config.scoring().getOrCreateScoringParameters(subpopulation);
			scoringParameters.setPerforming_utils_hr(32.);
			scoringParameters.setMarginalUtlOfWaitingPt_utils_hr(0.);
			activityTypesPerSubpopulation.forEach(activityType -> {
				ScoringConfigGroup.ActivityParams actParams = new ScoringConfigGroup.ActivityParams(activityType).setTypicalDuration(30 * 60);
				scoringParameters.addActivityParams(actParams);
			});
			modes.forEach(mode -> {
				ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
				scoringParameters.addModeParams(thisModeParams);
			});
			scoringParameters.addModeParams(new ScoringConfigGroup.ModeParams("walk"));
		});
		config.scoring().setExplainScores(true);
		config.scoring().setScoringParametersAsDefaultSubpopulation(subpopulations.stream().findFirst().orElseThrow());

		Set<String> qsimModes = new HashSet<>(config.qsim().getMainModes());
		Set<String> allQsimModes = Sets.union(qsimModes, modes);
		config.qsim().setMainModes(allQsimModes);
		SmallScaleCommercialTrafficUtils.ensureDefaultModeParams(config, allQsimModes);
		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		Set<String> allNetworkModes = Sets.union(networkModes, modes);
		config.routing().setNetworkModes(allNetworkModes);
		SmallScaleCommercialTrafficUtils.ensureDefaultModeParams(config, allNetworkModes);

		SimWrapper sw = SimWrapper.create(config);
		sw.getConfigGroup().defaultParams().setShp(null);
		sw.getConfigGroup().setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
		sw.getConfigGroup().setSampleSize(sample);
		sw.addDashboard(new OverviewDashboard(modes));
		sw.addDashboard(new CarrierDashboard("(*.)?output_carriers_solvedVRP.xml.gz"));
		String subpopSetterForDashboards;
		if (generateSmallScaleCommercialTrafficDemand.getUsedSmallScaleCommercialTrafficSegment() == GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment.completeSmallScaleCommercialTraffic)
			subpopSetterForDashboards = "commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic";
		else if (generateSmallScaleCommercialTrafficDemand.getUsedSmallScaleCommercialTrafficSegment() == GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment.commercialPersonTraffic)
			subpopSetterForDashboards = "commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service";
		else if (generateSmallScaleCommercialTrafficDemand.getUsedSmallScaleCommercialTrafficSegment() == GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment.goodsTraffic)
			subpopSetterForDashboards = "smallScaleGoodsTraffic=goodsTraffic";
		else
			throw new RuntimeException("No traffic type selected.");

		sw.addDashboard(
			new TripDashboard().setGroupsOfSubpopulationsForCommercialAnalysis(subpopSetterForDashboards).setAnalysisArgs("--shp-filter", "none"));
		sw.addDashboard(new CommercialTrafficDashboard(config.global().getCoordinateSystem()).setGroupsOfSubpopulationsForCommercialAnalysis(
			subpopSetterForDashboards));
		sw.addDashboard(new TrafficDashboard(modes));
		Controller controller = prepareController(scenario);

		if (!RoadPricingUtils.addOrGetRoadPricingScheme(scenario).getTolledLinkIds().isEmpty()) {
			controller.addOverridingModule(new RoadPricingModule(RoadPricingUtils.addOrGetRoadPricingScheme(scenario)));
		}
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringFunctionFactory.class).to(VehicleTypeBasedScoringFunctionFactory.class);
			}
		});
		controller.addOverridingModule(new SimWrapperModule(sw));

		// Creating inject always adds check for unmaterialized config groups.
		controller.getInjector();

		// Removes check after injector has been created
		controller.getConfig().removeConfigConsistencyChecker(UnmaterializedConfigGroupChecker.class);

		controller.run();
	}
	/**
	 * Prepares the controller.
	 *
	 * @param scenario
	 */
	Controller prepareController( Scenario scenario ) {
		Controller controller = ControllerUtils.createController(scenario );
		// use overwriteExistingFiles because before setting up the OutputDirectoryHierarchy, the OverwriteFileSetting was failIfDirectoryExists
		// in mean time some files were already written (e.g. carriers analysis), so we need to allow overwriting here
		controller.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort );
		return controller;
	}
}
