package playground.acmarmol.matsim2030.microcensus2010;
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

	public Etappe(int departureTime, int arrivalTime, Coord startCoord, Coord endCoord, int modeInteger, String startCountry, String endCountry, String carType) {

	this.arrivalTime = arrivalTime;
	this.departureTime = departureTime;
	this.startCoord =startCoord;
	this.endCoord = endCoord;
	this.modeInteger = modeInteger;
	this.startCountry = startCountry;
	this.endCountry = endCountry;
	this.carType = carType;
		
		
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
		if(modeInteger == 1){mode = MZConstants.PLANE;}
		else if(modeInteger == 2){mode = MZConstants.TRAIN;}
		else if(modeInteger == 3){mode = MZConstants.POSTAUTO;}
		else if(modeInteger == 4){mode = MZConstants.SHIP;}
		else if(modeInteger == 5){mode = MZConstants.TRAM;}
		else if(modeInteger == 6){mode = MZConstants.BUS;}
		else if(modeInteger == 7){mode = MZConstants.SONSTINGER_OEV;}
		else if(modeInteger == 8){mode = MZConstants.REISECAR;}
		else if(modeInteger == 9){mode = MZConstants.CAR;}
		else if(modeInteger == 10){mode =MZConstants.TRUCK ;}
		else if(modeInteger == 11){mode = MZConstants.TAXI;}
		else if(modeInteger == 12){mode = MZConstants.MOTORCYCLE;}
		else if(modeInteger == 13){mode = MZConstants.MOFA;}
		else if(modeInteger == 14){mode = MZConstants.BICYCLE;}
		else if(modeInteger == 15){mode = MZConstants.WALK;}
		else if(modeInteger == 16){mode = MZConstants.SKATEBOARD;}
		else if(modeInteger == 17){mode = MZConstants.OTHER;}
		else if(modeInteger == 99){mode = MZConstants.PSEUDOETAPPE;} else
			throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
		
		return mode;
		
	}
	


	
	
}
