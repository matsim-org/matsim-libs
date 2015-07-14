package playground.staheale.matsim2030;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class PTScheduleCleaner {
	private static Logger log = Logger.getLogger(PTScheduleCleaner.class);
	List<Id> newRouteLinkIds = new ArrayList<Id>();
	String oldStartTransitRouteId = null;
	String oldEndTransitRouteId = null;
	Id oldId = null;
	Id newRouteLinkId = null;
	int countRoutes = 0;
	int countTransitRoutes = 0;
	int countSameCoord = 0;
	int numberOfLines = 485544;
	double progress = 0;

	public static void main(String[] args) throws Exception {
		PTScheduleCleaner scheduleCleaner = new PTScheduleCleaner();
		scheduleCleaner.run();
	}

	public void run() throws Exception {

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		Network PTnetwork = sc.getNetwork();
		TransitSchedule PTschedule = sc.getTransitSchedule();

		// ------------------- read in PTnetwork ----------------------------
		log.info("Reading pt network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/02-OEV_DWV_2005-HAFAS_adaptedV2_ohne644.xml");
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		// ------------------- read in PTschedule ----------------------------
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/uvek2005schedule_with_routes.xml");
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");

		// ------------------- check if route has missing connections ----------------------------
		log.info("Start checking routes...");	
		for (TransitLine line : PTschedule.getTransitLines().values()) {
			countTransitRoutes += line.getRoutes().size();
			for (TransitRoute route : line.getRoutes().values()) {
				if (route.getRoute() == null) {
					log.warn("no route found for transit route " +route.getId());
				}
				else {
					for (int i=0 ; i < (route.getRoute().getLinkIds().size()-1) ; i++) {
						Id firstLink = route.getRoute().getLinkIds().get(i);
						Id secondLink = route.getRoute().getLinkIds().get(i+1);
						if (PTnetwork.getLinks().get(firstLink).getToNode().equals(PTnetwork.getLinks().get(secondLink).getFromNode()) != true) {
							log.warn("transit route " +route+ " has route with missing connections, firstLink: " +firstLink+ ", secondLink: " +secondLink);
						}
					}
				}
			}
		}
		log.info("Start checking routes...done");	

	}

}
