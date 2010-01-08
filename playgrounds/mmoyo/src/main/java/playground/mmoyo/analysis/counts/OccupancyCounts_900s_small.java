package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class OccupancyCounts_900s_small {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
	
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_small_rieser.xml";
		OccupancyCounts.main(new String[]{configFile});
	
		String configFile2 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_small_moyo_time.xml";
		OccupancyCounts.main(new String[]{configFile2});
		
		String configFile3 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/config_900s_small_moyo_parameterized.xml";
		OccupancyCounts.main(new String[]{configFile3});
	}
}
