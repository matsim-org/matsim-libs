package playground.wrashid.parkingChoice.infrastructure.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;

public interface Parking  extends Identifiable {

	public abstract String getType();
	
	public abstract Coord getCoord();
	
	public abstract int getCapacity();
	
	public abstract PriceScheme getPriceScheme();
}
