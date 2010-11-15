package playground.mmoyo.demo.X5.transfer;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.demo.ScenarioPlayer;

public class SimplePlanPlayer2 {
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer/config.xml";
		ScenarioPlayer.main(new String[]{configFile});
	}
	
}
