package playground.pieter.events;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.singapore.utils.events.listeners.TrimEventsWithPersonIds;

public class EventsMergeSorter {
	String inFileName;
	String outFileName;
	String numberedEventsFileName;
	String outputPath;
	private EventsManager events;
	
	
	
	public EventsMergeSorter(String inFileName, String outFileName) {
		super();
		this.inFileName = inFileName;
		this.outFileName = outFileName;
		this.outputPath = new File(outFileName).getParentFile().toString();
		this.numberedEventsFileName = this.outputPath+"\\numberedEvents.xml.gz";
	}




	/**
	 * @param inFileName
	 * @param outfileName
	 * <p> to do a  merge sort, we need to first addEventNumbers to each event.
	 */
	public void addEventNumbers(String inFileName, String outfileName){
		this.events = EventsUtils.createEventsManager();
		AddEventNumbers aev = new AddEventNumbers(outfileName);
		events.addHandler(aev);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.setValidating(false);
		reader.parse(inFileName);
		aev.closeFile();
	}
	
	private void splitEventsToMultipleSortedFiles() {
		this.events = EventsUtils.createEventsManager();
		SplitEventsToMultipleSortedFiles setmsf = new SplitEventsToMultipleSortedFiles(outputPath+"\\sorted_", 100000);
		events.addHandler(setmsf);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.setValidating(false);
		reader.parse(inFileName);
		setmsf.writeQueue();
		setmsf.closeFile();
		
	}
	
	private void run() {
//		addEventNumbers(inFileName, outFileName);
		Logger.getLogger(this.getClass()).info("Writing to folder "+outputPath);
//		addEventNumbers(inFileName, numberedEventsFileName);
		splitEventsToMultipleSortedFiles();
	}


	




	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		EventsMergeSorter eventMergeSorter = new EventsMergeSorter(args[0],args[1]);
		eventMergeSorter.run();
		

		
	}







}
