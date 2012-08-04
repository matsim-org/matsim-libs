package playground.toronto.transitnetworkutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * A class for exporting a MATSim transit schedule to an EMME format.
 * 
 * Allows the user to specify a time period to aggregate departures over (since EMME uses a flat headway for an assignment period).
 * 
 * @author pkucirek
 *
 */
public class TransitSchedule2EMME {

	private static final Logger log = Logger.getLogger(TransitSchedule2EMME.class);
	
	private static Network network;
	private static TransitSchedule schedule;
	
	private static FileFilter networkFF = new FileFilter() {
		@Override
		public String getDescription() {return "MATSim network file in *.xml format";}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
		}
	};
	
	private static FileFilter scheduleFF = new FileFilter() {
		@Override
		public String getDescription() {return "MATSim transit schedule file in *.xml format";}
		
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
		}
	};
	
	
	
	public static void main(String[] args){
		
		JFileChooser fc;
		int state;
		String scheduleFile = null;
		fc = new JFileChooser();
		fc.setDialogTitle("Load Transit Schedule File");
		fc.setFileFilter(networkFF);
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			scheduleFile = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) System.exit(0);
		if (scheduleFile == null || scheduleFile.equals("")) System.exit(0);
		
		String networkFile = null;
		fc.setDialogTitle("Load Base Network File");
		fc.setFileFilter(scheduleFF);
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			networkFile = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) System.exit(0);
		if (networkFile == null || networkFile.equals("")) System.exit(0);
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("network", "inputNetworkFile", networkFile);
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		network = scenario.getNetwork();
		schedule = scenario.getTransitSchedule();
		
		log.info("################################\n" +
				"#  SCENARIO LOADED\n" +
				"################################");
		
		double startTime = 0;
		double endTime = 0;
		{
			String s = JOptionPane.showInputDialog("Enter a time period to aggregate over.\n\n" +
					"Format: start_time end_time\n" +
					"hh:mm:ss or hh:mm. Do not use AM/PM");
			String[] cells = s.split(" ");
			
			if (s == null || s.equals("") || cells.length != 2) System.exit(0);
			
			startTime = Time.parseTime(cells[0]);
			endTime = Time.parseTime(cells[1]);
		}
		
		
		
	}
	
	private static Comparator<Departure> dComparator = new Comparator<Departure>() {
		
		@Override
		public int compare(Departure d1, Departure d2) {
			if (d1.getDepartureTime() == d2.getDepartureTime()) return 0;
			else if (d1.getDepartureTime() > d2.getDepartureTime()) return 1;
			else if (d1.getDepartureTime() < d2.getDepartureTime()) return -1;
			else return 0;
		}
	};
	
	/**
	 * Determines the headway of a transit route for a given time period.
	 * 
	 * Starts by determining the number of departures 
	 * 
	 * @param startTime
	 * @param endTime
	 * @param route
	 * @return
	 */
	private static double getPeriodHeadway(double startTime, double endTime, TransitRoute route){
		
		//An (eventually) order list of departures which fall within the range specified.
		ArrayList<Departure> periodDepartures = new ArrayList<Departure>();
		for (Departure dep : route.getDepartures().values()){
			if (dep.getDepartureTime() >= startTime && dep.getDepartureTime() < endTime)
				periodDepartures.add(dep);
		}
		if (periodDepartures.size() == 1) return (endTime - startTime); //returns the duration of the period if there is only one departure
		else if (periodDepartures.size() == 0) return -1; //returns a negative headway is the route is not in service.
		
		Collections.sort(periodDepartures, dComparator);
		
		double hdwySum = 0;
		double prevDep = periodDepartures.get(0).getDepartureTime();
		for (int i = 1; i < periodDepartures.size(); i++){
			double dep = periodDepartures.get(i).getDepartureTime();
			hdwySum += (dep - prevDep);
			prevDep = dep;
		}
		
		return hdwySum / (periodDepartures.size() - 1); 
	}
	
	private static double  getPeriodHeadway(double startTime, double endTime, HashSet<TransitRoute> routes){
		ArrayList<Departure> periodDepartures = new ArrayList<Departure>();
		for (TransitRoute route : routes){
			for (Departure dep : route.getDepartures().values()){
				if (dep.getDepartureTime() >= startTime && dep.getDepartureTime() < endTime)
					periodDepartures.add(dep);
			}
		}
		if (periodDepartures.size() == 1) return (endTime - startTime); //returns the duration of the period if there is only one departure
		else if (periodDepartures.size() == 0) return -1; //returns a negative headway is the route is not in service.
		
		Collections.sort(periodDepartures, dComparator);
		
		double hdwySum = 0;
		double prevDep = periodDepartures.get(0).getDepartureTime();
		for (int i = 1; i < periodDepartures.size(); i++){
			double dep = periodDepartures.get(i).getDepartureTime();
			hdwySum += (dep - prevDep);
			prevDep = dep;
		}
		
		return hdwySum / (periodDepartures.size() - 1); 
	}
}
