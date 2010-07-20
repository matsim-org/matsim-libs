package playground.wrashid.parkingSearch.planLevel.trb;

import org.matsim.core.controler.Controler;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

public class Main1 {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2010/input/config.xml";
		controler = new Controler(configFilePath);
		
		new BaseControlerScenario(controler);
		
		controler.run();
	}
}
