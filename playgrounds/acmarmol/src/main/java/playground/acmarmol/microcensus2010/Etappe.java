package playground.acmarmol.microcensus2010;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;


public class Etappe {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////
		
	private int departureTime;
	private int arrivalTime;
	private Coord startCoord;
	private Coord endCoord;
	private int modeInteger;
	private String carType;
	private String startCountry;
	private String endCountry;


//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public Etappe(int departureTime, int arrivalTime, Coord startCoord, Coord endCoord, int modeInteger, String startCountry, String endCountry) {

	this.arrivalTime = arrivalTime;
	this.departureTime = departureTime;
	this.startCoord =startCoord;
	this.endCoord = endCoord;
	this.modeInteger = modeInteger;
	this.startCountry = startCountry;
	this.endCountry = endCountry;
		
		
	}

//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////

	public int getDepartureTime() {
		return departureTime;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}


	public Coord getStartCoord() {
		return startCoord;
	}

	public Coord getEndCoord() {
		return endCoord;
	}

	public int getModeInteger() {
		return modeInteger;
	}

	public String getCarType() {
		return carType;
	}

	public String getStartCountry() {
		return startCountry;
	}

	public String getEndCountry() {
		return endCountry;
	}



	public String getMode(){
		
		String mode = null;
		if(modeInteger == 1){mode = "plane";}
		else if(modeInteger == 2){mode = "train";}
		else if(modeInteger == 3){mode = "postauto";}
		else if(modeInteger == 4){mode = "ship";}
		else if(modeInteger == 5){mode = "tram";}
		else if(modeInteger == 6){mode = "bus";}
		else if(modeInteger == 7){mode = "sonstigerOeV";}
		else if(modeInteger == 8){mode = "reisecar";}
		else if(modeInteger == 9){mode = "car";}
		else if(modeInteger == 10){mode = "truck";}
		else if(modeInteger == 11){mode = "taxi";}
		else if(modeInteger == 12){mode = "motorcycle";}
		else if(modeInteger == 13){mode = "mofa";}
		else if(modeInteger == 14){mode = "bicycle";}
		else if(modeInteger == 15){mode = "walk";}
		else if(modeInteger == 16){mode = "skateboard/skates";}
		else if(modeInteger == 17){mode = "other";}
		else if(modeInteger == 99){mode = "Pseudoetappe";}
		else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
		
		return mode;
		
	}
	


	
	
}
