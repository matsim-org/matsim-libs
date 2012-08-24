package playground.toronto.analysis.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class AggregateLineBoardingsAlightingsHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private ArrayList<Double> bins;
	private int[] boardings;
	private int[] alightings;
	private final TransitLine line;
	private HashSet<Id> vehicleIds;
	private HashSet<Id> activeVehicles;
	
	public AggregateLineBoardingsAlightingsHandler(TransitLine line){
		this.line = line;
		
		buildLineInfo();
		buildBins(900, Time.MIDNIGHT);
		this.boardings = new int[this.bins.size()];
		this.alightings = new int[this.bins.size()];
		
		this.activeVehicles = new HashSet<Id>();
	}
	public AggregateLineBoardingsAlightingsHandler(TransitLine line, double binSize, double endTime){
		this.line = line;
		
		buildLineInfo();
		buildBins(binSize, endTime);
		this.boardings = new int[this.bins.size()];
		this.alightings = new int[this.bins.size()];
		
		this.activeVehicles = new HashSet<Id>();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void buildLineInfo(){
		this.vehicleIds = new HashSet<Id>();
		for (TransitRoute route : this.line.getRoutes().values()){
			for (Departure dep : route.getDepartures().values()){
				this.vehicleIds.add(dep.getVehicleId());
				
			}
		}
	}
	
	private void buildBins(final double binSize, final double end){
		this.bins = new ArrayList<Double>();
		double currentTime = 0;
		
		while(currentTime < end){
			bins.add(currentTime);
			currentTime += binSize;
		}
	}

	private int getBinIndex(double now){
		double t = now;
		if (now <=  Time.parseTime("03:59:59")) 
			t += Time.MIDNIGHT;
		
		for (int i = 0; i < this.bins.size(); i++){
			if (t <= this.bins.get(i))  return i;
		}
		return -1;
	}
	
	public void exportTable(String filename) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		bw.write("Table of Boardings and Alightings for Line: " + this.line.getId().toString());
		bw.newLine(); bw.write("time_bin_start,boardings,alightings");
		for (int i = 0; i < this.bins.size(); i++){
			String t = Time.writeTime(this.bins.get(i));
			int b = this.boardings[i];
			int a = this.alightings[i];
			
			bw.newLine();
			bw.write(t + "," + b + "," + a);
		}
		bw.close();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////	
	
	@Override
	public void reset(int iteration) {
		this.boardings = new int[this.bins.size()];
		this.alightings = new int[this.bins.size()];
		this.activeVehicles = new HashSet<Id>();
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id vid = event.getVehicleId();
		if (this.vehicleIds.contains(vid)){
			//TODO Account for drivers somehow.
			int i = getBinIndex(event.getTime());
			if (i < 0) return; //skip persons outside of the time period.
			this.alightings[i]++;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vid = event.getVehicleId();
		if (this.vehicleIds.contains(vid)){
			if (!this.activeVehicles.contains(vid)){
				this.activeVehicles.add(vid);
				return; //Skips drivers
			}
			int i = getBinIndex(event.getTime());
			if (i < 0) return; //skip persons outside of the time period.
			this.boardings[i]++;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args){
		String eventsFile = args[0];
		String scheduleFile = args[1];
		String outputFolder = args[2];
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		AggregateLineBoardingsAlightingsHandler YUS = 
				new AggregateLineBoardingsAlightingsHandler(schedule.getTransitLines().get(new IdImpl(21948)));
		
		AggregateLineBoardingsAlightingsHandler BLR = 
				new AggregateLineBoardingsAlightingsHandler(schedule.getTransitLines().get(new IdImpl(21945)));
		
		AggregateLineBoardingsAlightingsHandler SHE = 
				new AggregateLineBoardingsAlightingsHandler(schedule.getTransitLines().get(new IdImpl(21946)));
		
		AggregateLineBoardingsAlightingsHandler SRT =
				new AggregateLineBoardingsAlightingsHandler(schedule.getTransitLines().get(new IdImpl(21947)));
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(SRT); em.addHandler(SHE); em.addHandler(BLR); em.addHandler(YUS);
		
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		try {
			YUS.exportTable(outputFolder + "/yus.txt");
			BLR.exportTable(outputFolder + "/blr.txt");
			SHE.exportTable(outputFolder + "/she.txt");
			SRT.exportTable(outputFolder + "/srt.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
