package playground.mmoyo.demo.X5.simplePlan1;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.demo.ScenarioPlayer;

public class SimplePlanPlayer {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		//invoke controler first
		String configFile = "src/playground/mmoyo/demo/X5/simplePlan1/config.xml";
		String scheduleFile =  "src/playground/mmoyo/demo/X5/simplePlan1/simple1TransitSchedule.xml";
		ScenarioPlayer.main(new String[]{configFile, scheduleFile});
	}
}

