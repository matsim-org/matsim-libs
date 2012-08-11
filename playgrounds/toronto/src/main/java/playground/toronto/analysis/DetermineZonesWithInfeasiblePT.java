package playground.toronto.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.demand.util.TableReader;

/**
 * Reads a transit schedule file, and a file of zone centroid coordinates,
 * and determines which zones have "no public transit service." 
 * 
 * @author pkucirek
 *
 */
public class DetermineZonesWithInfeasiblePT {

	private static TransitSchedule schedule;
	private static NetworkImpl zones;
	
	
	private static double getDistancToNearestStop(Coord coord){
		double dist = Double.MAX_VALUE;
		
		
		return dist;
	}
	
	private static void loadSchedule(String filename){
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", filename);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		schedule = scenario.getTransitSchedule();
	}
	
	private static void loadZones(String filename) throws FileNotFoundException, IOException{
		zones = NetworkImpl.createNetwork();
		
		TableReader tr = new TableReader(filename);
		tr.open();
		while (tr.next()){
			Id zid = new IdImpl(tr.current().get("zone_id"));
			CoordImpl coord = new CoordImpl(tr.current().get("x"), tr.current().get("y"));
			NodeImpl n = new NodeImpl(zid);
			n.setCoord(coord);
			zones.addNode(n);
		}
	}
}
