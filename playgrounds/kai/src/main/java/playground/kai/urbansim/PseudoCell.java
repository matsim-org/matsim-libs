package playground.kai.urbansim;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Helper class for PseudoGravityModel; has never been really tested since the PseudoGravityModel has never been really tested;
 * remains here since PseudoGravityModel remains in the package for the time being.
 * 
 * @author nagel
 *
 */
@Deprecated
public class PseudoCell {
	
	double xSum = 0. ; double xCnt = 0 ;
	double ySum = 0. ; double yCnt = 0 ;
	
	double nJobs = 0. ;
	double nWorkers = 0. ;
	
	private void addXY( double xx, double yy ) {
		xSum += xx ; xCnt++ ;
		ySum += yy ; yCnt++ ;
	}
	public double getX() {
		return xSum/xCnt ;  // FIXME: possibly NaN
	}
	public double getY() {
		return ySum/yCnt ;
	}
	public Coord getCoords() {
		Coord coord = new CoordImpl( getX(), getY() ) ;
		return coord ;
	}
	
	public void addJob( Coord cc ) {
		nJobs++ ;
		addXY( cc.getX(), cc.getY() ) ;
	}
	public void addWorker( Coord cc ) {
		nWorkers++ ;
		addXY( cc.getX(), cc.getY() ) ;
	}
	public double getNWorkers() {
		return nWorkers ;
	}
	public double getNJobs() {
		return nJobs ;
	}

}
