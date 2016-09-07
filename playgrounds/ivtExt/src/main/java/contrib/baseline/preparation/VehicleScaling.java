package contrib.baseline.preparation;

import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class VehicleScaling {

	public static void main(String[] args) {
		if (args.length < 3) {
			throw new RuntimeException("Usage: original_vehicles.xml scaled_vehicles.xml 0.1/0.01");
		}
		
		String sourcePath = args[0];
		String targetPath = args[1];
		double factor = Double.parseDouble(args[2]);
		
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(vehicles).readFile(sourcePath);
		
		for (VehicleType type : vehicles.getVehicleTypes().values()) {
			VehicleCapacity capacity = type.getCapacity();
			
			capacity.setSeats((int)Math.ceil(capacity.getSeats() * factor));
			capacity.setStandingRoom((int)Math.ceil(capacity.getStandingRoom() * factor));
			
			type.setPcuEquivalents(type.getPcuEquivalents() * factor);
		}
		
		new VehicleWriterV1(vehicles).writeFile(targetPath);
	}
}
