package playground.dhosse.prt.launch;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.dvrp.extensions.electric.ElectricVehicleImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.data.TaxiRank;
import playground.michalm.taxi.data.file.TaxiRankReader;

public class PrtLauncher {
	
	public static void main(String args[]){
		
//		createVehicles();
		
		PrtParameters params = new PrtParameters(args);
		VrpLauncher launcher = new VrpLauncher(params);
		launcher.run();
		
//		OTFVis.playNetwork("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/network_pt.xml");
		
	}

	private static void createVehicles() {
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TaxiData data = new TaxiData();
		TaxiRankReader reader = new TaxiRankReader(scenario, data);
		reader.parse("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/taxiRanks.xml");
		
		List<Vehicle> vehicles = new ArrayList<Vehicle>();
		int vehPerRank = 2;
		
		for(TaxiRank rank : data.getTaxiRanks()){
			
			int cnt = 0;
			while(cnt < vehPerRank){
				Vehicle veh = new ElectricVehicleImpl(Id.create(rank.getLink().getId().toString()+"_"+cnt, Vehicle.class), rank.getLink(), 4, 0, 31*3600);
				vehicles.add(veh);
				cnt++;
			}
			
		}
		
		VehicleWriter writer = new VehicleWriter(vehicles);
		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/vehicles.xml");
		
	}
	
}
