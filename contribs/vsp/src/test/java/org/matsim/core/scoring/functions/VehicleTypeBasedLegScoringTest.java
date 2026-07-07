package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleTypeBasedLegScoringTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void usesVehicleTypeScoringParametersWhenAvailable() {
		var vehicles = VehicleUtils.createVehiclesContainer();
		var truckType = addVehicleType(vehicles, "truck");
		var truckId = addVehicle(vehicles, "truck-1", truckType);
		var scoringConfig = createScoringConfig();

		scoringConfig.getOrCreateModeParams(TransportMode.car)
			.setConstant(-100)
			.setMarginalUtilityOfTraveling(-100)
			.setMarginalUtilityOfDistance(-100)
			.setMonetaryDistanceRate(-100)
			.setDailyMonetaryConstant(-100)
			.setDailyUtilityConstant(-100);

		scoringConfig.getOrCreateModeParams("truck")
			.setConstant(-1)
			.setMarginalUtilityOfTraveling(-7.2)
			.setMarginalUtilityOfDistance(-0.3)
			.setMonetaryDistanceRate(-0.4)
			.setDailyMonetaryConstant(-5)
			.setDailyUtilityConstant(-6);

		var params = createScoringParameters(scoringConfig);
		var scoring = new VehicleTypeBasedLegScoring(vehicles, params, Set.of());
		var leg = createLeg(TransportMode.car, 20, 30, truckId);

		scoring.handleTrip(TripStructureUtils.getTrips2(List.of(leg)).getFirst());

		assertEquals(-0.04 - 9 - 12 - 1 - 5 - 6, scoring.getScore(), MatsimTestUtils.EPSILON);
	}

	@Test
	void derivesScoringParametersFromVehicleTypeCostsWhenNoModeParamsExist() {
		var vehicles = VehicleUtils.createVehiclesContainer();
		var truckType = addVehicleType(vehicles, "truck");
		truckType.getCostInformation()
			.setFixedCost(5.0)
			.setCostsPerMeter(0.3)
			.setCostsPerSecond(0.2);
		var truckId = addVehicle(vehicles, "truck-1", truckType);
		var scoringConfig = createScoringConfig();
		scoringConfig.getOrCreateModeParams(TransportMode.car);

		var params = createScoringParameters(scoringConfig);
		var scoring = new VehicleTypeBasedLegScoring(vehicles, params, Set.of());
		var leg = createLeg(TransportMode.car, 20, 30, truckId);

		scoring.handleTrip(TripStructureUtils.getTrips2(List.of(leg)).getFirst());

		assertEquals(-4 - 9 - 5, scoring.getScore(), MatsimTestUtils.EPSILON);
		assertTrue(params.modeParams.containsKey("truck"));
	}

	@Test
	void addsOnlyUsedVehicleTypeScoringParametersToRelevantSubpopulation() {
		var config = ConfigUtils.createConfig();
		var vehicles = VehicleUtils.createVehiclesContainer();
		var privateTruckType = addVehicleType(vehicles, "privateTruck");
		privateTruckType.getCostInformation()
			.setFixedCost(5.0)
			.setCostsPerMeter(0.3)
			.setCostsPerSecond(0.2);
		var freightTruckType = addVehicleType(vehicles, "freightTruck");
		freightTruckType.getCostInformation()
			.setFixedCost(5.0)
			.setCostsPerMeter(0.3)
			.setCostsPerSecond(0.2);
		var privateTruckId = addVehicle(vehicles, "private-truck-1", privateTruckType);
		var freightTruckId = addVehicle(vehicles, "freight-truck-1", freightTruckType);

		var privateScoringParameterSet = config.scoring().getOrCreateScoringParameters("private");
		var freightScoringParameterSet = config.scoring().getOrCreateScoringParameters("freight");
		privateScoringParameterSet.setMarginalUtlOfWaitingPt_utils_hr(0.0);
		freightScoringParameterSet.setMarginalUtlOfWaitingPt_utils_hr(0.0);

		var privateScoring = new VehicleTypeBasedLegScoring(
			vehicles,
			createScoringParameters(config.scoring(), privateScoringParameterSet),
			privateScoringParameterSet,
			Set.of()
		);
		privateScoring.handleTrip(TripStructureUtils.getTrips2(List.of(createLeg(TransportMode.car, 20, 30, privateTruckId))).getFirst());

		var freightScoring = new VehicleTypeBasedLegScoring(
			vehicles,
			createScoringParameters(config.scoring(), freightScoringParameterSet),
			freightScoringParameterSet,
			Set.of()
		);
		freightScoring.handleTrip(TripStructureUtils.getTrips2(List.of(createLeg(TransportMode.car, 20, 30, freightTruckId))).getFirst());

		assertVehicleTypeParams(privateScoringParameterSet.getModes().get("privateTruck"));
		assertNull(privateScoringParameterSet.getModes().get("freightTruck"));
		assertVehicleTypeParams(freightScoringParameterSet.getModes().get("freightTruck"));
		assertNull(freightScoringParameterSet.getModes().get("privateTruck"));

		var outFile = utils.getOutputDirectory() + "/vehicle-type-based-scoring-config.xml";
		new ConfigWriter(config).write(outFile);

		var readConfig = ConfigUtils.createConfig();
		new ConfigReader(readConfig).readFile(outFile);

		var readPrivateParams = readConfig.scoring().getScoringParameters("private");
		var readFreightParams = readConfig.scoring().getScoringParameters("freight");
		assertVehicleTypeParams(readPrivateParams.getModes().get("privateTruck"));
		assertNull(readPrivateParams.getModes().get("freightTruck"));
		assertVehicleTypeParams(readFreightParams.getModes().get("freightTruck"));
		assertNull(readFreightParams.getModes().get("privateTruck"));
	}

	@Test
	void keepsConfiguredVehicleTypeScoringParametersInScoringConfigGroup() {
		var vehicles = VehicleUtils.createVehiclesContainer();
		var truckType = addVehicleType(vehicles, "truck");
		truckType.getCostInformation()
			.setFixedCost(5.0)
			.setCostsPerMeter(0.3)
			.setCostsPerSecond(0.2);
		var truckId = addVehicle(vehicles, "truck-1", truckType);

		var scoringConfig = createScoringConfig();
		var scoringParameterSet = scoringConfig.getScoringParameters(null);
		scoringConfig.getOrCreateModeParams("truck")
			.setConstant(-99.0)
			.setMarginalUtilityOfTraveling(-88.0)
			.setMarginalUtilityOfDistance(-77.0)
			.setDailyMonetaryConstant(-66.0);

		var params = createScoringParameters(scoringConfig, scoringParameterSet);
		var scoring = new VehicleTypeBasedLegScoring(vehicles, params, scoringParameterSet, Set.of());
		scoring.handleTrip(TripStructureUtils.getTrips2(List.of(createLeg(TransportMode.car, 20, 30, truckId))).getFirst());

		var truckParams = scoringConfig.getScoringParameters(null).getModes().get("truck");
		assertNotNull(truckParams);
		assertEquals(-99.0, truckParams.getConstant(), MatsimTestUtils.EPSILON);
		assertEquals(-88.0, truckParams.getMarginalUtilityOfTraveling(), MatsimTestUtils.EPSILON);
		assertEquals(-77.0, truckParams.getMarginalUtilityOfDistance(), MatsimTestUtils.EPSILON);
		assertEquals(-66.0, truckParams.getDailyMonetaryConstant(), MatsimTestUtils.EPSILON);
	}

	private static ScoringConfigGroup createScoringConfig() {
		var scoringConfig = new ScoringConfigGroup();
		scoringConfig.setMarginalUtilityOfMoney(1.0);
		return scoringConfig;
	}

	private static ScoringParameters createScoringParameters(ScoringConfigGroup scoringConfig) {
		return createScoringParameters(scoringConfig, scoringConfig.getScoringParameters(null));
	}

	private static ScoringParameters createScoringParameters(ScoringConfigGroup scoringConfig, ScoringConfigGroup.ScoringParameterSet scoringParameterSet) {
		return new ScoringParameters.Builder(
			scoringConfig,
			scoringParameterSet,
			Map.of(),
			new ScenarioConfigGroup()
		).build();
	}

	private static VehicleType addVehicleType(Vehicles vehicles, String id) {
		var vehicleType = vehicles.getFactory().createVehicleType(Id.create(id, VehicleType.class));
		vehicles.addVehicleType(vehicleType);
		return vehicleType;
	}

	private static Id<Vehicle> addVehicle(Vehicles vehicles, String id, VehicleType vehicleType) {
		var vehicleId = Id.create(id, Vehicle.class);
		vehicles.addVehicle(vehicles.getFactory().createVehicle(vehicleId, vehicleType));
		return vehicleId;
	}

	private static Leg createLeg(String mode, double travelTime, double distance, Id<Vehicle> vehicleId) {
		var leg = PopulationUtils.createLeg(mode);
		leg.setDepartureTime(0);
		leg.setTravelTime(travelTime);
		var route = RouteUtils.createGenericRouteImpl(Id.createLinkId("start"), Id.createLinkId("end"));
		route.setDistance(distance);
		leg.setRoute(route);
		leg.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, vehicleId);
		return leg;
	}

	private static void assertVehicleTypeParams(ScoringConfigGroup.ModeParams truckParams) {
		assertNotNull(truckParams);
		assertEquals(-5.0, truckParams.getDailyMonetaryConstant(), MatsimTestUtils.EPSILON);
		assertEquals(-0.3, truckParams.getMarginalUtilityOfDistance(), MatsimTestUtils.EPSILON);
		assertEquals(-0.2 * 3600, truckParams.getMarginalUtilityOfTraveling(), MatsimTestUtils.EPSILON);
	}
}
