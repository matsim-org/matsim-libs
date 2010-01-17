package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCountsAll {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String transitLineStrId = "BVB----344";
		String transitRouteStrId1 = "BVB----344.3.BVB----344.H";
		String transitRouteStrId2 = "BVB----344.5.BVB----344.R";
		
		String[] configs= new String[3];
		configs[0]= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_rieser.xml";
		configs[1]= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_time.xml";
		configs[2]= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_parameterized.xml";
		
		for (byte i=0; i<3; i++){
			OccupancyCounts.main(new String[]{configs[i], transitLineStrId, transitRouteStrId1, transitRouteStrId2});
		}
	
	}
}
