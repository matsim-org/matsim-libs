package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCounts_900s_big {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String transitLineStrId = "BVB----344";
		String transitRouteStrId1 = "BVB----344.3.BVB----344.H";
		String transitRouteStrId2 = "BVB----344.5.BVB----344.R";
		
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_rieser.xml";
		//OccupancyCounts.main(new String[]{configFile, transitLineStrId, transitRouteStrId1, transitRouteStrId2});
	
		String configFile2 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_moyo_time.xml";
		//OccupancyCounts.main(new String[]{configFile2, transitLineStrId, transitRouteStrId1, transitRouteStrId2});
		
		String configFile3 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_moyo_parameterized.xml";
		OccupancyCounts.main(new String[]{configFile3, transitLineStrId, transitRouteStrId1, transitRouteStrId2});	
	}
}
