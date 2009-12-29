package playground.wrashid.PSF.PSS;

import junit.framework.TestCase;

public class FirstPriceSignalMaintainingAlgorithmTest extends TestCase {

	public void testProcessPriceSignal(){
		
		double minPriceBaseLoad=FirstPriceSignalMaintainingAlgorithm.getMinPriceBaseLoad();
		double maxPriceBaseLoad=FirstPriceSignalMaintainingAlgorithm.getMaxPriceBaseLoad();
		double correctiveStepFactor=FirstPriceSignalMaintainingAlgorithm.getCorrectiveStepFactor();
		
		double valueBelowMaxBaseLoad=(minPriceBaseLoad+maxPriceBaseLoad)/2;
		double newPriceSignal;
		double newMinimumPriceSingal;
		double[] newSignalValues;
		
		// don't change the price, if price was and is below maxPriceBaseLoad
		newSignalValues=FirstPriceSignalMaintainingAlgorithm.processPriceSignal(valueBelowMaxBaseLoad, valueBelowMaxBaseLoad, 0.0);
		newPriceSignal=newSignalValues[0];
		newMinimumPriceSingal=newSignalValues[1];
		
		assertEquals(valueBelowMaxBaseLoad, newPriceSignal);
		assertEquals(0.0, newMinimumPriceSingal);
		
		// increase the price and minimum price level, if the price was high and is still high
		newSignalValues=FirstPriceSignalMaintainingAlgorithm.processPriceSignal(maxPriceBaseLoad+1, maxPriceBaseLoad+2, 0.0);
		newPriceSignal=newSignalValues[0];
		newMinimumPriceSingal=newSignalValues[1];
		
		assertEquals((maxPriceBaseLoad+2)*correctiveStepFactor, newPriceSignal);
		assertEquals((maxPriceBaseLoad+2)*correctiveStepFactor, newMinimumPriceSingal);
		
		// if the price is high for the first time, adapt the minimum price level to maxPriceBaseLoad
		// don't change the value of the price level
		newSignalValues=FirstPriceSignalMaintainingAlgorithm.processPriceSignal(maxPriceBaseLoad+1, valueBelowMaxBaseLoad, 0.0);
		newPriceSignal=newSignalValues[0];
		newMinimumPriceSingal=newSignalValues[1];
		
		assertEquals(maxPriceBaseLoad+1, newPriceSignal);
		assertEquals(maxPriceBaseLoad+1, newMinimumPriceSingal);
		
		// the price signal should never drop below 'maxPriceBaseLoad', if it has been higher than this value
		newSignalValues=FirstPriceSignalMaintainingAlgorithm.processPriceSignal(valueBelowMaxBaseLoad, maxPriceBaseLoad+1.0, maxPriceBaseLoad+1.0);
		newPriceSignal=newSignalValues[0];
		newMinimumPriceSingal=newSignalValues[1];
		
		assertEquals(maxPriceBaseLoad+1, newPriceSignal);
		assertEquals(maxPriceBaseLoad+1, newMinimumPriceSingal);
	}
	 
}     
