package playground.mmoyo.analysis.comp;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.PTRouter.PTValues;

public class Router_900small {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		PTValues.scenario = "small";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
	
		for (byte i=1; i<=3; i++){
			PTValues.routerCalculator = i;
			PlanRouter.main(new String[]{configFile});
		}
	}
}
