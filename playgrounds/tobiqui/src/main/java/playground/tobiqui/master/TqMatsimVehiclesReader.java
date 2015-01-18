package playground.tobiqui.master;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class TqMatsimVehiclesReader {
	protected Map<Id<Vehicle>, Vehicle> vehicles = new HashMap<Id<Vehicle>, Vehicle>();
	protected Map<Id<VehicleType>, VehicleType> vehicleTypes = new HashMap<Id<VehicleType>, VehicleType>();
	protected Vehicles v;

	public TqMatsimVehiclesReader(String fileName) {
		this.v = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(this.v).readFile(fileName);
	}
	
	public Map<Id<Vehicle>, Vehicle> getVehicles() {
		vehicles = v.getVehicles();
		
		return vehicles;
	}
	
	public Map<Id<VehicleType>, VehicleType> getVehicleTypes() {
		vehicleTypes = v.getVehicleTypes();
		
		return vehicleTypes;
	}
}
