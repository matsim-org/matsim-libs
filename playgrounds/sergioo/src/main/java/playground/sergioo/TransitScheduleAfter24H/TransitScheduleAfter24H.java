package playground.sergioo.TransitScheduleAfter24H;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
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
			for(TransitRoute transitRoute:transitLine.getRoutes().values())
				for(Departure departure:transitRoute.getDepartures().values())
					if(departure.getDepartureTime()<numSeconds)
						transitRoute.addDeparture(transitScheduleFactory.createDeparture(new IdImpl(departure.getId().toString()+"_rep"), Time.MIDNIGHT+departure.getDepartureTime()));	
		return null;
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
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(args[1]);
		new TransitScheduleWriter(TransitScheduleAfter24H.addHours(((ScenarioImpl)scenario).getTransitSchedule(),numHours)).writeFile(args[2]);
	}

}
