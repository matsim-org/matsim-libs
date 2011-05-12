package playground.andreas.bln.ana.events2timespacechart;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class DelayEvalVeh {
	
	private final static Logger log = Logger.getLogger(DelayEvalVeh.class);
	private String outputDir;
	
	
	private NetworkImpl network;
	private TransitSchedule transitSchedule;
	HashMap<Id, String> stopIDNameMap;
	
	private LinkedList<DelayHandler> handler = new LinkedList<DelayHandler>();
	
	public static void main(String[] args) {
//		DelayEvalVeh delayEval = new DelayEvalVeh("E:/_out/veh/");
		DelayEvalVeh delayEval = new DelayEvalVeh("E:/_out/veh/", "d:/Berlin/BVG/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/stopareamap.txt");
//		delayEval.readNetwork("d:/Berlin/BVG/berlin-bvg09/pt/nullfall_BB_5x/network.xml.gz");
//		delayEval.readTransitSchedule("d:/Berlin/BVG/berlin-bvg09/pt/nullfall_BB_5x/transitSchedule_long.xml.gz");
		delayEval.readNetwork("d:/Berlin/intervalltakt/simulation/network.xml");
		delayEval.readTransitSchedule("d:/Berlin/intervalltakt/simulation/transitSchedule.xml");
		delayEval.addTransitLine("M44  ");	
		
		
//		delayEval.addTransitLine("B-M44");
//		delayEval.addTransitLine("T-M 5");
//		delayEval.addTransitLine("S-5");
		delayEval.readEvents("E:/_out/veh/0.events.xml.gz");
	}
	
	public DelayEvalVeh(String outputDir) {
		this.outputDir = outputDir;
		this.stopIDNameMap = null;		
	}	
	
	public DelayEvalVeh(String outputDir, String stopIdNamesMapFile) {
		this.outputDir = outputDir;
		try {
			this.stopIDNameMap = ReadStopIDNamesMap.readFile(stopIdNamesMapFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	private void addTransitLine(String line) {
		DelayHandler delayHandler = new DelayHandler(this.outputDir, line);
		this.handler.add(delayHandler);
		delayHandler.intialize(this.transitSchedule);
	}

	public void readEvents(String filename){
		EventsManager events = EventsUtils.createEventsManager();
		
		for (DelayHandler delayHandler : this.handler) {
			events.addHandler(delayHandler);
		}		
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
		
		for (DelayHandler delayHandler : this.handler) {
			delayHandler.writeGnuPlot(this.stopIDNameMap);
		}
	}
	
	private void readNetwork(String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		this.network =  scenario.getNetwork();
	}

	private void readTransitSchedule(String transitScheduleFile){
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(this.transitSchedule, this.network);
		transitScheduleReaderV1.readFile(transitScheduleFile);
	}



}