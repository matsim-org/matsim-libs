package playground.tobiqui.master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class TqMatsimVehiclesReader {
	protected List<Vehicle> vehicles = new ArrayList<>();
	protected List<VehicleType> vehicleTypes = new ArrayList<>();
	protected Vehicles v;

	public TqMatsimVehiclesReader(String fileName) {
		this.v = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(this.v).readFile(fileName);
	}
	
	public Map<Id<Vehicle>, Vehicle> getVehicles() {
		
		return v.getVehicles();
	}
	
	public List<VehicleType> getVehicleTypes() {
		
		return new ArrayList<VehicleType>(v.getVehicleTypes().values());
	}
}
