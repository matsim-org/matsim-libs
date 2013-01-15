package org.matsim.contrib.freight.vrp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesCostAndTWs;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesLocalActInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesShipmentInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.StandardRouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

public class MatsimVrpSolverImplTest extends MatsimTestCase{
	
	Carriers carriers;
	
	Network network;
	
	Controler controler;
	
	public void setUp() throws Exception{
		super.setUp();
		carriers = new Carriers();
		new CarrierPlanReader(carriers).read(getInputDirectory() + "carrierPlansEquils.xml");
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(getInputDirectory() + "network.xml");
		network = scenario.getNetwork();
		
		controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
		controler.setCreateGraphs(false);
	}
	
	public void testSelectedPlanIsInitialSolution(){
		TravelDisutility travelCost = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time,Person person, org.matsim.vehicles.Vehicle vehicle) {
				return link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return 0;
			}
		};
		
		TravelTime travelTime = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time,Person person, org.matsim.vehicles.Vehicle vehicle) {
				return link.getLength()/link.getFreespeed();
			}
			
		};
		final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(network, travelCost, travelTime);
		TransportCostCalculator tcc = new TransportCostCalculator(router,network,900);
		MatsimVrpSolverImpl solver = new MatsimVrpSolverImpl(carriers.getCarriers().values().iterator().next(), tcc);
		TourCost tourCost = new TourCost() {
			
			@Override
			public double getTourCost(TourImpl tour, Driver driver, Vehicle vehicle) {
				return tour.tourData.totalCost;
			}
		};
		solver.setVrpSolverFactory(new RuinAndRecreateStandardAlgorithmFactory(
				new StandardRouteAgentFactory(new CalculatesShipmentInsertion(tcc, new CalculatesLocalActInsertion(tcc)), new CalculatesCostAndTWs(tcc))));
		solver.useSelectedPlanAsInitialSolution(true);
		solver.solve();
		
		assertTrue(true);
		
		
	}

}
