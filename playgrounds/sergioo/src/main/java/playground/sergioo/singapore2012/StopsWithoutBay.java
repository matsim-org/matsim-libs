package playground.sergioo.singapore2012;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class StopsWithoutBay {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[1]);
		Set<TransitStopFacility> changedStops = new HashSet<TransitStopFacility>();
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
			Link link = scenario.getNetwork().getLinks().get(stop.getLinkId());
			if(link.getNumberOfLanes()<2 && link.getAllowedModes().size()!=1)
				changedStops.add(stop);
		}
		System.out.println(changedStops.size());
		for(TransitStopFacility stop:changedStops) {
			TransitStopFacility newStop = factory.createTransitStopFacility(stop.getId(), stop.getCoord(), true);
			newStop.setLinkId(stop.getLinkId());
			newStop.setName(stop.getName());
			scenario.getTransitSchedule().removeStopFacility(stop);
			scenario.getTransitSchedule().addStopFacility(newStop);
		}
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(args[2]);
	}
	
}
