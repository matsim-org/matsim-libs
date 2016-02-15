package playground.sergioo.transitScheduleTools2014;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;

public class TransitScheduleAfter24H {
	
	public static TransitSchedule addHours(TransitSchedule transitSchedule, Integer numHours) {
		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
		double numSeconds = numHours*3600;
		for(TransitLine transitLine:transitSchedule.getTransitLines().values())
			for(TransitRoute transitRoute:transitLine.getRoutes().values()) {
				List<Departure> newDepartures = new ArrayList<Departure>();
				for(Departure departure:transitRoute.getDepartures().values())
					if(departure.getDepartureTime()<numSeconds) {
						Departure newDeparture = transitScheduleFactory.createDeparture(Id.create(departure.getId().toString()+"_rep", Departure.class), Time.MIDNIGHT+departure.getDepartureTime());
						newDeparture.setVehicleId(Id.create(departure.getVehicleId().toString()+"_rep", Vehicle.class));
						newDepartures.add(newDeparture);
					}
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
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[1]);
		new TransitScheduleWriter(TransitScheduleAfter24H.addHours(((MutableScenario)scenario).getTransitSchedule(),numHours)).writeFile(args[2]);
	}

}
