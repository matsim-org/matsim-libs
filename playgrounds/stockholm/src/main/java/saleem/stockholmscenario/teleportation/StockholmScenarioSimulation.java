package saleem.stockholmscenario.teleportation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterFactoryImpl;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.p0.P0ControlListener;

public class StockholmScenarioSimulation {
public static void main(String[] args) {
		System.out.println("Saleem");
        String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
        Controler controler = new Controler(config);
        Scenario scenario = controler.getScenario();
        
        Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		//network.getLinks().clear();
		//network.getNodes().clear();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
		
		controler.run();
        /*Config config = ConfigUtils.loadConfig(path, new MatrixBasedPtRouterConfigGroup());
        
        //fetching relevant groups ot of the config
        MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
        PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();
        

        //setting up scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Bounding Box for PT Matrix - may be scaled down to a smaller area
        BoundingBox nbb = BoundingBox.createBoundingBox(scenario.getNetwork());
        // setting up PT Matrix
        PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, nbb, mbpcg);
        
        //and finally setting up the controler
        Controler controler = new Controler(config);
        // setting up routing 
        controler.setTripRouterFactory( new MatrixBasedPtRouterFactoryImpl(controler.getScenario(), ptMatrix) ); // the car and pt router
        
        controler.run();*/
	}
}
