package playground.wrashid.parkingSearch.planLevel.trb;

import org.matsim.core.controler.Controler;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

public class MainConfig5 {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig5.xml";
		controler = new Controler(configFilePath);
		
		new BaseControlerScenario(controler);
		
		controler.run();
	}
}
