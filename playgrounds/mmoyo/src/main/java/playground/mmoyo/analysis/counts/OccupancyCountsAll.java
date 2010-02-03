package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCountsAll {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String transitLineStrId = "B-344";
		String transitRouteStrId1 = "B-344.101.901.H";
		String transitRouteStrId2 = "B-344.101.901.R";
		
		//fragmented
		String config;
		
		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoParameterized.xml"; 
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});
		
		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoTime.xml"; 
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});

		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans.xml"; 
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});

		//no fragmented
		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans_MoyoParameterized.xml";
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});

		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans_MoyoTime.xml";
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});

		config = "..//shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans.xml";
		OccupancyCounts.main(new String[]{config, transitLineStrId, transitRouteStrId1, transitRouteStrId2});

		
	} 
			
}

