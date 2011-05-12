package playground.andreas.fixedHeadway.ana;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class AnaTravelTimeOfPassenger {
	
	private final static Logger log = Logger.getLogger(AnaTravelTimeOfPassenger.class);

	BufferedWriter writer;
	String eventsInFile;
	List<PassengerTTAnaEventHandler> eventHandlerList = new LinkedList<PassengerTTAnaEventHandler>();
	
	public AnaTravelTimeOfPassenger(BufferedWriter writer, String eventsInFile){
		this.writer = writer;
		this.eventsInFile = eventsInFile;
	}

	public void run() {


		readEvents(this.eventsInFile);
		
		log.info("Finished");
		
	}
	
	public void addEventHandler(PassengerTTAnaEventHandler eventHandler){
		this.eventHandlerList.add(eventHandler);
	}

	private void readEvents(String filename){
		log.info("Reading " + this.eventsInFile);
		log.info("Added " + this.eventHandlerList.size() + " handlers");
		
		EventsManager events = EventsUtils.createEventsManager();
		
		for (PassengerTTAnaEventHandler handler : this.eventHandlerList) {
			events.addHandler(handler);
		}	
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
		
		try {
			this.writer.append(this.eventsInFile); this.writer.newLine();
			
			for (PassengerTTAnaEventHandler handler : this.eventHandlerList) {
				this.writer.append(handler.printResults()); this.writer.newLine();			
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}	

	public static void main(String[] args) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("d:/Berlin/Intervalltakt/analysis/travelTimeAna.csv")));
			
			AnaTravelTimeOfPassenger anaNormal = new AnaTravelTimeOfPassenger(writer, "d:/Berlin/Intervalltakt/analysis/normal/0.events.xml.gz");		
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "8:30", "9:32"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "8:29", "9:27"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_8", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_9", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_13", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_16", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "0:00", "30:00"));
			anaNormal.addEventHandler(new PassengerTTAnaEventHandler("veh_18", "0:00", "30:00"));
			anaNormal.run();
			
			AnaTravelTimeOfPassenger anaDeparture = new AnaTravelTimeOfPassenger(writer, "d:/Berlin/Intervalltakt/analysis/woDeparture/0.events.xml.gz");		
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "8:30", "9:32"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "8:29", "9:32"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "8:40", "9:39"));
			
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_8", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_9", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_13", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_16", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "0:00", "30:00"));
			anaDeparture.addEventHandler(new PassengerTTAnaEventHandler("veh_18", "0:00", "30:00"));
			anaDeparture.run();			
			
			AnaTravelTimeOfPassenger anaBaseCase = new AnaTravelTimeOfPassenger(writer, "d:/Berlin/Intervalltakt/analysis/basecase/0.events.xml.gz");
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "8:30", "9:31"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "8:29", "9:29"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_8", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_9", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_13", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_16", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new PassengerTTAnaEventHandler("veh_18", "0:00", "30:00"));
			anaBaseCase.run();
			
			AnaTravelTimeOfPassenger anaControl = new AnaTravelTimeOfPassenger(writer, "d:/Berlin/Intervalltakt/analysis/control/0.events.xml.gz");
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "8:30", "9:32"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "8:29", "9:27"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_8", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_9", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_13", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_14", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_15", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_16", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_17", "0:00", "30:00"));
			anaControl.addEventHandler(new PassengerTTAnaEventHandler("veh_18", "0:00", "30:00"));
			anaControl.run();
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
}
