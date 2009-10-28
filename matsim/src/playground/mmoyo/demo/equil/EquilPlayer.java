package playground.mmoyo.demo.equil;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import playground.mmoyo.demo.ScenarioPlayer;

public class EquilPlayer {

	public static void main(String[] args) {
		
		String config ="src/playground/mmoyo/demo/equil/EquilConfig.xml"; 
		String schedule =  "src/playground/marcel/pt/demo/equilnet/transitSchedule.xml";
		
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
