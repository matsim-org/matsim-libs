package playground.anhorni.locationchoice.analysis.mc;

import playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources.Hectare;

public class MZTripHectare {
	
	MZTrip mzTrip;
	Hectare hectare;
	
	public MZTripHectare(MZTrip mzTrip, Hectare hectare) {
		this.mzTrip = mzTrip;
		this.hectare = hectare;
	}
	
	public MZTrip getMzTrip() {
		return mzTrip;
	}
	public void setMzTrip(MZTrip mzTrip) {
		this.mzTrip = mzTrip;
	}
	public Hectare getHectare() {
		return hectare;
	}
	public void setHectare(Hectare hectare) {
		this.hectare = hectare;
	}
}
