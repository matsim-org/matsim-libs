package playground.wrashid.bsc.vbmh.SFAnpassen;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.bsc.vbmh.vm_parking.Parking;
import playground.wrashid.bsc.vbmh.vm_parking.Parking_Map;
import playground.wrashid.bsc.vbmh.vm_parking.Parking_writer;
public class create_demo_parking {

	public static void main(String[] args) {
		double privat_anteil_p_gesammt=0.2;
		double privat_anteil_EV=0.2;
		double public_anteil_p_gesammt=0.05;
		double public_anteil_EV=0.2;
		int i = 0;
		double zufallsz;
		Parking_Map parking_map = new Parking_Map();
		Parking_writer writer = new Parking_writer();
		Random zufall = new Random();
	
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_1.xml"));
		
		
		// P R I V A T E --------------------------------------------
		
		Map<Id, ? extends ActivityFacility> facility_map = scenario.getActivityFacilities().getFacilities();
	
		for (Facility facility : facility_map.values()){
			ActivityFacilityImpl actfacility = (ActivityFacilityImpl) facility;
			for(String key : actfacility.getActivityOptions().keySet()){
				ActivityOption activity = actfacility.getActivityOptions().get(key);
				
				double location_capacity= activity.getCapacity();
				CoordImpl location_coord = (CoordImpl) actfacility.getCoord();
				IdImpl location_id=(IdImpl)facility.getId();
				
				
				if (location_capacity>1000){
					System.out.println(location_capacity);
				}
				
				
				
				Parking parking = new Parking();
				parking.capacity_ev=Math.round(location_capacity * privat_anteil_p_gesammt * privat_anteil_EV);
				parking.capacity_nev=Math.round(location_capacity * privat_anteil_p_gesammt * (1-privat_anteil_EV));
				
				// Facilitys ohne capacity Angabe haben intern unendlich >> Bessere loesung suchen!
				if (parking.capacity_ev>1000){
					System.out.println("Null gesetzt");
					parking.capacity_ev=0;
				}
				if (parking.capacity_nev>1000){
					parking.capacity_nev=0;
				}
				
				
				parking.set_coordinate(location_coord);
				parking.facility_id=location_id.toString();
				parking.id=i;
				parking.type="private";
				
				parking_map.addParking(parking);
				i++;
			}
		}
		
		
		
		
		// P U B L I C -------------------------------------
		
		for (Link link : scenario.getNetwork().getLinks().values()){
			double link_length = link.getLength();
			Coord link_coord=link.getFromNode().getCoord();
			double location_capacity = link_length/10;
			Parking parking = new Parking();
			parking.capacity_ev=Math.round(location_capacity * public_anteil_p_gesammt * public_anteil_EV);
			parking.capacity_nev=Math.round(location_capacity * public_anteil_p_gesammt * (1-public_anteil_EV));
			if (parking.capacity_ev>1000){
				parking.capacity_ev=100;
			}
			if (parking.capacity_nev>1000){
				parking.capacity_nev=100;
			}
			
			
			parking.set_coordinate(link_coord);
			parking.id=i;
			parking.type="public";
			zufallsz=zufall.nextDouble();
			if (zufallsz<0.2){
				parking.parking_pricem=0;
			} else{
				parking.parking_pricem=3;
			}
				
			parking_map.addParking(parking);
			i++;
			
		}
		
		
		
		
		
		
		
		
		writer.write(parking_map, "input/parkings_demo.xml");
		System.out.println("feddisch");

	}

}
