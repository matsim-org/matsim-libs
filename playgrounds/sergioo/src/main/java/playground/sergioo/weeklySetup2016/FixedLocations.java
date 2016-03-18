package playground.sergioo.weeklySetup2016;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class FixedLocations {

	private static enum ActivityType {
		LEISURE("leisure"),
		SHOP("shop"),
		PERSONAL("personal"),
		PUDO("pudo"),
		BUSINESS("biz");
		
		private String longName;
		
		private ActivityType(String longName) {
			this.longName = longName;
		}
		
		private static ActivityType getActivityTypeL(String longName) {
			for(ActivityType activityType:ActivityType.values())
				if(activityType.longName.equals(longName))
					return activityType;
			return null;
		}
		
	}

	private static final double WALK_SPEED = 4.0*1000/3600;
	private static final double WALK_BL = 1.3;
	
	public static void main(String[] args) throws SQLException, NoConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		DataBaseAdmin dataBaseAdmin  = new DataBaseAdmin(new File(args[0]));
		Map<String, Coord> coords = new HashMap<>();
		ResultSet result  = dataBaseAdmin.executeQuery("SELECT id, x, y, acttype  FROM ro_p_facilities.new_facilities");
		while(result.next())
			if(ActivityType.getActivityTypeL(result.getString(4))==null) {
				String id = result.getString(1);
				Coord facility = coords.get(id);
				if(facility==null)
					coords.put(id, new Coord(result.getDouble("x"), result.getDouble("y")));
			}
		Network allNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(allNetwork).readFile(args[1]);
		Set<String> carMode = new HashSet<String>();
		carMode.add("car");
		NetworkImpl network = (NetworkImpl) NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(allNetwork).filter(network, carMode);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(args[2]);
		TransitRouterNetwork networkPT = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.transitRouter().getMaxBeelineWalkConnectionDistance());
		PrintWriter writer = new PrintWriter(args[3]);
		Set<String> locs = new HashSet<>();
		result = dataBaseAdmin.executeQuery("SELECT persid,postcode FROM ro_population.persons_compact");
		while(result.next()) {
			String id = result.getString("postcode");
			if(!locs.contains(id)) {
				Coord coord = coords.get(id);
				Node carNode = ((NetworkImpl)network).getNearestNode(coord);
				TransitRouterNetworkNode ptNode = networkPT.getNearestNode(coord);
				writer.println(id+","+coord.getX()+","+coord.getY()+","+carNode.getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, carNode.getCoord())*WALK_BL/WALK_SPEED+","+ptNode.getStop().getStopFacility().getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, ptNode.getCoord())*WALK_BL/WALK_SPEED);
				locs.add(id);
			}
		}
		result = dataBaseAdmin.executeQuery("SELECT persid,facility_id FROM ro_mainactfacilityassignment.work_assignment");
		while(result.next()) {
			String id = result.getString("facility_id");
			if(!locs.contains(id)) {
				Coord coord = coords.get(id);
				Node carNode = ((NetworkImpl)network).getNearestNode(coord);
				TransitRouterNetworkNode ptNode = networkPT.getNearestNode(coord);
				writer.println(id+","+coord.getX()+","+coord.getY()+","+carNode.getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, carNode.getCoord())*WALK_BL/WALK_SPEED+","+ptNode.getStop().getStopFacility().getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, ptNode.getCoord())*WALK_BL/WALK_SPEED);
				locs.add(id);
			}
		}
		result = dataBaseAdmin.executeQuery("SELECT persid,schoolpostalcode FROM ro_p_schoollocationchoice.schooldestinations_it_10");
		while(result.next()) {
			String id = result.getString("schoolpostalcode");
			if(!locs.contains(id)) {
				Coord coord = coords.get(id);
				Node carNode = ((NetworkImpl)network).getNearestNode(coord);
				TransitRouterNetworkNode ptNode = networkPT.getNearestNode(coord);
				writer.println(id+","+coord.getX()+","+coord.getY()+","+carNode.getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, carNode.getCoord())*WALK_BL/WALK_SPEED+","+ptNode.getStop().getStopFacility().getId().toString()+","+CoordUtils.calcEuclideanDistance(coord, ptNode.getCoord())*WALK_BL/WALK_SPEED);
				locs.add(id);
			}
		}
		writer.close();
	}

}
