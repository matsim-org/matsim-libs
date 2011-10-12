package playground.wrashid.artemis.lav;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;


public class EnergyConsumptionRegressionModelTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    @Test
    public final void basicTests() {
    	 String energyConsumptionRegModelFile = utils.getInputDirectory()+ "testData.txt";
    	 
    	 EnergyConsumptionRegressionModel energyConsumptionRegressionModel=new EnergyConsumptionRegressionModel(energyConsumptionRegModelFile);
    	 
    	 Assert.assertEquals(30.0,energyConsumptionRegressionModel.getBestMatchForMaxSpeed(30.0),0.1);
    	 Assert.assertEquals(50.0,energyConsumptionRegressionModel.getBestMatchForMaxSpeed(50.0),0.1);
    	 Assert.assertEquals(60.0,energyConsumptionRegressionModel.getBestMatchForMaxSpeed(60.0),0.1);
    	 Assert.assertEquals(90.0,energyConsumptionRegressionModel.getBestMatchForMaxSpeed(90.0),0.1);
    	 Assert.assertEquals(120.0,energyConsumptionRegressionModel.getBestMatchForMaxSpeed(120.0),0.1);
    	 
    	 //TODO: add tests here specific to test data...
 		
    }

	
}
