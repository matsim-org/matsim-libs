package playground.andreas.fixedHeadway.ana;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.xml.sax.SAXException;

public class AnaDelayOfVehicle {
	
	private final static Logger log = Logger.getLogger(AnaDelayOfVehicle.class);

	BufferedWriter writer;
	String eventsInFile;
	List<VehicleDelayAnaEventHandler> eventHandlerList = new LinkedList<VehicleDelayAnaEventHandler>();
	
	public AnaDelayOfVehicle(BufferedWriter writer, String eventsInFile){
		this.writer = writer;
		this.eventsInFile = eventsInFile;
	}

	public void run() {


		readEvents(this.eventsInFile);
		
		log.info("Finished");
		
	}
	
	public void addEventHandler(VehicleDelayAnaEventHandler eventHandler){
		this.eventHandlerList.add(eventHandler);
	}

	private void readEvents(String filename){
		log.info("Reading " + this.eventsInFile);
		log.info("Added " + this.eventHandlerList.size() + " handlers");
		
		EventsManagerImpl events = new EventsManagerImpl();
		
		for (VehicleDelayAnaEventHandler handler : this.eventHandlerList) {
			events.addHandler(handler);
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
		
		try {
			this.writer.append(this.eventsInFile); this.writer.newLine();
			
			for (VehicleDelayAnaEventHandler handler : this.eventHandlerList) {
				this.writer.append(handler.printResults()); this.writer.newLine();			
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}	

	public static void main(String[] args) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("d:/Berlin/Intervalltakt/analysis/travelTimeAnaVehicleDelay.csv")));
			
			AnaDelayOfVehicle anaNormal = new AnaDelayOfVehicle(writer, "d:/Berlin/Intervalltakt/analysis/normal/0.events.xml.gz");		
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "8:30", "9:32"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "8:29", "9:27"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_8", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_9", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_13", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_16", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "0:00", "30:00"));
			anaNormal.addEventHandler(new VehicleDelayAnaEventHandler("veh_18", "0:00", "30:00"));
			anaNormal.run();
			
			AnaDelayOfVehicle anaDeparture = new AnaDelayOfVehicle(writer, "d:/Berlin/Intervalltakt/analysis/woDeparture/0.events.xml.gz");		
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "8:30", "9:32"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "8:29", "9:32"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "8:40", "9:39"));
			
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_8", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_9", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_13", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_16", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "0:00", "30:00"));
			anaDeparture.addEventHandler(new VehicleDelayAnaEventHandler("veh_18", "0:00", "30:00"));
			anaDeparture.run();			
			
			AnaDelayOfVehicle anaBaseCase = new AnaDelayOfVehicle(writer, "d:/Berlin/Intervalltakt/analysis/basecase/0.events.xml.gz");
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "8:30", "9:31"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "8:29", "9:29"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_8", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_9", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_13", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_16", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "0:00", "30:00"));
			anaBaseCase.addEventHandler(new VehicleDelayAnaEventHandler("veh_18", "0:00", "30:00"));
			anaBaseCase.run();
			
			AnaDelayOfVehicle anaControl = new AnaDelayOfVehicle(writer, "d:/Berlin/Intervalltakt/analysis/control/0.events.xml.gz");
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "8:30", "9:32"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "8:29", "9:27"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "8:40", "9:42"));
			
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_8", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_9", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_13", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_14", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_15", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_16", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_17", "0:00", "30:00"));
			anaControl.addEventHandler(new VehicleDelayAnaEventHandler("veh_18", "0:00", "30:00"));
			anaControl.run();
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
}
