package playground.mmoyo.demo.X5.waitTime;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.demo.ScenarioPlayer;

public class WaitingPlanPlayer {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = "../mmoyo/src/main/java/playground/mmoyo/demo/X5/waitTime/config.xml";
		ScenarioPlayer.main(new String[]{configFile});
	}
}

