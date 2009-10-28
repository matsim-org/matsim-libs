package playground.mmoyo.demo.X5;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import playground.mmoyo.demo.ScenarioPlayer;

public class X5Player {

	public static void main(String[] args) {
		
		String config ="src/playground/mmoyo/demo/berlin/BerlinConfig.xml"; 
		String schedule =  "../shared-svn/studies/pt-experimental/5X5/transitSchedule.xml";
		
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
