package playground.toronto.transitnetworkutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class GetCommonStops {

	private static TransitSchedule schedule;
	
	public static void main(String[] args){
		
		String scheduleFile = args[0];
		String outputFolder = args[1];
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		schedule = scenario.getTransitSchedule();
		
		String s = JOptionPane.showInputDialog("Enter a stop Id to lookup");
		
		while (true){
			if (s == null || s.equals("")) break;
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputFolder + "/" + s + ".txt"));
				bw.write(lookupStopServices(s));
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
					
			s = JOptionPane.showInputDialog("Enter a stop Id to lookup");
		}
		
	}
	
	
	private static String lookupStopServices(String s){
		
		TransitStopFacility stop = schedule.getFacilities().get(Id.create(s, TransitStopFacility.class));
		if (stop == null) throw new IllegalArgumentException("Could not find stop " + s);
		
		String report = "DEPARTURE TIME REPORT\n" +
				"----------------------------\n\n" +
				"STOP: " + s + "\n";
		
		for (TransitLine line : schedule.getTransitLines().values()){
			HashMap<Id, List<String>> routeDepartures = new HashMap<Id, List<String>>();
			for (TransitRoute route : line.getRoutes().values()){
				boolean isUsed = false;
				double offset = 0;
				for (TransitRouteStop x : route.getStops()){
					if (x.getStopFacility().equals(stop)) isUsed = true;
					offset = x.getArrivalOffset();
				}
				
				//route.getStops().indexOf(stop) >= 0
				if (isUsed){
					ArrayList<String> departures = new ArrayList<String>();
					for (Departure dep : route.getDepartures().values()) 
						departures.add(Time.writeTime(dep.getDepartureTime() + offset));
					routeDepartures.put(route.getId(), departures);
				}
			}
			
			if (routeDepartures.size() > 0){
				report += "\nLine --" + line.getId().toString() + "--\n";
				
				for (Entry<Id, List<String>> e : routeDepartures.entrySet()){
					report += "\n" + e.getKey().toString();
					for (String S : e.getValue()) report += "\t" + S;
				}
			}
		}
		
		return report;
	}
}
