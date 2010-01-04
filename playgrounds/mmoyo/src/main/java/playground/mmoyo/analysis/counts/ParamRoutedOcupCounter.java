package playground.mmoyo.analysis.counts;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**Counts occupancy for routed plans with parameterized router*/
public class ParamRoutedOcupCounter {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = "..playgrounds/mmoyo/src/main/java/playground/mmoyo/analysis/counts/config.xml";
		String scheduleFile =  "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		OccupancyCounts.main(new String[]{configFile, scheduleFile});
	}
}
