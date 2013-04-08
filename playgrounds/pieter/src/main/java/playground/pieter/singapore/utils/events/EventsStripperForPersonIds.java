package playground.pieter.singapore.utils.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.Sample;
import playground.pieter.singapore.utils.events.listeners.TrimEventsWithPersonIds;

public class EventsStripperForPersonIds {

	
	String[] choiceSet;
	private EventsManager events;
	Scenario scenario =  ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	public EventsStripperForPersonIds(String networkFile, ArrayList<String> ids) throws SQLException{
		
		this.populateList( networkFile, ids);
	}
	

	
	private void populateList( String networkFile, ArrayList<String> ids) throws SQLException{
	
		new MatsimNetworkReader(scenario).readFile(networkFile);
		choiceSet = new String[ids.size()];
		for(int i=0; i<choiceSet.length;i++){
			choiceSet[i] = ids.get(i).toString();
		}
		
		
		scenario=null;
	}
	
	public void stripEvents(String inFileName, String outfileName, double frequency){
		this.events = EventsUtils.createEventsManager();
		int N = choiceSet.length;
		int M = (int) ((double)N*frequency);
		HashSet<String> sampledIds = new HashSet<String>();
		for(int i: Sample.sampleMfromN(M, N)){
			sampledIds.add(choiceSet[i]);
		}
		TrimEventsWithPersonIds filteredWriter = new TrimEventsWithPersonIds(outfileName, sampledIds);
		events.addHandler(filteredWriter);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(inFileName);
		filteredWriter.closeFile();
	}
	


	
	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub
		ArrayList<String> ids =new ArrayList<String>();
		ids.add("1000257190");
		EventsStripperForPersonIds stripper = new EventsStripperForPersonIds(args[0],  ids );
		stripper.stripEvents(args[1], args[2], 1.0);
	}



}
