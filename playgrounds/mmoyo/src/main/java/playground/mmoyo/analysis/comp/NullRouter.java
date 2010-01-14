package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;

public class NullRouter {
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		PTValues.scenario = "null";
		PTValues.routerCalculator = 3;
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/nullFall/config_Null.xml";
		PlanRouter.main(new String[]{configFile});
	}

}
