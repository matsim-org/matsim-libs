package playground.wrashid.parkingSearch.planLevel.scenario;

import org.matsim.core.controler.Controler;


public class BaseControlerScenarioOneLiner {

	public static Controler getControler(String configPath) {
		Controler controler;
		controler = new Controler(configPath);
		
		
		new BaseControlerScenario(controler);
		
		return controler;
	}
	
}
