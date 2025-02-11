package org.matsim.contrib.drt.extension.fiss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.core.config.groups.ReplanningConfigGroup.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;

public class RunFissDrtScenarioIT {
	private static final Logger LOG = LogManager.getLogger( RunFissDrtScenarioIT.class );

	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test void test() {

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

		String fleetFile = "holzkirchenFleet.xml";
		String plansFile = "holzkirchenPlans_car_drt.xml.gz";
		String networkFile = "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile = "holzkirchenOperationFacilities.xml";
		String shiftsFile = "holzkirchenShifts.xml";

		DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

		DrtConfigGroup drtConfigGroup = drtWithShiftsConfigGroup;
		drtConfigGroup.mode = TransportMode.drt;
		drtConfigGroup.stopDuration = 30.;
		DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
				(DefaultDrtOptimizationConstraintsSet) drtConfigGroup.addOrGetDrtOptimizationConstraintsParams()
						.addOrGetDefaultDrtOptimizationConstraintsSet();
		defaultConstraintsSet.maxTravelTimeAlpha = 1.5;
		defaultConstraintsSet.maxTravelTimeBeta = 10. * 60.;
		defaultConstraintsSet.maxWaitTime = 600.;
		defaultConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = true;
		defaultConstraintsSet.maxWalkDistance = 1000.;
		drtConfigGroup.useModeFilteredSubnetwork = false;
		drtConfigGroup.vehiclesFile = fleetFile;
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.door2door;
		drtConfigGroup.plotDetailedCustomerStats = true;
		drtConfigGroup.idleVehiclesReturnToDepots = false;

		drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtConfigGroup.createParameterSet("rebalancing");
		drtConfigGroup.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).interval = 600;

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.targetAlpha = 0.3;
		strategyParams.targetBeta = 0.3;

		drtConfigGroup.getRebalancingParams().get().addParameterSet(strategyParams);

		DrtZoneSystemParams drtZoneSystemParams = new DrtZoneSystemParams();
		SquareGridZoneSystemParams zoneSystemParams = (SquareGridZoneSystemParams) drtZoneSystemParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		zoneSystemParams.cellSize = 500.;
		drtZoneSystemParams.addParameterSet(zoneSystemParams);
		drtZoneSystemParams.targetLinkSelection = DrtZoneSystemParams.TargetLinkSelection.mostCentral;
		drtConfigGroup.addParameterSet(drtZoneSystemParams);

		multiModeDrtConfigGroup.addParameterSet(drtWithShiftsConfigGroup);

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup, dvrpConfigGroup);
		config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

		Set<String> modes = new HashSet<>();
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		config.scoring().addModeParams( new ModeParams("drt") );
		config.scoring().addModeParams( new ModeParams("walk") );

		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);

		DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

		config.scoring().addActivityParams( new ActivityParams("home").setTypicalDuration(8 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("other").setTypicalDuration(4 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("education").setTypicalDuration(6 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("shopping").setTypicalDuration(2 * 3600 ) );
		config.scoring().addActivityParams( new ActivityParams("work").setTypicalDuration(2 * 3600 ) );

		config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(1 ) );

		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory( utils.getOutputDirectory() );

		DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
		ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
		OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
		operationsParams.addParameterSet(shiftsParams);
		operationsParams.addParameterSet(operationFacilitiesParams);

		operationFacilitiesParams.operationFacilityInputFile = opFacilitiesFile;
		shiftsParams.shiftInputFile = shiftsFile;
		shiftsParams.allowInFieldChangeover = true;
		drtWithShiftsConfigGroup.addParameterSet(operationsParams);


		if (!config.qsim().getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
			config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		}

		// ### controler:

		final Controler controler = DrtOperationsControlerCreator.createControler(config, false);

		//FISS part
		{
			// FISS config:
			FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
			fissConfigGroup.sampleFactor = 0.1;
			fissConfigGroup.sampledModes = Set.of(TransportMode.car);
			fissConfigGroup.switchOffFISSLastIteration = true;

			// provide mode vehicle types (in production code, one should set them more diligently):
			Vehicles vehiclesContainer = controler.getScenario().getVehicles();
			for( String sampledMode : fissConfigGroup.sampledModes ){
				vehiclesContainer.addVehicleType( VehicleUtils.createVehicleType( Id.create( sampledMode, VehicleType.class ) ) );
			}

			// add FISS module:
			controler.addOverridingModule( new FISSModule() );

		}

		// for testing:
		LinkCounter linkCounter = new LinkCounter();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(linkCounter);
			}
		});

		controler.run();
		{
			String expected = utils.getInputDirectory() + "0.events.xml.gz" ;
			String actual = utils.getOutputDirectory() + "ITERS/it.0/0.events.xml.gz" ;
			ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
			assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
		}
		{
			String expected = utils.getInputDirectory() + "output_events.xml.gz" ;
			String actual = utils.getOutputDirectory() + "output_events.xml.gz" ;
			ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
			assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
		}
		Assertions.assertEquals(20000, linkCounter.getLinkLeaveCount(), 2000);// yy why a delta of 2000?  kai, jan'25


	}

	static class LinkCounter implements LinkLeaveEventHandler {
		private int counts = 0;

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			this.counts++;
		}

		public int getLinkLeaveCount() {
			return this.counts;
		}
	}

}
