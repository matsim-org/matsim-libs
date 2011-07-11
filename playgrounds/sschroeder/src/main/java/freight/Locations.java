package freight;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface Locations {

	public abstract Coord getCoord(Id id);

}