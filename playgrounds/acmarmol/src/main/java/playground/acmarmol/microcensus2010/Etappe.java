package playground.acmarmol.microcensus2010;
import org.matsim.api.core.v01.Coord;


public class Etappe {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////
		
	private int departureTime;
	private int arrivalTime;
	private Coord startCoord;
	private Coord endCoord;
	private String mode;
	private String carType;


//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public Etappe(int departureTime, int arrivalTime, Coord startCoord, Coord endCoord, String mode) {

	this.setArrivalTime(arrivalTime);
	this.setDepartureTime(departureTime);
	this.setStartCoord(startCoord);
	this.setEndCoord(endCoord);
	this.setMode(mode);
		
		
	}

//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////

	public int getDepartureTime() {
		return departureTime;
	}


	public void setDepartureTime(int departureTime) {
		this.departureTime = departureTime;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(int arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public Coord getStartCoord() {
		return startCoord;
	}

	public void setStartCoord(Coord startCoord) {
		this.startCoord = startCoord;
	}

	public Coord getEndCoord() {
		return endCoord;
	}

	public void setEndCoord(Coord endCoord) {
		this.endCoord = endCoord;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getCarType() {
		return carType;
	}

	public void setCarType(String carType) {
		this.carType = carType;
	}	


	
	
}
