package playground.wrashid.parkingChoice.infrastructure.api;

import org.matsim.core.api.internal.MatsimFactory;

public interface ParkingInfrastructureFactory extends MatsimFactory {

	public abstract Parking createParking();
	
}
