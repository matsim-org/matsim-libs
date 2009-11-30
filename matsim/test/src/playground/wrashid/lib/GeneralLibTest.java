package playground.wrashid.lib;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.testcases.MatsimTestCase;

public class GeneralLibTest extends MatsimTestCase {

	
	public void testReadWritePopulation(){
		Population population=GeneralLib.readPopulation("test/scenarios/equil/plans2.xml","test/scenarios/equil/network.xml" );
		
		GeneralLib.writePopulation(population, getOutputDirectory() + "plans2.xml");
		
		population=GeneralLib.readPopulation(getOutputDirectory() + "plans2.xml","test/scenarios/equil/network.xml" );
		
		assertEquals(2, population.getPersons().size());
	}
	
	public void testReadWriteFacilities(){
		ActivityFacilitiesImpl facilities=GeneralLib.readActivityFacilities("test/scenarios/equil/facilities.xml");
		
		GeneralLib.writeActivityFacilities(facilities, getOutputDirectory() + "facilities.xml");
		
		facilities=GeneralLib.readActivityFacilities(getOutputDirectory() + "facilities.xml");
		
		assertEquals(23, facilities.getFacilities().size());
		  
	}
	
	public void testReadWriteMatrix(){
		double[][] hubPriceInfoOriginal=GeneralLib.readMatrix(96, 4, false, "test/input/playground/wrashid/PSF/data/hubPriceInfo.txt");
	
		GeneralLib.writeMatrix(hubPriceInfoOriginal, getOutputDirectory() + "hubPriceInfo.txt", null);
		
		double[][] hubPriceInfoRead=GeneralLib.readMatrix(96, 4, false, getOutputDirectory() + "hubPriceInfo.txt");
		
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
	
	public void testScaleMatrix(){
		double[][] matrix=new double[1][1];
		matrix[0][0]=1.0;
		matrix=GeneralLib.scaleMatrix(matrix, 2);
		
		assertEquals(2.0, matrix[0][0]);
	}
} 