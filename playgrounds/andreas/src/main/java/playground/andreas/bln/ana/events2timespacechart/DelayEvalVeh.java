package playground.andreas.bln.ana.events2timespacechart;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

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
		EventsManagerImpl events = new EventsManagerImpl();
		
		for (DelayHandler delayHandler : this.handler) {
			events.addHandler(delayHandler);
		}		
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		try {
			reader.parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (DelayHandler delayHandler : this.handler) {
			delayHandler.writeGnuPlot(this.stopIDNameMap);
		}
	}
	
	private void readNetwork(String networkFile){
		ScenarioImpl scenario = new ScenarioImpl();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		this.network =  scenario.getNetwork();
	}

	private void readTransitSchedule(String transitScheduleFile){
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(this.transitSchedule, this.network);
		try {
			transitScheduleReaderV1.readFile(transitScheduleFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}