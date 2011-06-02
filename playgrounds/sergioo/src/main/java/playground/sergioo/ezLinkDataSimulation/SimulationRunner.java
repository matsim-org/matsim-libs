package playground.sergioo.ezLinkDataSimulation;

import org.matsim.core.controler.Controler;

public class SimulationRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse("networkFile");
		TransitSchedule tS = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader =  new TransitScheduleReaderV1(tS, scenario.getNetwork(), scenario);
		reader.parse("transitScheduleFile");*/
		Controler controler = new Controler("./data/ezLinkDataSimulation/config.xml");
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
