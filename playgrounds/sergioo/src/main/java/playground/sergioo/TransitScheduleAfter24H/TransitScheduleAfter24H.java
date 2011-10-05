package playground.sergioo.TransitScheduleAfter24H;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class TransitScheduleAfter24H {
	
	public static TransitSchedule addHours(TransitSchedule transitSchedule, Integer numHours) {
		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
		double numSeconds = numHours*3600;
		for(TransitLine transitLine:transitSchedule.getTransitLines().values())
			for(TransitRoute transitRoute:transitLine.getRoutes().values()) {
				List<Departure> newDepartures = new ArrayList<Departure>();
				for(Departure departure:transitRoute.getDepartures().values())
					if(departure.getDepartureTime()<numSeconds)
						newDepartures.add(transitScheduleFactory.createDeparture(new IdImpl(departure.getId().toString()+"_rep"), Time.MIDNIGHT+departure.getDepartureTime()));
				for(Departure departure:newDepartures)
					transitRoute.addDeparture(departure);
			}
		return transitSchedule;
	}
	
	//Main
	/**
	 * @param args
	 * 		  0 - number of hours after midnight
	 * 		  1 - Input MATSim transit file
	 *  	  2 - Output MATSim transit file
	 */
	public static void main(String[] args) {
		Integer numHours = Integer.parseInt(args[0]);
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(args[1]);
		new TransitScheduleWriter(TransitScheduleAfter24H.addHours(((ScenarioImpl)scenario).getTransitSchedule(),numHours)).writeFile(args[2]);
	}

}
