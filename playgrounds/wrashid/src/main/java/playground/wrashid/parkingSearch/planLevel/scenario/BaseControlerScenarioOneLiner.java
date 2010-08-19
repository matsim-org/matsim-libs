package playground.wrashid.parkingSearch.planLevel.scenario;

import org.matsim.core.controler.Controler;


public class BaseControlerScenarioOneLiner {

	private final String configPath;

	public BaseControlerScenarioOneLiner(String configPath) {
		this.configPath = configPath;
	}
	
	public void run(){
		Controler controler;
		controler = new Controler(configPath);

		new BaseControlerScenario(controler);

		controler.run();
	}
	
}
