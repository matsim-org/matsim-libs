package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;

public class AllRouter {

	/**routes scenario with a defined cost calculations and increasing parameters coefficients**/

	public AllRouter( ){
				
	}
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split plans?
		-cost coefficients
		-only plans inside the investigation area?
		*/
		
		String configFile = args[0];
		
		PTValues.scenario = "5x_95Time_5dist_60penalty";
		PTValues.routerCalculator = 3;
		PTValues.distanceCoefficient =0.5;
		PTValues.timeCoefficient = 0.95;
		PTValues.transferPenalty = 300.0;
		PlanRouter.main(new String[]{configFile});
	}

	
}
