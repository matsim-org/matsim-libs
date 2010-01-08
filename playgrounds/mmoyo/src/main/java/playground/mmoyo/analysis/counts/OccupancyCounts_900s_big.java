package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import playground.mmoyo.analysis.comp.PlanRouter;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCounts_900s_big {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		//DO: change the cost calculation to parameterized
		//String configFileRouter = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_big.xml";
		//PlanRouter.main(new String[]{configFileRouter});
		
		//DO: move output/moyorouted   to routedplans and rename to 900s_big_moyo_routedPlans_transfer_Penalty.xml
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_rieser.xml";
		OccupancyCounts.main(new String[]{configFile});
	
		String configFile2 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_moyo_time.xml";
		OccupancyCounts.main(new String[]{configFile2});
		
		String configFile3 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_big_moyo_parameterized.xml";
		//attention the plans file will be located at ../playgrounds/mmoyo/output/moyo_routedPlans.xml because it will run at night
		OccupancyCounts.main(new String[]{configFile3});	
	}
}
