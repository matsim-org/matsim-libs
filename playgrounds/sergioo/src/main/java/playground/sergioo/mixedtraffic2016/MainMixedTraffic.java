package playground.sergioo.mixedtraffic2016;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class MainMixedTraffic {

	public static void main(String[] args) {
		Scenario scenario =ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new AnalizeLink(Id.createLinkId(args[1]), scenario.getConfig().qsim().getMainModes(), scenario.getNetwork(), scenario.getConfig().qsim().getEndTime()));
		new MatsimEventsReader(events).readFile(args[2]);
	}

}
