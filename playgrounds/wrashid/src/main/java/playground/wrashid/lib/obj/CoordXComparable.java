package playground.wrashid.lib.obj;



/**
 * These coordinates are comparable regarding the x coordinate.
 * @author wrashid
 *
 */
public class CoordXComparable extends Coord implements Comparable<CoordXComparable> {	
	public CoordXComparable(double x, double y) {
		super(x, y);
	}

	public int compareTo(CoordXComparable o) {
		if (getX() > o.getX() ){
			return 1;
		} else if (getX() < o.getX()){
			return -1;
		}
		return 0;
	}
	
}
