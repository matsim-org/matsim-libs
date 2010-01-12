package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCounts_5x5 {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String transitLineStrId = "Blue Line";
		String transitRouteStrId1 = "Blue Route 0_20";
		String transitRouteStrId2 = "Blue Route 0_20";
		
		String configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/simplePlan1/CountsConfig.xml";
		
		String [] parameters = {configFile, transitLineStrId, transitRouteStrId1, transitRouteStrId2};
		OccupancyCounts.main(parameters);
	}
}
