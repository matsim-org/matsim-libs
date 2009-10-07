package playground.wrashid.lib;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.testcases.MatsimTestCase;

public class GeneralLibTest extends MatsimTestCase {

	
	public void testReadWritePopulation(){
		Population population=GeneralLib.readPopulation("test/scenarios/equil/plans2.xml","test/scenarios/equil/network.xml" );
		
		GeneralLib.writePopulation(population, "output/plans2.xml");
		
		population=GeneralLib.readPopulation("output/plans2.xml","test/scenarios/equil/network.xml" );
		
		assertEquals(2, population.getPersons().size());
	}
	
	public void testReadWriteFacilities(){
		ActivityFacilitiesImpl facilities=GeneralLib.readActivityFacilities("test/scenarios/equil/facilities.xml");
		
		GeneralLib.writeActivityFacilities(facilities, "output/facilities.xml");
		
		facilities=GeneralLib.readActivityFacilities("output/facilities.xml");
		
		assertEquals(23, facilities.getFacilities().size());
		  
	}
	
	public void testReadWriteMatrix(){
		double[][] hubPriceInfoOriginal=GeneralLib.readMatrix(96, 4, false, "test/input/playground/wrashid/PSF/data/hubPriceInfo.txt");
	
		GeneralLib.writeMatrix(hubPriceInfoOriginal, "output/hubPriceInfo.txt", null);
		
		double[][] hubPriceInfoRead=GeneralLib.readMatrix(96, 4, false, "output/hubPriceInfo.txt");
		
		assertTrue(isEqual(hubPriceInfoOriginal, hubPriceInfoRead));
	}
	
	private boolean isEqual(double[][] matrixA, double[][] matrixB){
		for (int i=0;i<matrixA.length;i++){
			for (int j=0;j<matrixA[0].length;j++){
				if (matrixA[i][j]!=matrixB[i][j]){
					return false;
				}
			}
		}
		
		return true;
	}
}