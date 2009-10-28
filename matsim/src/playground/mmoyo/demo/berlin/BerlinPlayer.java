package playground.mmoyo.demo.berlin;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import playground.mmoyo.demo.ScenarioPlayer;

public class BerlinPlayer {

	public static void main(String[] args) {
		
		String config ="src/playground/mmoyo/demo/berlin/BerlinConfig.xml"; 
		String schedule =  "../shared-svn/studies/ptsimmanuel/input/transitSchedule.networkOevModellBln.xml";
		
		try {
			ScenarioPlayer.main(new String[]{config, schedule});
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}

}
