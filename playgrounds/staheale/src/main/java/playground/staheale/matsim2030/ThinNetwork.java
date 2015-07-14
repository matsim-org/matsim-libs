package playground.staheale.matsim2030;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ThinNetwork {

private static Logger log = Logger.getLogger(ThinNetwork.class);
	
	int nId = 0;
	List<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();

	public static void main(String[] args) throws Exception {
		
		ThinNetwork thinNetwork = new ThinNetwork();
		thinNetwork.run();
		
	}

	public void run() throws Exception {
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		Network PTnetwork = sc.getNetwork();
		TransitSchedule PTschedule = sc.getTransitSchedule();
		
		ScenarioImpl scWrite = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scWrite.getConfig().transit().setUseTransit(true);
		scWrite.getConfig().scenario().setUseVehicles(true);
		TransitSchedule newPTschedule = scWrite.getTransitSchedule();
		
		TransitScheduleFactory scheduleFactory = newPTschedule.getFactory();
		NetworkFactory factory = PTnetwork.getFactory();

		//////////////////////////////////////////////////////////////////////
		// read in PTnetwork
		
		log.info("Reading pt network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/uvek2005network_adjusted.xml.gz");
		log.info("Reading pt network...done.");
		log.info("Network contains " +PTnetwork.getLinks().size()+ " links and " +PTnetwork.getNodes().size()+ " nodes.");

		//////////////////////////////////////////////////////////////////////
		// read in PTschedule
		
		log.info("Reading pt schedule...");	
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile("./input/uvek2005schedule_adjusted.xml.gz");
		log.info("Reading pt schedule...done.");
		log.info("Schedule contains " +PTschedule.getTransitLines().size()+ " lines.");
		
		//////////////////////////////////////////////////////////////////////
		// change name of stop facilities

		log.info("Start adjusting network and schedule...");
		for (TransitLine line : PTschedule.getTransitLines().values()){
			TransitLine nLine = scheduleFactory.createTransitLine(line.getId());
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop rStop : route.getStops()) {
					
					TransitStopFacility fac = rStop.getStopFacility();
					nId += 1;
					Id<TransitStopFacility> newFacId = Id.create(line.getId().toString().replaceAll("\\s","")+"."+nId, TransitStopFacility.class);
					TransitStopFacility newFac = scheduleFactory.createTransitStopFacility(newFacId, fac.getCoord(), false);
					newFac.setLinkId(fac.getLinkId());
					newFac.setName(fac.getName());
					rStop.setStopFacility(newFac);
					routeStops.add(rStop);
					
				}

				// create transit route
				TransitRoute nTransitRoute = scheduleFactory.createTransitRoute(route.getId(), route.getRoute(), routeStops, "pt");
				// copy departures
				for (Departure dep : route.getDepartures().values()) {
					nTransitRoute.addDeparture(dep);
				}
				// add route to line
				nLine.addRoute(nTransitRoute);
				routeStops.clear();
				
			}
			// add line to schedule
			newPTschedule.addTransitLine(nLine);
			nId = 0;
		}
		
		//////////////////////////////////////////////////////////////////////
		// thin network
	
		

		
		NetworkWriter nw = new NetworkWriter(PTnetwork);
		nw.write("./output/uvek2005network_adjusted_final.xml.gz");

		TransitScheduleWriter sw = new TransitScheduleWriter(newPTschedule);
		sw.writeFile("./output/uvek2005schedule_adjusted_final.xml.gz");
		
	}

}
