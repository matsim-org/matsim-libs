package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import playground.anhorni.locationchoice.cs.helper.MZTrip;
import playground.anhorni.locationchoice.facilities.Hectare;

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
