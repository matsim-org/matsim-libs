package playground.toronto.gtfsutils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import playground.toronto.demand.util.TableReader;


public class CreateMultipleVehicleTypesForSchedule {

	private final Vehicles vehicles;
	private final TransitSchedule schedule;
	private Map<Id<TransitRoute>, Id<VehicleType>> routeVehTypeMap;
	
	public CreateMultipleVehicleTypesForSchedule(final TransitSchedule schedule, final Vehicles vehicles){
		this.vehicles = vehicles;
		this.schedule = schedule;
	}
	
	public void ReadVehicleTypes(String filename) throws FileNotFoundException, IOException{
		
		TableReader tr = new TableReader(filename);
		tr.open();
		while (tr.next()){
			Id<VehicleType> i = Id.create(tr.current().get("veh_id"), VehicleType.class);
			VehicleTypeImpl type = new VehicleTypeImpl(i);
			
			type.setDescription(tr.current().get("description"));
			
			type.setAccessTime(Double.parseDouble(tr.current().get("access_time_pp")));
			
			VehicleCapacityImpl cap = new VehicleCapacityImpl();
			cap.setSeats(Integer.parseInt(tr.current().get("cap_seated")));
			cap.setStandingRoom(Integer.parseInt(tr.current().get("cap_stand")));
			type.setCapacity(cap);
			
			String opMode = tr.current().get("doors_op_type");
			if (opMode.equals("parallel")){
				type.setDoorOperationMode(DoorOperationMode.parallel);
			}else if (opMode.equals("serial")){
				type.setDoorOperationMode(DoorOperationMode.serial);
			}else{
				throw new IOException("Door operation mode not recognized");
			}
			
			type.setEgressTime(Double.parseDouble(tr.current().get("egress_time_pp")));
			
			type.setLength(Double.parseDouble(tr.current().get("length")));
			
			type.setPcuEquivalents(Double.parseDouble(tr.current().get("pce")));
			
			type.setWidth(Double.parseDouble(tr.current().get("width")));
			
			this.vehicles.addVehicleType( type);
		}
		tr.close();
	}
	
	public void ReadRouteVehicleMapping(String filename) throws FileNotFoundException, IOException{
		this.routeVehTypeMap = new HashMap<>();
		
		TableReader tr = new TableReader(filename);
		tr.open();
		while (tr.next()){
			Id<TransitRoute> routeId = Id.create(tr.current().get("route_id"), TransitRoute.class);
			Id<VehicleType> vehTypeId = Id.create(tr.current().get("vehicle_type"), VehicleType.class);
			
			this.routeVehTypeMap.put(routeId, vehTypeId);
		}
		tr.close();
	}
	
	public void run(){
		
		VehiclesFactory vb = this.vehicles.getFactory();
		
		HashMap<Id<VehicleType>, Integer> numberOfVehiclesByType = new HashMap<>();
		for (Id<VehicleType> i : this.vehicles.getVehicleTypes().keySet()) numberOfVehiclesByType.put(i, new Integer(0));

		for (TransitLine line : this.schedule.getTransitLines().values()) {
			Id<VehicleType> vehTypeId = this.routeVehTypeMap.get(line.getId());
			
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					String i = "VehType" + vehTypeId.toString() + "_" + numberOfVehiclesByType.get(vehTypeId);
					Vehicle v = vb.createVehicle(Id.create(i, Vehicle.class), this.vehicles.getVehicleTypes().get(vehTypeId));
					numberOfVehiclesByType.put(vehTypeId, numberOfVehiclesByType.get(vehTypeId) + 1);
					this.vehicles.addVehicle( v);
					departure.setVehicleId(v.getId());
				}
			}
		}
	}
	
	public void exportVehicles(String filename){
		VehicleWriterV1 writer = new VehicleWriterV1(vehicles);
		writer.writeFile(filename);
	}
}
