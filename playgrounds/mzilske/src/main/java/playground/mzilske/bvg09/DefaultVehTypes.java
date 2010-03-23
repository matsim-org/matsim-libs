package playground.mzilske.bvg09;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

public class DefaultVehTypes {
	
	public static Map<String, BasicVehicleType> getDefaultVehicleTypes(){
		
		Map<String, BasicVehicleType> vehMap = new HashMap<String, BasicVehicleType>();
		
		/** The following pt line types exist:
		 * "B" Bus Berlin
		 * "F" Fuss
		 * "P" Bus Umland
		 * "R" Regionalbahn
		 * "S" S-Bahn
		 * "T" Tram Berlin
		 * "U" U-Bahn
		 * "V" Tram Umland
		 */
		
		vehMap.put("B", getBusBerlin("B"));
		vehMap.put("P", getBusUmland("P"));
		
		vehMap.put("T", getTramBerlin("T"));
		vehMap.put("V", getTramUmland("V"));
		
		vehMap.put("U", getUBahn("U"));
		vehMap.put("S", getSBahn("S"));
		
		vehMap.put("R", getRegionalBahn("R"));
		
		return vehMap;
	}
	
	private static BasicVehicleType getBusBerlin(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));

		veh.setDescription("Standard 3 doors");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(30));
		cap.setStandingRoom(new Integer(60));
		veh.setCapacity(cap);
		
		veh.setLength(12.0);
		veh.setWidth(2.55);
		veh.setMaximumVelocity(23.6); // 85 km/h
		
		veh.setAccessTime(2.0);
		veh.setEgressTime(0.5);
		
		return veh;
	}
	
	private static BasicVehicleType getBusUmland(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));

		veh.setDescription("Standard 2 doors");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(30));
		cap.setStandingRoom(new Integer(70));
		veh.setCapacity(cap);
		
		veh.setLength(12.0);
		veh.setWidth(2.55);
		veh.setMaximumVelocity(22.2); // 80 km/h
		
		veh.setAccessTime(2.0);
		veh.setEgressTime(1.0); // just one door
		
		return veh;
	}
	
	private static BasicVehicleType getTramBerlin(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));
		
		veh.setDescription("GT6, one wagon");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(45));
		cap.setStandingRoom(new Integer(103));
		veh.setCapacity(cap);
		
		veh.setLength(28.27);
		veh.setWidth(2.30);
		veh.setMaximumVelocity(19.44); // 70 km/h
		
		veh.setAccessTime(0.5);
		veh.setEgressTime(0.5);
		
		return veh;
	}
	
	private static BasicVehicleType getTramUmland(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));
		
		veh.setDescription("Tatra long KT4D mod, one wagon");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(33));
		cap.setStandingRoom(new Integer(66));
		veh.setCapacity(cap);
		
		veh.setLength(19.05);
		veh.setWidth(2.30);
		veh.setMaximumVelocity(16.67); // 60 km/h
		
		veh.setAccessTime(1.0); // hochflur
		veh.setEgressTime(1.0); // hochflur
		
		return veh;
	}
	
	private static BasicVehicleType getUBahn(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));
		
		veh.setDescription("Baureihe F gross, 6 wagon unit");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(228));
		cap.setStandingRoom(new Integer(477));
		veh.setCapacity(cap);
		
		veh.setLength(96.3);
		veh.setWidth(2.65);
		veh.setMaximumVelocity(19.44); // 70 km/h
		
		veh.setAccessTime(0.05); // 6 wagons * 3 doors
		veh.setEgressTime(0.05);
		
		return veh;
	}
	
	private static BasicVehicleType getSBahn(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));
		
		veh.setDescription("Baureihe 481, 8 wagon unit");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(376));
		cap.setStandingRoom(new Integer(800));
		veh.setCapacity(cap);
		
		veh.setLength(147.2);
		veh.setWidth(3.00);
		veh.setMaximumVelocity(27.78); // 100 km/h
		
		veh.setAccessTime(0.04); // 8 wagons * 3 doors
		veh.setEgressTime(0.04);
		
		return veh;
	}
	
	private static BasicVehicleType getRegionalBahn(String id){
		BasicVehicleType veh = new BasicVehicleTypeImpl(new IdImpl(id));
		
		veh.setDescription("Baureihe DBpza 752 RE160, 5 wagon unit");
		
		BasicVehicleCapacityImpl cap = new BasicVehicleCapacityImpl();
		cap.setSeats(new Integer(578)); // 4*121 + 1*94
		cap.setStandingRoom(new Integer(250)); // maybe 50p per wagon
		veh.setCapacity(cap);
		
		veh.setLength(150.0); // 5*26,8 + Engine
		veh.setWidth(3.00);
		veh.setMaximumVelocity(44.44); // 160 km/h
		
		veh.setAccessTime(0.125); // 4 wagons * 2 doors
		veh.setEgressTime(0.125);
		
		return veh;
	}

}
