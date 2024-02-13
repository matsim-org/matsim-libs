package org.matsim.contrib.drt.teleportation;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.routing.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.*;

class Test2 {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@org.junit.jupiter.api.Test
	void test1() {

		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_1.0.xml.gz");

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		// install the drt routing stuff, but not the mobsim stuff!
		Controler controler = DrtControlerCreator.createControler(config, false);


		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);

		DrtEstimatorParams params = new DrtEstimatorParams();
		params.teleport=true;
		drtConfigGroup.addParameterSet(params);

		System.out.println(config);

		// TODO
		// We want to use DRT infrastructure (routing) so we need to integrate into drt teleportation
		// Write our own TeleportingPassengerEngine
		// this engine can either calc estimates beforehand or during departure (using information of drt router)


		// alternative: implement our own router
		// do nothing drt specific -> calculate travel time information during routing
		// can use standard teleportation engines given route information
		// we need to update routes ourself, we have no drt access egress, no waiting times, no drt output or requests
		// this would be more general, could be useful for other use cases?
		// but we only need it for DRT for now?

		/*
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addRoutingModuleBinding( "drt" ).to( DrtEstimatingRoutingModule.class );
			}
		} );
		 */

		controler.run();

	}


	public record EstimatedDrtTeleportationInfo(double estTotalTravelTime, double estWaitTime, double estRideTime, double estRideDistance) {
		// The teleportation info refers to the main DRT leg (accessLink -> egressLink, departing at departureTime)
		// The info teleportation engine can make use of this data to generate a teleport leg
		// wait time and ride time are specified, such that we can score them differently (if required)
	}

	public record DrtRouteInfoEstimator(LeastCostPathCalculator router, TravelTime travelTime, DistributionGenerator distributionGenerator) {
		// This record/class is to be created before (each?) MobSim/QSim.
		public EstimatedDrtTeleportationInfo estimateDrtRoute(Link accessLink, Link egressLink, double departureTime) {
			double waitTime = distributionGenerator.generateWaitTime();
			double directRideTime = VrpPaths.calcAndCreatePath(accessLink, egressLink, departureTime + waitTime, router, travelTime).getTravelTime();
			double rideTime = distributionGenerator.generateRideTime(directRideTime);

			// since VrpPath does not contain Path information. We need to calculate this manually.
			// VRP path logic: toNode of fromLink -> fromNode of toLink -> toNode of toLink
			LeastCostPathCalculator.Path path = router.calcLeastCostPath(accessLink.getToNode(), egressLink.getFromNode(),
				departureTime + waitTime, null, null);
			path.links.add(egressLink);

			double directRideDistance = path.links.stream().mapToDouble(Link::getLength).sum();
			double rideDistance = distributionGenerator.generateRideDistance(rideTime, directRideTime, directRideDistance);

			return new EstimatedDrtTeleportationInfo(waitTime + rideTime, waitTime, rideTime, rideDistance);
		}
	}

	public static class DistributionGenerator {
		private final Random random = new Random(4711);
		private final double estRideTimeAlpha;
		private final double estRideTimeBeta;
		private final double rideTimeStd;
		private final double estMeanWaitTime;
		private final double waitTimeStd;
		private final double probabilityRejection;

		public DistributionGenerator(double estRideTimeAlpha, double estRideTimeBeta, double rideTimeStd, double estMeanWaitTime,
									 double waitTimeStd, double probabilityRejection) {
			this.estRideTimeAlpha = estRideTimeAlpha;
			this.estRideTimeBeta = estRideTimeBeta;
			this.rideTimeStd = rideTimeStd;
			this.estMeanWaitTime = estMeanWaitTime;
			this.waitTimeStd = waitTimeStd;
			this.probabilityRejection = probabilityRejection;
		}

		public DistributionGenerator generateExampleDistributionGenerator() {
			return new DistributionGenerator(1.5, 300, 0.2, 300, 0.4, 0.0);
		}

		public double generateRideTime(double directRideTime) {
			// TODO improve this distribution
			double estMeanRideTime = estRideTimeAlpha * directRideTime + estRideTimeBeta;
			return Math.max(directRideTime, estMeanRideTime * (1 + random.nextGaussian() * rideTimeStd));
		}

		public double generateRideDistance(double estRideTime, double directRideTime, double directRideDistance) {
			// TODO Currently, same ratio is used as in the ride time estimation; improve this distribution
			double ratio = estRideTime / directRideTime;
			return ratio * directRideDistance;
		}

		public double generateWaitTime() {
			// TODO improve this distribution
			return Math.max(estMeanWaitTime * (1 + random.nextGaussian() * waitTimeStd), 0);
		}

		public boolean generateIsTripAccepted() {
			// TODO maybe incorporate this into the estimated wait time, ride time and ride distance
			return random.nextDouble() >= probabilityRejection;
		}
	}


}
