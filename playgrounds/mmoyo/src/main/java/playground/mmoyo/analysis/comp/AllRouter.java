package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;

public class AllRouter {
	/**
	Do not forget to set:
	-name of scenario
	-get ride of autos?
	-split the plans?
	-cost coefficients
	*/
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		PTValues.scenario = "small";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/config_900s_small.xml";
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}
		
		PTValues.scenario = "big";
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_bignetwork/config_900s_big.xml";
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}

		PTValues.scenario = "nullAlles";
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/nullFall/config_Null.xml";
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}
	}

}
