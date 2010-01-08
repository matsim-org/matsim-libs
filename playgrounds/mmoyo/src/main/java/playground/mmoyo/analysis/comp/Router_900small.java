package playground.mmoyo.analysis.comp;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.PTRouter.PTValues;

public class Router_900small {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {

		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
		
		//PTValues.routerCalculator = 1;
		//PlanRouter.main(new String[]{configFile});
		
		PTValues.routerCalculator = 2;
		PlanRouter.main(new String[]{configFile});
	}
}
