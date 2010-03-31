package playground.mmoyo.analysis.comp;

import java.io.IOException;
import java.text.DecimalFormat;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.PlanRouter;

/**routes scenario with a defined cost calculations and increasing parameters coefficients**/
public class AllRouter {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split plans?
		-cost coefficients
		-only plans inside the investigation area?
		-ATTENTION. There is not "cost calculator 2" anymore. Adjust the coefficient to time=1, distance 0, transfer= 60.
		*/
		
		String configFile = null;
		
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/comparison/Berlin/16plans/0config_5x_4plans.xml";
		}
		
		/*
		PTValues.scenarioName ="m_to_compare";
		PTValues.routerCalculator = 3;
		PlanRouter.main(new String[]{configFile});
		*/
		
		
		PTValues.scenarioName ="r_16final";
		PTValues.routerCalculator = 1;
		PlanRouter.main(new String[]{configFile});
		
	}
}

/*
 * 		PTValues.routerCalculator = 3;
		for (double x= 0; x<=1.01; x = x + 0.05 ){
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			PTValues.timeCoefficient = Double.valueOf(twoDForm.format(x));
			PTValues.distanceCoefficient= Math.abs(Double.valueOf(twoDForm.format(1-x)));
			PTValues.scenarioName =  sample + "dist" + PTValues.distanceCoefficient + "_time" + PTValues.timeCoefficient ; 

			System.out.println("\nScenario:" + PTValues.scenarioName);
			PlanRouter.main(new String[]{configFile});
		}

*/
