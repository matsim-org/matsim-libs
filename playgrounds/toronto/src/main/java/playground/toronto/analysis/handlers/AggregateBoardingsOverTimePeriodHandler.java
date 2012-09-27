package playground.toronto.analysis.handlers;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * A handler for aggregating total boardings for transit lines, over all branches, for a given 
 * time period. 
 * 
 * Assumes each vehicleId is referenced to only a single line. Will give bad results otherwise.
 * 
 * Does not count transit drivers.
 * 
 * @author pkucirek
 *
 */
public class AggregateBoardingsOverTimePeriodHandler implements PersonEntersVehicleEventHandler {

	private static final Logger log = Logger.getLogger(AggregateBoardingsOverTimePeriodHandler.class);
	
	private final double periodStart;
	private final double periodEnd;
	private final TransitSchedule schedule;
	
	private HashMap<Id, Integer> periodBoardings;
	private HashMap<Id, Id> vehicleIdLineIdMap;
	private HashSet<Id> activeVehicles;

	public AggregateBoardingsOverTimePeriodHandler(TransitSchedule schedule){
		this.periodStart = 0;
		this.periodEnd = Double.POSITIVE_INFINITY;
		this.schedule = schedule;
		this.periodBoardings = new HashMap<Id, Integer>();
		this.activeVehicles = new HashSet<Id>();
		
		this.buildVehIdLineIdMap();
	}
	
	public AggregateBoardingsOverTimePeriodHandler(double startTime, double endTime, TransitSchedule schedule){
		this.periodStart = startTime;
		this.periodEnd = endTime;
		this.schedule = schedule;
		this.periodBoardings = new HashMap<Id, Integer>();
		this.activeVehicles = new HashSet<Id>();
		
		this.buildVehIdLineIdMap();
	}
	
	public void printResults(){
		log.info("Printing results for period starting at " + Time.writeTime(periodStart) + " to " + Time.writeTime(periodEnd));
		for (Entry<Id, Integer> e : this.periodBoardings.entrySet()){
			System.out.println("Line " + e.getKey() + ": " + e.getValue());
		}
	}
	
	public int getTotalBoardings(Id lineId){
		return this.periodBoardings.get(lineId);
	}
	
	@Override
	public void reset(int iteration) {
		this.periodBoardings = new HashMap<Id, Integer>();
		this.activeVehicles = new HashSet<Id>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		//Assuming that the first PersonentersVehicleEvent for each vehicle is the driver entering
		if (!this.activeVehicles.contains(event.getVehicleId())){
			this.activeVehicles.add(event.getVehicleId());
			return; //does not add the driver.
		}
		
		if (event.getTime() < this.periodStart || event.getTime() > this.periodEnd) return; //ignores non-driver agents who are boarding outside of the period of interest.
		
		Id lineId = this.vehicleIdLineIdMap.get(event.getVehicleId());
		if (lineId == null) {
			log.warn("Could not find a line Id for vehicle id = " + event.getVehicleId() + "!");
			return;
		}
		
		if (!this.periodBoardings.containsKey(lineId)){
			this.periodBoardings.put(lineId, 1);
		} else{
			this.periodBoardings.put(lineId, this.periodBoardings.get(lineId) + 1);
		}
			
	}
	
	private void buildVehIdLineIdMap(){
		this.vehicleIdLineIdMap = new HashMap<Id, Id>();
		
		for (TransitLine line : this.schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (Departure dep : route.getDepartures().values()){
					vehicleIdLineIdMap.put(dep.getVehicleId(), line.getId());
				}
			}
		}
	}
	
	public static void main(String[] args){
		String eventsFile = args[0];
		String scheduleFile = args[1];
		
		String startTime = null;
		String endTime = null;
		if (args.length == 4){
			startTime = args[2];
			endTime = args[3];
		}

		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		AggregateBoardingsOverTimePeriodHandler abotph;
		
		if (args.length == 4){
			try {
				double start = Time.parseTime(startTime);
				double end = Time.parseTime(endTime);
				abotph = new AggregateBoardingsOverTimePeriodHandler(start, end, schedule);
			} catch (NumberFormatException e) {
				abotph = new AggregateBoardingsOverTimePeriodHandler(schedule);
			}
		}else{
			abotph = new AggregateBoardingsOverTimePeriodHandler(schedule);
		}
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(abotph);
		
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		abotph.printResults();
		Toolkit.getDefaultToolkit().beep(); //*beep*
	}
}
