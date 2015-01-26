package org.matsim.contrib.freight.usecases.chessboard;

import jsprit.analysis.toolbox.Plotter;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PlotPlans {

    public static void main(String[] args) {
        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario).readFile("input/usecases/chessboard/network/grid9x9.xml");


        final Carriers carriers = new Carriers();
//		new CarrierPlanXmlReaderV2(carriers).read("input/usecases/chessboard/freight/singleCarrierTwentyActivities.xml");
        new CarrierPlanXmlReaderV2(carriers).read("output/ITERS/it.140/140.carrierPlans.xml");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).read("input/usecases/chessboard/freight/vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

        final Carrier carrier = carriers.getCarriers().get(Id.create("carrier1",Carrier.class));
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
        VehicleRoutingProblem vrp = vrpBuilder.build();

        VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(carrier.getSelectedPlan(), vrp);

        new Plotter(vrp,solution.getRoutes()).plot("output/plot_140", "carrier1");

    }

}
