package playground.wrashid.bsc.vbmh.SFAnpassen;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;

public class wasKannDasScenario {
	
	public static void main(String[] args){
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_3.xml"));
		//for (Household haushalt : scenario.getHouseholds().getHouseholds().values()){
			//System.out.println(haushalt.getVehicleIds().size());
		//}
		
		for(Vehicle vehicle : scenario.getVehicles().getVehicles().values()){
			//System.out.println(vehicle.getType().getId());
		}
		
	}
	

}
