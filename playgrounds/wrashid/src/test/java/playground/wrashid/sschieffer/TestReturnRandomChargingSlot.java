package playground.wrashid.sschieffer;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;

public class TestReturnRandomChargingSlot extends TestCase{

	
	public void testReturnRandomChargingSlot() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);
		double [][] newValleyTimes  =  {{0, 1000}};
		myInfo.setValleyTimes(newValleyTimes);
		
		PolynomialFunction p = new PolynomialFunction(new double []{1.0/1000});
		myInfo.setProbabilityDensityFunction(p);
		
		double [][] probDensityRanges={{0, 1.0}};
		myInfo.setProbabilityRanges(probDensityRanges);
				
		double rand= Math.random();
		double[][] chargingTime=myInfo.returnRandomChargingSlot(rand);
		double diff= Math.abs(rand*1000-chargingTime[0][0]);
		if (chargingTime[0][1]== 1000)
			assertEquals(1000-Main.slotLength, chargingTime[0][0]);
		else{
			assertTrue(diff<=5);
		}
		
	}
	
}
