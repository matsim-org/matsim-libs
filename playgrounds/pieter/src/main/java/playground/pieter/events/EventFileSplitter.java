package playground.pieter.events;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class EventFileSplitter {
	private final String inFileName;
	private final String outFileName;
	private final String numberedEventsFileName;
	private final String outputPath;


    private EventFileSplitter(String inFileName, String outFileName) {
		super();
		this.inFileName = inFileName;
		this.outFileName = outFileName;
		this.outputPath = new File(outFileName).getParentFile().toString();
		this.numberedEventsFileName = this.outputPath+"\\numberedEvents.xml.gz";
	}




//	/**
//	 * @param inFileName
//	 * @param outfileName
//	 * <p> to do a  merge sort, we need to first addEventNumbers to each event.
//	 */
//	public void addEventNumbers(String inFileName, String outfileName){
//		this.events = EventsUtils.createEventsManager();
//		AddEventNumbers aev = new AddEventNumbers(outfileName);
//		events.addHandler(aev);
//		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
//		reader.setValidating(false);
//		reader.parse(inFileName);
//		aev.closeFile();
//	}
	
	private void splitEventsToMultipleSortedFiles() {
        EventsManager events = EventsUtils.createEventsManager();
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
		
		
		EventFileSplitter eventMergeSorter = new EventFileSplitter(args[0],args[1]);
		eventMergeSorter.run();
		

		
	}







}
