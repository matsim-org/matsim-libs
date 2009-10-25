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

		
		// if the price was previously higher than 10 and still
		// higher than 10, then increase the price level
		// by 30%, because this means the price is still too low

		// as the prices are determined by e.g. a squar root
		// probabilistic algortihm, a
		// value of 30% results in much less reduction of vehicle
		// energy conumption during
		// the given slot than by 30%
		if (newPriceSignal >= maxPriceBaseLoad && oldInternalPriceSignal >= maxPriceBaseLoad) {
			newPriceSignal = getCorrectiveStepFactor() * oldInternalPriceSignal;

			// we are sure, that the maximum price level for this
			// slot is the current value

			minimumPriceSignal = Math.max(newPriceSignal, minimumPriceSignal);
		}

		// if this is the first time, the price has been above 10.0,
		// we need to find out the minimumPriceSignal value
		// therefore we decrease the value of the price signal
		// slowly

		// => as soon, as the current price has doped enough, there
		// will be a rise in the price, leading to the
		// mimumPriceSignal beeing set.
		// if (newPriceSignalMatrix[i][j]<maxPriceBaseLoad &&
		// oldPriceSignalMatrix[i][j]>maxPriceBaseLoad &&
		// minimumPriceSignal[i][j]==0.0){
		// decrease price by 5 %
		// newPriceSignalMatrix[i][j]=oldPriceSignalMatrix[i][j]*0.95;
		// }

		// if the new price is smaller than the minimum Price
		// signal, it should be set to the mimimumPriceSignal
		// => needed for stabilization of the system (because when
		// the right price is reached, PPSS will
		// drop the price lower than 10 => we need to keep the price
		// high. (correct it).
		if (newPriceSignal < minimumPriceSignal) {
			newPriceSignal = minimumPriceSignal;
		}

		// if new price is now higher than 9 and previously it was
		// lower than 9, then adapted the minimum price signal to at
		// least 9
		// (it should never go below 9 because of the above check)
		if (newPriceSignal>= maxPriceBaseLoad && oldInternalPriceSignal <= maxPriceBaseLoad) {
			minimumPriceSignal = maxPriceBaseLoad;
		}
		
		
		result[0]=newPriceSignal;
		result[1]=minimumPriceSignal;
		
		return result;
	}
}
