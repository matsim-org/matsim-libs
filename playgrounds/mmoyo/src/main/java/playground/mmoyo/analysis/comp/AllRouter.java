package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;

public class AllRouter {

	/**routes all scenarios with the 3 cost calculations and increasing parameters coefficients**/
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split the plans?
		-cost coefficients
		-only plans inside the investigation area?
		*/

		/*
		PTValues.scenario = "small";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
	
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}
		
		PTValues.scenario = "big";
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_big.xml";
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}
		*/
		
		/*
		PTValues.routerCalculator = 3;
		for (double x= 0; x<=1; x = x + 0.1 ){
			PTValues.distanceCoefficient= x;
			PTValues.timeCoefficient= 1-x;
			String coef = "dist" + PTValues.distanceCoefficient + "_time" + PTValues.timeCoefficient;
			
			
			String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
			PTValues.scenario = coef + "small.xml"; 
			System.out.println("Scenario:" + PTValues.scenario);
			PlanRouter.main(new String[]{configFile});
			
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_big.xml";
			PTValues.scenario = coef + "big.xml";
			System.out.println("Scenario:" + PTValues.scenario);
			PlanRouter.main(new String[]{configFile});
		}*/
		
		PTValues.scenario = "BerlinBrandenburg";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/config.xml";
		
		
		PTValues.routerCalculator = 1;
		PlanRouter.main(new String[]{configFile});
		
		PTValues.routerCalculator = 2;
		PlanRouter.main(new String[]{configFile});
		
		PTValues.routerCalculator = 3;
		PTValues.distanceCoefficient =0.95;
		PTValues.timeCoefficient = 0.05;
		PTValues.transferPenalty = 300.0;
		PlanRouter.main(new String[]{configFile});
		

		
	}

}
