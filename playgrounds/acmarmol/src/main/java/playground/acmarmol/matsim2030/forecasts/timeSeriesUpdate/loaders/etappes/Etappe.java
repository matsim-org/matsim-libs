package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes;

import org.matsim.core.gbl.Gbl;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

public class Etappe {

	private String weight;	
	private String mode;
	private int modeInteger;
	private String totalPeople;
	private String purpose;
	private String distance;
	private String duration;
	private int wegeNr;
	
	
	public void Ettape(){
		
	}


	public String getMode() {
		return mode;
	}


	public void setMode(String mode) {
		this.mode = mode;
	}


	public String getTotalPeople() {
		return totalPeople;
	}


	public void setTotalPeople(String total_people) {
		this.totalPeople = total_people;
	}


	public String getPurpose() {
		return purpose;
	}


	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}


	public String getDistance() {
		return distance;
	}


	public void setDistance(String distance) {
		this.distance = distance;
	}


	public String getWeight() {
		return weight;
	}


	public void setWeight(String weight) {
		this.weight = weight;
	}


	public String getDuration() {
		return duration;
	}


	public void setDuration(String duration) {
		this.duration = duration;
	}


	public int getWegeNr() {
		return wegeNr;
	}


	public void setWegeNr(int wegeNr) {
		this.wegeNr = wegeNr;
	}


	public int getModeInteger() {
		return modeInteger;
	}


	public void setModeInteger(int modeInteger) {
		this.modeInteger = modeInteger;
	}


	public String getWegeEquivalentModeFromEtappeMode(){
		
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
