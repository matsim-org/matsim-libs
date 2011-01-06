package playground.jjoubert.digicoreNew.containers;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class DigicoreVehicle implements Vehicle {
	private Id id;
	private VehicleType type;
	private List<DigicoreChain> chains;
	
	public DigicoreVehicle() {
		
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public VehicleType getType() {
		return this.type;
	}
	
	public List<DigicoreChain> getChains(){
		return this.chains;
	}

}
