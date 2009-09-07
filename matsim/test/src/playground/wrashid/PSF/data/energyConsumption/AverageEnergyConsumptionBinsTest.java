package playground.wrashid.PSF.data.energyConsumption;

import playground.wrashid.PHEV.Utility.AverageSpeedEnergyConsumption;
import junit.framework.TestCase;

public class AverageEnergyConsumptionBinsTest extends TestCase{

	public void testGalus(){
		AverageEnergyConsumptionBins averageEnergyConsumptionBins=new AverageEnergyConsumptionGalus();
		
		assertEquals(0.0,averageEnergyConsumptionBins.getEnergyConsumption(0,1));
		
		// does interpolation between explicitly given sample points work (at the beginning)?
		assertEquals(3.173684E+02/2,averageEnergyConsumptionBins.getEnergyConsumption(5.555555556/2,1));
		
		assertEquals(3.173684E+02,averageEnergyConsumptionBins.getEnergyConsumption(5.555555556,1));
		
		// does interpolation between explicitly given sample points work?
		assertEquals((3.173684E+02+4.231656E+02)/2,averageEnergyConsumptionBins.getEnergyConsumption((5.555555556+8.333333333)/2,1));
		
		assertEquals(4.231656E+02,averageEnergyConsumptionBins.getEnergyConsumption(8.333333333,1));
			
		
		assertEquals(6.490326E+02,averageEnergyConsumptionBins.getEnergyConsumption(25,1));
		
		
		assertEquals(2.418100E+03,averageEnergyConsumptionBins.getEnergyConsumption(38.88888889,1));
		
		
		assertEquals(2.905639E+03,averageEnergyConsumptionBins.getEnergyConsumption(41.66666667,1));
		
		// interpolate at the end (through zero point and last point)
		assertEquals(2.905639E+03*2,averageEnergyConsumptionBins.getEnergyConsumption(41.66666667*2,1));
		
	}
	
} 
