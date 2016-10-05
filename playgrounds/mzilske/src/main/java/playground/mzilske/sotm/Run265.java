package playground.mzilske.sotm;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class Run265 {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig("/Users/michaelzilske/wurst/");
		config.global().setCoordinateSystem("GK4");
		config.network().setInputFile("sotm-network.xml");
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("sotm-schedule.xml");

		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfVisConfigGroup.setMapOverlayMode(true);
		otfVisConfigGroup.setDelay_ms(0);
		otfVisConfigGroup.setRenderImages(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);


		long departureId = 0;
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (double time = 0.0; time < 0.5 * 60 * 60; time += 10 * 60.0) {
					transitRoute.addDeparture(scenario.getTransitSchedule().getFactory().createDeparture(Id.create(departureId++, Departure.class), time));
				}
			}
		}
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();


		OTFVis.playScenario(scenario);

	}

}
