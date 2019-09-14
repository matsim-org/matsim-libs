package org.matsim.contrib.freight.usecases.chessboard;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

final class PlotPlans {

    public static void main(String[] args) {
        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9.xml");


        final Carriers carriers = new Carriers();
//		new CarrierPlanXmlReader(carriers).read("input/usecases/chessboard/freight/singleCarrierTwentyActivities.xml");
        new CarrierPlanXmlReader(carriers).readFile("output/ITERS/it.140/140.carrierPlans.xml" );

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).readFile("input/usecases/chessboard/freight/vehicleTypes.xml");
        new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

        final Carrier carrier = carriers.getCarriers().get(Id.create("carrier1",Carrier.class));
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
        VehicleRoutingProblem vrp = vrpBuilder.build();

        VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(carrier.getSelectedPlan(), vrp);

        new Plotter(vrp,solution.getRoutes()).plot("output/plot_140", "carrier1");

    }

}
