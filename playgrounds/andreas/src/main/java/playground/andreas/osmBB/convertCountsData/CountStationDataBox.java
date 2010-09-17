package playground.andreas.osmBB.convertCountsData;

import org.matsim.api.core.v01.Coord;

public class CountStationDataBox {
	private String shortName;
	private String unitName;
	private Coord coord;
	private String position;
	private String positionDetail;
	private String direction;
	private String orientation;
	private int numberOfLanesDetected;
	private int errorCodeFromMapping;
	
	protected String getShortName() {
		return this.shortName;
	}
	protected void setShortName(String shortName) {
		this.shortName = shortName;
	}
	protected String getUnitName() {
		return this.unitName;
	}
	protected void setUnitName(String unitName) {
		this.unitName = unitName;
	}
	protected Coord getCoord() {
		return this.coord;
	}
	protected void setCoord(Coord coord) {
		this.coord = coord;
	}
	protected String getPosition() {
		return this.position;
	}
	protected void setPosition(String position) {
		this.position = position;
	}
	protected String getPositionDetail() {
		return this.positionDetail;
	}
	protected void setPositionDetail(String positionDetail) {
		this.positionDetail = positionDetail;
	}
	protected String getDirection() {
		return this.direction;
	}
	protected void setDirection(String direction) {
		this.direction = direction;
	}
	protected String getOrientation() {
		return this.orientation;
	}
	protected void setOrientation(String orientation) {
		this.orientation = orientation;
	}
	protected int getNumberOfLanesDetected() {
		return this.numberOfLanesDetected;
	}
	protected void setNumberOfLanesDetected(int numberOfLanesDetected) {
		this.numberOfLanesDetected = numberOfLanesDetected;
	}
	protected int getErrorCodeFromMapping() {
		return this.errorCodeFromMapping;
	}
	protected void setErrorCodeFromMapping(int errorCodeFromMapping) {
		this.errorCodeFromMapping = errorCodeFromMapping;
	}
	
	@Override
	public String toString() {
		return "Count Station " + getShortName() + ", " + getUnitName() 
		+ " at " + getCoord() + " " + getPosition() 
		+ " " + getPositionDetail() + " " + getOrientation() 
		+ " with " + getNumberOfLanesDetected() + " lanes";
	}	
	
}
