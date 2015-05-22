package playground.sergioo.simulation2011;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

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
		Controler controler = new Controler(args[0]);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

}
