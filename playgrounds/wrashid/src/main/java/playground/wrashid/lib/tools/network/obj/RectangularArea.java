package playground.wrashid.lib.tools.network.obj;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.parking.lib.GeneralLib;



public class RectangularArea {

	private final Coord rectangleCornerOne;
	private final Coord oppoSiteCorner;

	public RectangularArea(Coord rectangleCornerOne, Coord oppoSiteCorner) {
		super();
		this.rectangleCornerOne = rectangleCornerOne;
		this.oppoSiteCorner = oppoSiteCorner;
	}
	
	public boolean isInArea(Coord coordinateToCheck){
		if (isXCoordinateOk(coordinateToCheck) && isYCoordinateOk(coordinateToCheck)){
			return true;
		}
		return false;
	}
	
	private boolean isXCoordinateOk(Coord coordinateToCheck){
		return GeneralLib.isNumberInBetween(rectangleCornerOne.getX(), oppoSiteCorner.getX(), coordinateToCheck.getX()) || coordinateToCheck.getX()==rectangleCornerOne.getX() || coordinateToCheck.getX()==oppoSiteCorner.getX();
	}
	
	private boolean isYCoordinateOk(Coord coordinateToCheck){
		return GeneralLib.isNumberInBetween(rectangleCornerOne.getY(), oppoSiteCorner.getY(), coordinateToCheck.getY()) || coordinateToCheck.getY()==rectangleCornerOne.getY() || coordinateToCheck.getY()==oppoSiteCorner.getY();
	}
	
	
	
}
