package playground.toronto.analysis.handlers;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;
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
public class AggregateBoardingsOverTimePeriodHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler {

	private static final Logger log = Logger.getLogger(AggregateBoardingsOverTimePeriodHandler.class);
	
	private final double periodStart;
	private final double periodEnd;
	
	private HashMap<Id, Integer> periodBoardings;
	private HashMap<Id, Id> vehicleIdLineIdMap;
	private HashSet<Id> drivers;
	private HashMap<Id, String> lineModeMap;
	
	public AggregateBoardingsOverTimePeriodHandler(){
		this.periodStart = 0;
		this.periodEnd = Double.POSITIVE_INFINITY;
		this.periodBoardings = new HashMap<Id, Integer>();
		this.vehicleIdLineIdMap = new HashMap<Id, Id>();
		this.drivers = new HashSet<Id>();
	}
	
	public AggregateBoardingsOverTimePeriodHandler(double startTime, double endTime){
		this.periodStart = startTime;
		this.periodEnd = endTime;
		this.periodBoardings = new HashMap<Id, Integer>();
		this.vehicleIdLineIdMap = new HashMap<Id, Id>();
		this.drivers = new HashSet<Id>();
	}
	
	public void MapLineModes(TransitSchedule schedule){
		this.lineModeMap = new HashMap<Id, String>();
		
		for (TransitLine line : schedule.getTransitLines().values()){
			String mode = "";
			for (TransitRoute r :line.getRoutes().values()){
				mode = r.getTransportMode();
				break;
			}
			this.lineModeMap.put(line.getId(), mode);
		}
	}
	
	public int getBoardingsByMode(String mode){
		int c = 0;
		for (Entry<Id, Integer> line : this.periodBoardings.entrySet()){
			if (this.lineModeMap.get(line.getKey()).equals(mode))
				c += line.getValue();
		}
		return c;
	}
	
	public void printResults(){
		log.info("Printing results for period starting at " + Time.writeTime(periodStart) + " to " + Time.writeTime(periodEnd));
		for (Entry<Id, Integer> e : this.periodBoardings.entrySet()){
			System.out.println("Line " + e.getKey() + ": " + e.getValue());
		}
	}
	
	public int getTotalBoardings(Id lineId){
		if (!this.periodBoardings.containsKey(lineId)) return 0;
		return this.periodBoardings.get(lineId);
	}
	public int getTotalBoardings(){
		int c = 0;
		for (Integer i : this.periodBoardings.values()) c += i;
		return c;
	}
	
	@Override
	public void reset(int iteration) {
		this.periodBoardings = new HashMap<Id, Integer>();
		this.vehicleIdLineIdMap = new HashMap<Id, Id>();
		this.drivers = new HashSet<Id>();
	}
		
	public static void main(String[] args){
		String eventsFile = args[0];
		
		String startTime = null;
		String endTime = null;
		if (args.length == 3){
			startTime = args[1];
			endTime = args[2];
		}

		
		AggregateBoardingsOverTimePeriodHandler abotph;
		SimplePopulationHandler sph = new SimplePopulationHandler();
		
		if (args.length == 3){
			try {
				double start = Time.parseTime(startTime);
				double end = Time.parseTime(endTime);
				abotph = new AggregateBoardingsOverTimePeriodHandler(start, end);
			} catch (NumberFormatException e) {
				abotph = new AggregateBoardingsOverTimePeriodHandler();
			}
		}else{
			abotph = new AggregateBoardingsOverTimePeriodHandler();
		}
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(abotph);
		em.addHandler(sph);
		
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		abotph.printResults();
		System.out.println("Events contained " + sph.getPop().size() + " persons.");
		Toolkit.getDefaultToolkit().beep(); //*beep*
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.drivers.contains(event.getPersonId())) return; //skip drivers
		
		if (event.getTime() < this.periodStart || event.getTime() > this.periodEnd) return; //ignores non-driver agents who are boarding outside of the period of interest.
		
		Id lineId = this.vehicleIdLineIdMap.get(event.getVehicleId());
		if (lineId == null) {
			//log.warn("Could not find a line Id for vehicle id = " + event.getVehicleId() + "!");
			return;
		}
		
		if (!this.periodBoardings.containsKey(lineId)){
			this.periodBoardings.put(lineId, 1);
		} else{
			this.periodBoardings.put(lineId, this.periodBoardings.get(lineId) + 1);
		}
			
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.drivers.add(event.getDriverId());
		this.vehicleIdLineIdMap.put(event.getVehicleId(), event.getTransitLineId());
		
	}

	
}