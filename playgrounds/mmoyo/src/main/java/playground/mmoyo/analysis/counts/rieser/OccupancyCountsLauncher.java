package playground.mmoyo.analysis.counts.rieser;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCountsLauncher {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String transitLineStrId = "Blue Line";
		String transitRouteStrId1 = "Blue Route 0_20";
		String transitRouteStrId2 = "Blue Route 0_20";

		String configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/simplePlan1/CountsConfig.xml";
		
		OccupancyCounts.main(new String[]{configFile, transitLineStrId, transitRouteStrId1, transitRouteStrId2});
	}
}
