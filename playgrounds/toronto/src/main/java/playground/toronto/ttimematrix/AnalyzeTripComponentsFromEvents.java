package playground.toronto.ttimematrix;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.analysis.ODMatrix;
import playground.toronto.analysis.handlers.AgentTripChainHandler;
import playground.toronto.demand.util.TableReader;
import playground.toronto.mapping.Link2ZoneMap;

/**
 * Gets OD matrices for trip components from an events file.
 * 
 * @author pkucirek
 *
 */
public class AnalyzeTripComponentsFromEvents {

	private static Network network;
	private static TransitSchedule schedule;
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		String eventsFile= args[0];
		String networkFile = args[1];
		String scheduleFile = args[2];
		String zonesFile = args[3];
		String emmeFilepath = args[4];
		
		loadFiles(networkFile, scheduleFile, zonesFile);
		
		Link2ZoneMap linkZoneMap = new Link2ZoneMap();
		linkZoneMap.run(network);
		
		AgentTripChainHandler atch = new AgentTripChainHandler();
		atch.setLinkZoneMap(linkZoneMap);
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(atch);
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		double amPeriodEnd = Time.parseTime("08:59:59");
		double middayEnd = Time.parseTime("14:59:59");
		double pmPeriodEnd = Time.parseTime("18:59:59");
		double eveningEnd = Time.parseTime("21:59:59");
		
		ODMatrix transitWalkAM = atch.getAvgTransitWalkTimeODM(0, amPeriodEnd, false);
		ODMatrix transitWaitAM = atch.getAvgTransitWaitTimeODM(0, amPeriodEnd, false);
		ODMatrix transitIvttAM = atch.getAvgTransitInVehicleTimeODM(0, amPeriodEnd, false);
		
		try {
			transitIvttAM.exportAs311File(emmeFilepath + "\transit_ivtt_am.311", 66, "ivttam");
			transitWaitAM.exportAs311File(emmeFilepath + "\transit_wait_am.311", 67, "waitam");
			transitWalkAM.exportAs311File(emmeFilepath + "\transit_walk_am.311", 68, "auxtam");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void loadFiles(String networkFile, String scheduleFile, String zonesFile) throws FileNotFoundException, IOException{
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		schedule = scenario.getTransitSchedule();
		network = scenario.getNetwork();
		
		TableReader tr = new TableReader(zonesFile);
		tr.open();
		while (tr.next()){
			Id<Node> zoneId = Id.create(tr.current().get("zone_id"),Node.class);
			double x = Double.parseDouble(tr.current().get("x"));
			double y = Double.parseDouble(tr.current().get("y"));
			
			NodeImpl n = new NodeImpl(zoneId);
			n.setCoord(new CoordImpl(x, y));
			network.addNode(n);
		}
		tr.close();
	}
	
}
