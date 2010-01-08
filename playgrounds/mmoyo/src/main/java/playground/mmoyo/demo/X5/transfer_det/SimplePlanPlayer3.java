package playground.mmoyo.demo.X5.transfer_det;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.demo.ScenarioPlayer;

public class SimplePlanPlayer3{
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		//invoke controler first
		
		String configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer_det/config.xml";
		String scheduleFile =  "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer_det/simple1TransitSchedule.xml";
		ScenarioPlayer.main(new String[]{configFile, scheduleFile});
	}

}
