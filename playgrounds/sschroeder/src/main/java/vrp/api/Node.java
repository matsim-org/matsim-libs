package vrp.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface Node {

	public abstract Coord getCoord();

	public abstract void setCoord(Coord coord);
	
	public abstract Id getId();

	public abstract void setMatrixId(int matrixId);
	
	public abstract int getMatrixId();

}