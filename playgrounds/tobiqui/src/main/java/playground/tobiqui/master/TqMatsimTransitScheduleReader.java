package playground.tobiqui.master;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class TqMatsimTransitScheduleReader {
	protected TransitSchedule transitSchedule;
	protected Scenario scenario;
	
	public TqMatsimTransitScheduleReader(String fileName, String configFileName) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFileName));
		new TransitScheduleReader(this.scenario).readFile(fileName);
	}
	
	public TransitSchedule getTransitSchedule() {
		this.transitSchedule = this.scenario.getTransitSchedule();
		
		return this.transitSchedule;
	}
	
}
