package playground.wrashid.lib.tools.network.obj;

import org.matsim.api.core.v01.Coord;

import playground.wrashid.lib.GeneralLib;


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
		return GeneralLib.isNumberInBetween(rectangleCornerOne.getX(), oppoSiteCorner.getX(), coordinateToCheck.getX());
	}
	
	private boolean isYCoordinateOk(Coord coordinateToCheck){
		return GeneralLib.isNumberInBetween(rectangleCornerOne.getY(), oppoSiteCorner.getY(), coordinateToCheck.getY());
	}
	
	
	
}
