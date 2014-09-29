package playground.wrashid.parkingChoice.infrastructure.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;

public interface Parking  extends Identifiable<Parking> {

	public abstract String getType();
	
	public abstract void setType(String parkingType);
	
	public abstract Coord getCoord();
	
	public abstract double getCapacity();
	
	public abstract int getIntCapacity();
	
	public abstract void setCapacity(double capacity);
	
	public abstract PriceScheme getPriceScheme();
}
