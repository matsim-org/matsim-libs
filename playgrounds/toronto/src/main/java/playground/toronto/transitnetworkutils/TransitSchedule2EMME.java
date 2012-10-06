package playground.toronto.transitnetworkutils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.TorontoLinkTypes;

/**
 * A class for exporting a MATSim transit schedule to an EMME format.
 * 
 * Allows the user to specify a time period to aggregate departures over (since EMME uses a flat headway for an assignment period).
 * 
 * @author pkucirek
 *
 */
public class TransitSchedule2EMME {

	///////////////////////
	//PROPERTIES
	//////////////////////
	
	private static final Logger log = Logger.getLogger(TransitSchedule2EMME.class);
	
	private static NetworkImpl network;
	private static TransitSchedule schedule;
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
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
	
	private static FileFilter emmeTransitFF = new FileFilter() {
		@Override
		public String getDescription() {return "EMME transit batchin file in *.221 format";}
		
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".221" );
		}
	};
	
	
	///////////////////////
	//RUN METHOD
	//////////////////////
	
	private static void run(double startTime, double endTime, char agencyId, String filename) throws IOException{
		//flag turn links
		//FlagTurnLinks.run(network);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("c MATSim network export to EMME transit batchin file");
		writer.newLine(); writer.write("c '" + network.getName() + "'");
		writer.newLine(); writer.write("c Exported at " + dateFormat.format(new Date()));
		writer.newLine(); writer.write("t lines init");
		
		for (TransitLine line : schedule.getTransitLines().values()){
			int branches = 0;
			
			for (TransitRoute route : line.getRoutes().values()){
				boolean isStreetcarROW = false;
				
				String newRouteName = get6CharEmmeLineName(agencyId, line.getId(), route, ++branches);
				char mode = convertMode(route.getTransportMode());
				double hdwySeconds = getPeriodHeadway(startTime, endTime, route);
				if (hdwySeconds < 0) continue;//skips routes with no service.
				
				double hdwyMinutes = Math.round(hdwySeconds / 60.00 * 100.00) / 100.00; //rounds to 2 decimal places				
				if (route.getDepartures().size() < 3){
					log.warn("Line " + line.getId() + " route " + route.getId() + " has two or fewer departures in the selected service period!");
				}
				
				double speed = Math.round(getRouteSpeed(route) * 100.00) / 100.00;
				String descr = "Line " + line.getId() + " Route " + route.getId();
				
				int vehicleType = getVehicleType(route);
				
				//write line header
				writer.newLine(); 
				writer.write("a '" + newRouteName + "' " + mode + " " + vehicleType + " " + hdwyMinutes + " " + speed + " '" + descr + "'");
				writer.newLine();
								
				//get node sequence
				ArrayList<String> nodeIdSequence = new ArrayList<String>();
				{					
					LinkImpl currentLink = (LinkImpl) network.getLinks().get(route.getRoute().getStartLinkId());
					if (currentLink.getType().equals(TorontoLinkTypes.streetcarROW))
						isStreetcarROW = true;
					if (!currentLink.getType().equals(TorontoLinkTypes.loop)){
						nodeIdSequence.add(currentLink.getFromNode().getId().toString());
					}
					for (Id i : route.getRoute().getLinkIds()){
						currentLink = (LinkImpl) network.getLinks().get(i);
						if (currentLink.getType().equals(TorontoLinkTypes.streetcarROW))
							isStreetcarROW = true;
						if (currentLink.getType().equals(TorontoLinkTypes.loop)) continue; //Skips loop links
						
						nodeIdSequence.add(currentLink.getFromNode().getId().toString());
					}
					currentLink = (LinkImpl) network.getLinks().get(route.getRoute().getEndLinkId());
					nodeIdSequence.add(currentLink.getFromNode().getId().toString());
					if (!currentLink.getType().equals(TorontoLinkTypes.loop)){
						nodeIdSequence.add(currentLink.getToNode().getId().toString()); //If the route does not end on a loop (not likely) then append the final node to the sequence
					}
				}
				
				//fix streetcar ROWs
				if (isStreetcarROW){
					nodeIdSequence = fixStreetcarROWnodeSequence(nodeIdSequence);
				}
				
				//write initial node, with path setting, dwell time, and TTF.
				String prevNode = fixTurnNode(nodeIdSequence.get(0));
				writer.write(" path=no " + prevNode + " dwt=+.00 ttf=0");
				
				//writes the node sequence. fixes bugs that happen when dealing with Turn Links
				int col = 3;
				
				for (int i = 1; i < nodeIdSequence.size(); i++){
					if (col > 7){
						writer.newLine();
						col = 0;
					}
					String currentNode = fixTurnNode(nodeIdSequence.get(i));
					if (!prevNode.equals(currentNode)){
						writer.write(" " + currentNode);
						col++;
						prevNode = currentNode;
					}
				}
			}
			
		}
		
		writer.close();
	}
	
	///////////////////////
	//UTILITIES
	//////////////////////
	
	private static String fixTurnNode(String s){
		int mark = s.indexOf('-');
		if (mark < 0) mark = s.length();
		return s.substring(0, mark);
	}
	
	/**
	 * A modular function for creating the 6-character transit line Id required by
	 * EMME.
	 * 
	 *  I've set it to parse the specific TransitRouteId; but in the future others
	 *  may want to tweak this depending on how their MATSim transit schedule is
	 *  set up and named. pkucirek Aug '12
	 */
	private static String get6CharEmmeLineName(char agencyId, Id lineId, TransitRoute route, int branchNumber){
		String result = null;
		Character a = 'a';
		
		//short_name, direction, branch
		String[] components = route.getId().toString().split("-|_");
		//char dir = (char) (a.charValue() +  Integer.parseInt(components[1].replace("D", "")));
		char br = (char) (a.charValue() + Integer.parseInt(components[2].replace("B", "")));
		
		//result = agencyId + components[0] + dir + components[2].replace("B", "");
		result = agencyId + components[0] + br + components[1].replace("D", "");
		
		return result;
	}
	
	private static char convertMode(String matsimMode){
		if (matsimMode.equals("Subway") || matsimMode.equals("metro") || matsimMode.equals("subway")){
			return 'm';
		}else if (matsimMode.equals("Streetcar") || matsimMode.equals("tram")){
			return 's';
		}else if (matsimMode.equals("Bus") || matsimMode.equals("bus")){
			return 'b';
		}else if (matsimMode.equals("Train") | matsimMode.equals("rail")){
			return 'r';
		}
		
		return 0;
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
	
	private static ArrayList<String> fixStreetcarROWnodeSequence(ArrayList<String> nodeSequence){
		ArrayList<String> a = new ArrayList<String>();
		
		for (String s : nodeSequence){
			if (s.equals("97073")) {a.add("97073");}
			else if (s.equals("97074")) {a.add("97074");}
			else if (s.equals("11280")) {a.add("97075");}
			else if (s.equals("11279")) {a.add("97076");}
			else if (s.equals("11278")) {a.add("97077");}
			else if (s.equals("11277")) {a.add("97078");}
			else if (s.equals("10365-3")) {a.add("12380");}
			else if (s.equals("10365-6")) {a.add("12380");}
			else if (s.equals("11186-0")) {a.add("12379");}
			else if (s.equals("11186-8")) {a.add("12379");}
			else if (s.equals("11286")) {a.add("12378");}
			else if (s.equals("10359")) {a.add("12377");}
			else if (s.equals("10981")) {a.add("12376");}
			else if (s.equals("10354")) {a.add("12375");}
			else if (s.equals("10982")) {a.add("12374");}
			else if (s.equals("10983")) {a.add("12373");}
			else if (s.equals("10347")) {a.add("12372");}
			else if (s.equals("13251")) {a.add("12371");}
			else if (s.equals("10341")) {a.add("12370");}
			else if (s.equals("13249")) {a.add("12369");}
			else if (s.equals("10338")) {a.add("12368");}
			else if (s.equals("13227")) {a.add("12367");}
			else if (s.equals("10451")) {a.add("12366");}
			else if (s.equals("13225")) {a.add("12365");}
			else if (s.equals("10261")) {a.add("12360");}
			else if (s.equals("97075")) {a.add("12360");}
			else if (s.equals("11186-3")) {a.add("12379");}
			else if (s.equals("11186-5")) {a.add("12379");}
			else if (s.equals("10365-1")) {a.add("12380");}
			else if (s.equals("10365-7")) {a.add("12380");}
		}

		return a;
	}
	
	/**
	 * Gets the route speed as scheduled NOT as simulated (ie, without the effects of congestion).
	 * 
	 * @param route
	 * @return
	 */
	private static double getRouteSpeed(TransitRoute route){
		
		//figure out line length, in m
		double length = 0;
		for (Id i : route.getRoute().getLinkIds()){
			LinkImpl link = (LinkImpl) network.getLinks().get(i);
			if (link.getType().equals(TorontoLinkTypes.loop)) continue;
			length += link.getLength();
		}
		
		//determine offset to last stop, in s
		double lastStopOffset = route.getStops().get(route.getStops().size() - 1).getArrivalOffset();
		
		return length / lastStopOffset * 3.60; //calculated in m/s, converted to km/hr
	}
	
	private static double getPeriodHeadway(double startTime, double endTime, TransitRoute route){
		int departures = 0;
		 
		 for (Departure dep : route.getDepartures().values()){
			 if (dep.getDepartureTime() >= startTime && dep.getDepartureTime() < endTime)
				 departures++;
		 }
		 
		 if (departures == 0) return -1;
		 else{
			 double d = (double) departures;
			 return (endTime - startTime) / d;
		 }
	}
	
	/**
	 * Assumes that vehicleIds are formatted as "VehicleType[x]_[Number]"
	 * Also assumes that the vehicle type assigned to the first departure is
	 * the vehicle assigned to that route.
	 * 
	 * @param route
	 * @return
	 */
	private static int getVehicleType(TransitRoute route){
		ArrayList<Departure> periodDepartures = new ArrayList<Departure>();
		for (Departure dep : route.getDepartures().values()){
			periodDepartures.add(dep);
		}
		
		Collections.sort(periodDepartures, dComparator);
		
		String vehId =  periodDepartures.get(0).getVehicleId().toString();
		String typeAsString = vehId.split("_")[0].replace("VehType", "");
				
		return Integer.parseInt(typeAsString);
	}
	
	///////////////////////
	//MAIN
	//////////////////////
	
	public static void main(String[] args){
		
		JFileChooser fc;
		int state;
		String scheduleFile = null;
		fc = new JFileChooser();
		fc.setDialogTitle("Load Transit Schedule File");
		fc.setFileFilter(scheduleFF);
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			scheduleFile = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) System.exit(0);
		if (scheduleFile == null || scheduleFile.equals("")) System.exit(0);
		
		String networkFile = null;
		fc.setDialogTitle("Load Base Network File");
		fc.setFileFilter(networkFF);
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			networkFile = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) System.exit(0);
		if (networkFile == null || networkFile.equals("")) System.exit(0);
		
		String emmeTransitLinesFile = null;
		fc.setDialogTitle("Choose export file name");
		fc.setFileFilter(emmeTransitFF);
		state = fc.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			emmeTransitLinesFile = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) System.exit(0);
		if (emmeTransitLinesFile == null || emmeTransitLinesFile.equals("")) System.exit(0);
		
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("network", "inputNetworkFile", networkFile);
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		network = (NetworkImpl) scenario.getNetwork();
		network.setName(networkFile);
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
		
		try {
			run(startTime, endTime, 'T', emmeTransitLinesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
