package saleem.stockholmscenario.teleportation;

import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

public class StockholmScenarioSimulation {
public static void main(String[] args) {
        String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
        Controler controler = new Controler(config);
        controler.addControlerListener(new StockholmControlListener());
		Scenario scenario = controler.getScenario();
        Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		//network.getLinks().clear();
		//network.getNodes().clear();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
		Iterator iter = network.getLinks().values().iterator();
		while(iter.hasNext()){
			Link link = (Link)iter.next();
			if(link.getId().toString().startsWith("tr")){
				//link.setCapacity(link.getCapacity()*10);
			}
		}
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
		//TransitScheduleWriter tw = new TransitScheduleWriter(schedule);
		//tw.writeFile("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoSchedule.xml");
		
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
