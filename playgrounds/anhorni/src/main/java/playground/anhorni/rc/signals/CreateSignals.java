package playground.anhorni.rc.signals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalGroup;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;

import playground.anhorni.rc.TunnelLinksFilterFactory;

public class CreateSignals {
	
	private static final Logger log = Logger.getLogger(CreateSignals.class);
	
	public static void main(String[] args) {
		CreateSignals signalsCreator = new CreateSignals();
		// arguments: String configFile, String outfolder, String greentimesXMLFile, String signalgroupsFile
		signalsCreator.run(args[0], args[1], args[2], args[3]);
	}
	
	/* TODO:
	 * create intersections file
	 * run config -> tutorial
	 */
	
	public void run(String configFile, String outfolder, String greentimesXMLFile, String signalgroupsFile) {
		log.info("greentimesXMLFile: "  + greentimesXMLFile);
		log.info("signalgroupsFile: "  + signalgroupsFile);
		this.createSignals(signalgroupsFile, greentimesXMLFile);		
		
		Config config = new Config();
		ConfigUtils.loadConfig(config, configFile);
		this.writeSignals(config, outfolder);
	}
	
	private void createSignals(String signalgroupsFile, String greentimesXMLFile) {
		SignalsData signalsData = new SignalsDataImpl(ConfigUtils.createConfig().signalSystems());
		
		// read file and 
		List<Intersection> intersections = this.readIntersections(signalgroupsFile);
		log.info(intersections.size() + " intersections created");
		
		this.createSignalSystemsAndGroups(signalsData, intersections);
		
		TreeMap<Id<Link>, LinkGTF> greentimefractions = this.readGTFS(greentimesXMLFile);	
		log.info(greentimefractions.size() + " link data read");
//		for (Id<Link> link : greentimefractions.keySet()) {
//			log.info(link.toString());
//		}
		
		this.createSignalControl(signalsData, greentimefractions);
	}
	
	private List<Intersection>  readIntersections(String signalgroupsFile) {
		List<Intersection> intersections = new Vector<Intersection>();
		try {
	          final BufferedReader in = new BufferedReader(new FileReader(signalgroupsFile));
	          String curr_line = in.readLine(); // Skip header
	          while ((curr_line = in.readLine()) != null) {	
		          String parts[] = curr_line.split(";");
		          Intersection intersection = new Intersection(Id.createNodeId(parts[0]));
		          int i = 0;
		          for (String link : parts) {
		        	  if (i>0) {
		        		  Id<Link> linkId = Id.create(link, Link.class);
		        		  intersection.addLinkId(linkId);
		        	  }
		        	  i++;
		          }
		          intersections.add(intersection);
	          }
	          in.close();	          
	        } // end try
	        catch (IOException e) {
	        	e.printStackTrace();
	        }
		
		return intersections;
	}
	
	private TreeMap<Id<Link>, LinkGTF> readGTFS(String greentimesXMLFile) {
		GTFSReader reader = new GTFSReader();	
		reader.parse(greentimesXMLFile);
		TreeMap<Id<Link>, LinkGTF> greentimefractions = reader.getGreentimefractions();	
		return greentimefractions;
	}
	
	
	private void createSignalSystemsAndGroups(SignalsData signalsData, List<Intersection> intersections) {
		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();
		
		// create a system for every intersection ...
		int cntSystems = 0;
		int cntSignals = 0;
		for (Intersection intersection : intersections) {
			SignalSystemData sys = systems.getFactory().createSignalSystemData(Id.create(Integer.toString(cntSystems), SignalSystem.class));
			systems.addSignalSystemData(sys);
				
			List<Id<Link>> linkIdsIntersection = intersection.getLinkIds();
			for (Id<Link> id: linkIdsIntersection) {
				SignalData signal = systems.getFactory().createSignalData(Id.create(Integer.toString(cntSignals), Signal.class));
				sys.addSignalData(signal);	
				signal.setLinkId(Id.create(id, Link.class));
				SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
				cntSignals++;
			}
			cntSystems++;
		}
		log.info(cntSystems + " systems created");
	}
	
	
	private void createSignalControl(SignalsData signalsData, TreeMap<Id<Link>, LinkGTF> greentimefractions) {
		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalControlData control = signalsData.getSignalControlData();
		Map<Id<SignalSystem>, SignalSystemData> systemmap = systems.getSignalSystemData();
		
		for (SignalSystemData signalsystem : systemmap.values()) {
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(signalsystem.getId());
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
									
			for (int h = 0; h < 24; h++) {	
				SignalPlanData plan = control.getFactory().createSignalPlanData(Id.create("0", SignalPlan.class));
				controller.addSignalPlanData(plan);
				plan.setStartTime(h * 3600.0);
				plan.setEndTime((h + 1) * 3600.0);
				
				Map<Id<Signal>, SignalData> signals = signalsystem.getSignalData();
				this.addCycleValues2Plan(signals, greentimefractions, h, plan, control);					
			}
		}
	}

	private void addCycleValues2Plan(Map<Id<Signal>, SignalData> signals, TreeMap<Id<Link>, LinkGTF> greentimefractions, int h, SignalPlanData plan, SignalControlData control) {
		List<Double> gtfsAllSignals = new Vector<Double>();
		double sumGtfs = 0.0;
		for (SignalData signal : signals.values()) {	
			double gtfs = 1.0;
			if (!greentimefractions.containsKey(signal.getLinkId())) {
				log.info("there is no signal data for link: " + signal.getLinkId());
				gtfs = 1.0/(signals.size() + 1);
			}
			else {
				gtfs = greentimefractions.get(signal.getLinkId()).getHourlyGTFS().get(h);
			}
			gtfsAllSignals.add(gtfs);
			sumGtfs += gtfs;
			
		}
		
		int offset = 5 * gtfsAllSignals.size();
		int cycle = 120 + offset;
		double scale = 120.0 / sumGtfs;
		
		int cnt = 0;
		int switchtime = 0;
		for (SignalData signal : signals.values()) {
			SignalGroupSettingsData settings = control.getFactory().createSignalGroupSettingsData(Id.create(signal.getId().toString(), SignalGroup.class));
			plan.addSignalGroupSettings(settings);
			
			settings.setOnset(switchtime);
			
			switchtime += gtfsAllSignals.get(cnt) * scale;
			cnt++;			
			settings.setDropping(switchtime);
			switchtime += 5;
		}
		plan.setCycleTime(cycle);
		
	}
	
	private void writeSignals(Config config, String outfolder) {	
		Scenario scenario = ScenarioUtils.loadScenario(config);		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		String signalSystemsFile = outfolder + "/signal_systems.xml";
		String signalGroupsFile = outfolder + "/signal_groups.xml";
		String signalControlFile = outfolder + "/signal_control.xml";
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);
		signalsWriter.setSignalControlOutputFilename(signalControlFile);
		signalsWriter.writeSignalsData(signalsData);
	}
}





