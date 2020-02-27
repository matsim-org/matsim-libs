package org.matsim.contrib.freight.jsprit;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;

import javax.management.InvalidAttributeValueException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class IntegrationIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testJsprit() throws InvalidAttributeValueException {
		final String networkFilename = utils.getClassInputDirectory() + "/merged-network-simplified.xml.gz";
		final String vehicleTypeFilename = utils.getClassInputDirectory() + "/vehicleTypes.xml";
		final String carrierFilename = utils.getClassInputDirectory() + "/carrier.xml";
		
		Config config = ConfigUtils.createConfig();
		config.global().setRandomSeed(4177);

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setCarriersFile(carrierFilename);
		freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypeFilename);
		freightConfigGroup.setTravelTimeSliceWidth(24*3600);
		freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
	
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
		Controler controler = new Controler(scenario);
		for (Carrier carrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
			CarrierUtils.setJspritIterations(carrier, 1);
		}
		FreightUtils.runJsprit(controler);
		double scoreWithRunJsprit = 0;
		for (Carrier carrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
			scoreWithRunJsprit = scoreWithRunJsprit + carrier.getSelectedPlan().getScore();
		}
		double scoreRunWithOldStructure = generateCarrierPlans(scenario.getNetwork(), FreightUtils.getCarriers(controler.getScenario()), FreightUtils.getCarrierVehicleTypes(controler.getScenario()));
		Assert.assertEquals("The score of both runs are not the same", scoreWithRunJsprit, scoreRunWithOldStructure, MatsimTestUtils.EPSILON);
	}

	private static double generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes) {
		final Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		// netBuilder.setBaseTravelTimeAndDisutility(travelTime, travelDisutility) ;
		netBuilder.setTimeSliceWidth(1800); // !!!!, otherwise it will not do anything.
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
		double score = 0;

		for (Carrier carrier : carriers.getCarriers().values()) {
			
			carrier.clearPlans();
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier,
					network);
			vrpBuilder.setRoutingCost(netBasedCosts);
			VehicleRoutingProblem problem = vrpBuilder.build();

			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);

			NetworkRouter.routePlan(newPlan, netBasedCosts);
			// (maybe not optimal, but since re-routing is a matsim strategy,
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan);

			SolutionPrinter.print(problem, solution, SolutionPrinter.Print.VERBOSE);
			score = score + newPlan.getScore();
		}
		return score;
	}

	private static CarrierVehicleTypes createVehicleTypes(String vehicleTypeFilename) {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypeFilename);
		return vehicleTypes;
	}

	private static Carriers createCarriers(String carrierFilename, CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers();
		{
			Carrier carrier = CarrierUtils.createCarrier(Id.create("TestCarrier", Carrier.class));
			{
				org.matsim.contrib.freight.carrier.CarrierCapabilities.Builder capabilityBuilder = CarrierCapabilities.Builder
						.newInstance();
				capabilityBuilder.setFleetSize(FleetSize.FINITE);
				{
					CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(
							Id.create("TestVehicle1", Vehicle.class), Id.create("182616-182618", Link.class));
					vehicleBuilder.setTypeId(Id.create("15_tonner", VehicleType.class));

					vehicleBuilder.setEarliestStart(0.);
					vehicleBuilder.setLatestEnd(24. * 3600.);

					CarrierVehicle vehicle = vehicleBuilder.build();
					capabilityBuilder.addVehicle(vehicle);
				}
				CarrierCapabilities capabilities = capabilityBuilder.build();
				carrier.setCarrierCapabilities(capabilities);
			}
			for (int ii = 1; ii <= 100; ii++) {
				CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(
						Id.create("service" + Integer.toString(ii), CarrierService.class),
						Id.create("146829", Link.class));
				serviceBuilder.setCapacityDemand(33);
				serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(0., 24. * 3600.));
				serviceBuilder.setServiceDuration(10. * 60.);
				CarrierService carrierService = serviceBuilder.build();
				CarrierUtils.addService(carrier, carrierService);
			}
			carriers.addCarrier(carrier);
		}

		// assign vehicle types to the carriers (who already have their vehicles (??)):
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
		return carriers;
	}

}
