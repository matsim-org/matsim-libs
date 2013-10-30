package playground.qvanheerden.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierController;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.southafrica.utilities.Header;

import util.Solutions;

import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;


public class MyCarrierSimulation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MyCarrierSimulation.class.toString(), args);

		String configFile = args[0];
		String networkFile = args[1];
		String carrierPlanFile = args[2];
		String vehicleTypesFile = args[3];
		String initialPlanAlgorithm = args[4];
		String algorithm = args[5];

		Config config = ConfigUtils.loadConfig(configFile);
		//		config.addCoreModules();
		//		config.controler().setFirstIteration(0);
		//		config.controler().setLastIteration(1);
		//		config.addQSimConfigGroup(new QSimConfigGroup());
		//		config.getQSimConfigGroup().setStartTime(0);
		//		config.getQSimConfigGroup().setEndTime(144000);
		//		config.getQSimConfigGroup().setSnapshotStyle("queue");
		//		config.getQSimConfigGroup().setSnapshotPeriod(10);
		//		
		//		config.controler().setMobsim("qsim");
		//		List<String> snap = new ArrayList<String>();
		//		snap.add("otfvis");
		//		config.controler().setSnapshotFormat(snap);


		//Read network
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFile);


		//read carriers and their capabilities
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(carrierPlanFile);

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).read(vehicleTypesFile);

		//assign them to their corresponding vehicles - carriers already have vehicles in the carrier plan file
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);

		//get initial solution (using this from KnFreight2)
		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, scenario.getNetwork() ) ;
			NetworkBasedTransportCosts netBasedCosts =
					NetworkBasedTransportCosts.Builder.newInstance( scenario.getNetwork()
							, vehicleTypes.getVehicleTypes().values() ).build() ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build() ;

			VehicleRoutingAlgorithm vra = algorithms.VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem,initialPlanAlgorithm);

			VehicleRoutingProblemSolution solution = Solutions.getBest(vra.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
			// (maybe not optimal, but since re-routing is a matsim strategy, 
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan) ;

		}
		
		CarrierController carrierController = new CarrierController(carriers, null, null);
		Controler matsimController = new Controler(scenario);
		matsimController.addControlerListener(carrierController);
		matsimController.setOverwriteFiles(true);
		matsimController.run();

		Header.printFooter();


	}

}
