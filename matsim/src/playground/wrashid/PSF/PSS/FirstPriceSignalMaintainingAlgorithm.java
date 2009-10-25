package playground.wrashid.PSF.PSS;

public class FirstPriceSignalMaintainingAlgorithm {
	
	static double minPriceBaseLoad = 8.00;
	static double maxPriceBaseLoad = 9.00;
	static double correctiveStepFactor= 1.3; // multiply with this value the old price value, if the price remains high
	public static double getCorrectiveStepFactor() {
		return correctiveStepFactor;
	}



	public static double getMinPriceBaseLoad() {
		return minPriceBaseLoad;
	}


	
	public static double getMaxPriceBaseLoad() {
		return maxPriceBaseLoad;
	}

	/**
	 * Gives back in the a double array of size (2). The first array value is
	 * the newSignalPrice (to be set as the new internal signal). The second
	 * value is the new minimumPriceSingal.
	 * 
	 * 
	 * @param newPriceSignal
	 * @param oldInternalPriceSignal
	 * @param minimumPriceSignal
	 * @return
	 */
	public static double[] processPriceSignal(double newPriceSignal, double oldInternalPriceSignal, double minimumPriceSignal) {
		
		
		double[] result = new double[2];

		
		// simple philosophy:
		// keep the price high, once it has risen.
		// make the price even higher, if this does not help.
		
		// rise the minimumPriceSignal
		if (newPriceSignal>maxPriceBaseLoad){
			minimumPriceSignal=Math.max(newPriceSignal, minimumPriceSignal);
		}
		
		// if price has still not droped below maxPriceBaseLoad, then rise the price again
		if (newPriceSignal > maxPriceBaseLoad && oldInternalPriceSignal > maxPriceBaseLoad) {
			newPriceSignal = getCorrectiveStepFactor() * oldInternalPriceSignal;

			minimumPriceSignal = Math.max(newPriceSignal, minimumPriceSignal);
		}

		// assure, that the price level is never smaller than minimumPriceSignal
		if (newPriceSignal < minimumPriceSignal) {
			newPriceSignal = minimumPriceSignal;
		}
	
		
		result[0]=newPriceSignal;
		result[1]=minimumPriceSignal;
		
		return result;
	}
}
