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
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.dashboard.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class MATSimIterations{
	private static final Logger log = LogManager.getLogger( MATSimIterations.class );
	private final Integer matSimIterationsAfterDemandGeneration;
	private final Double sample;

	MATSimIterations( Integer matsimIterationsAfterDemandGeneration, Double sample ) {
		// the sequence of constructor arguments is random.  Possibly replace by builder so that the sequence no longer matters.
		this.matSimIterationsAfterDemandGeneration = matsimIterationsAfterDemandGeneration;
		this.sample = sample;
	}

	void runMATSimIterations( Scenario scenario, Config config,
	                          GenerateSmallScaleCommercialTrafficDemand generateSmallScaleCommercialTrafficDemand ){
		log.info("Running MATSim for {} iterations after demand generation.", matSimIterationsAfterDemandGeneration );
		Carriers carriers = CarriersUtils.addOrGetCarriers( scenario );
		carriers.getCarriers().clear();

		//this is necessary because integrated existing models can have additional vehicleTypes
		CarriersUtils.getOrAddCarrierVehicleTypes( scenario ).getVehicleTypes().values().forEach( vehicleType -> {
			log.info("Adding vehicle type {} to scenario vehicles.", vehicleType.getId() );
			if (!scenario.getVehicles().getVehicleTypes().containsKey(vehicleType.getId() ))
				scenario.getVehicles().addVehicleType(vehicleType );
		} );
		Set<String> activityTypes = new HashSet<>(
			scenario.getPopulation().getPersons().values().stream()
			        .flatMap(person -> PopulationUtils.getActivities(person.getSelectedPlan(),
					TripStructureUtils.StageActivityHandling.ExcludeStageActivities ).stream() )
			        .map( Activity::getType )
			        .toList()
		);
		for (String activityType : activityTypes) {
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(activityType).setTypicalDuration(30 * 60 ) );
		}
		List<String> subpopulations = scenario.getPopulation().getPersons().values().stream()
		                                      .map(PopulationUtils::getSubpopulation)
		                                      .filter( Objects::nonNull )
		                                      .toList();

		subpopulations.forEach(subpopulation -> {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(
					DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta ).setWeight(
					0.85 ).setSubpopulation(subpopulation) );

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(
					0.1).setSubpopulation(subpopulation) );
		});

		Set<String> modes = scenario.getVehicles().getVehicleTypes().values().stream()
		                            .map(vehicleType -> vehicleType.getId().toString())
		                            .distinct().collect( Collectors.toSet() );

		modes.forEach(mode -> {
			ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
			config.scoring().addModeParams(thisModeParams );
		});

		Set<String> qsimModes = new HashSet<>( config.qsim().getMainModes());
		config.qsim().setMainModes( Sets.union(qsimModes, modes ) );

		Set<String> networkModes = new HashSet<>( config.routing().getNetworkModes());
		config.routing().setNetworkModes(Sets.union(networkModes, modes ) );

		SimWrapper sw = SimWrapper.create();
		sw.getConfigGroup().defaultParams().setShp(null);
		sw.getConfigGroup().setDefaultDashboards( SimWrapperConfigGroup.DefaultDashboardsMode.disabled );
		sw.getConfigGroup().setSampleSize( this.sample );
		sw.addDashboard(new OverviewDashboard(modes) );
		sw.addDashboard(new CarrierDashboard("(*.)?output_carriers_withPlans.xml.gz") );
		sw.addDashboard(new TripDashboard().setGroupsOfSubpopulationsForCommercialAnalysis("commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic" ).setAnalysisArgs("--shp-filter", "none" ) );
		sw.addDashboard(new CommercialTrafficDashboard(
			config.global().getCoordinateSystem()).setGroupsOfSubpopulationsForCommercialAnalysis("commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic" ) );
		sw.addDashboard(new TrafficDashboard(modes) );
		Controller controller = generateSmallScaleCommercialTrafficDemand.matsimIterations.prepareController( scenario );

		if (!RoadPricingUtils.addOrGetRoadPricingScheme( scenario ).getTolledLinkIds().isEmpty()) {
			controller.addOverridingModule(new RoadPricingModule(RoadPricingUtils.addOrGetRoadPricingScheme( scenario )) );
		}
		controller.addOverridingModule(new SimWrapperModule(sw) );

		// Creating inject always adds check for unmaterialized config groups.
		controller.getInjector();

		// Removes check after injector has been created
		controller.getConfig().removeConfigConsistencyChecker( UnmaterializedConfigGroupChecker.class );

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
