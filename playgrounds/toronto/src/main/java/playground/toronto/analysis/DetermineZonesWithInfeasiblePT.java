package playground.toronto.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.toronto.demand.util.TableReader;

/**
 * Reads a transit schedule file, and a file of zone centroid coordinates,
 * and determines which zones have "no public transit service." 
 * 
 * @author pkucirek
 *
 */
public class DetermineZonesWithInfeasiblePT {

	//private static TransitSchedule schedule;
	private HashSet<Node> zones;
	private Network stops;
	private static final double maxSearchRadius = 1500.0; //1.5 km radius		
		
	public DetermineZonesWithInfeasiblePT(){
	}
	
	private boolean isCoordinateFeasible(Coord coord){
		final Coord coord1 = coord;
		Collection<Node> N = NetworkUtils.getNearestNodes(this.stops,coord1, maxSearchRadius);
		return (N.size() > 0);
	}
	
	public void run(){
		for (Node n : this.zones){
			if (!isCoordinateFeasible(n.getCoord())){
				System.out.println("Zone " + n.getId() + " is more than " + maxSearchRadius + " m from a transit stop!");
			}
		}
	}
	
	public void loadSchedule(String filename){
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", filename);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		this.stops = NetworkUtils.createNetwork();
		for (TransitStopFacility stop : schedule.getFacilities().values()){
			Id<TransitStopFacility> stopId = stop.getId();
			Node n = NetworkUtils.createNode(Id.create(stopId, Node.class));
			n.setCoord(stop.getCoord());
			stops.addNode(n);
		}
	}
	
	public void loadZones(String filename) throws FileNotFoundException, IOException{		
		this.zones = new HashSet<Node>();
		TableReader tr = new TableReader(filename);
		tr.open();
		while (tr.next()){
			Id<Node> zid = Id.create(tr.current().get("zone_id"), Node.class);
			Coord coord = new Coord(Double.parseDouble(tr.current().get("x")), Double.parseDouble(tr.current().get("y")));
			Node n = NetworkUtils.createNode(zid);
			n.setCoord(coord);
			zones.add(n);
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		String zonesFile = args[0];
		String scheduleFile = args[1];
		
		DetermineZonesWithInfeasiblePT checker = new DetermineZonesWithInfeasiblePT();
		checker.loadZones(zonesFile);
		checker.loadSchedule(scheduleFile);
		
		checker.run();
	}
	
}
