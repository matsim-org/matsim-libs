package playground.mzilske.freight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.events.DriverEventHandler;
import playground.mzilske.freight.events.DriverPerformanceEvent;

public class DriverEventWriter implements DriverEventHandler{

	private Map<Id,DriverPerformanceEvent> events = new HashMap<Id, DriverPerformanceEvent>();
	
	private String filename;
	
	
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void handleEvent(DriverPerformanceEvent event) {
		events.put(event.driverId, event);
	}

	@Override
	public void finish() {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writeFirstLine(writer);
		for(DriverPerformanceEvent event : events.values()){
			writeEvent(event,writer);
		}
		close(writer);
	}

	private void close(BufferedWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeFirstLine(BufferedWriter writer) {
		try {
			writer.write("driver" + semicolon() + "vehicle" + semicolon() + "carrier" + semicolon() + "distance[m]" + semicolon() + "time[sec]" + semicolon() +
					"capacityUsage" + semicolon() + "performance[unit*m]" + semicolon() + "volumes[units]" + newLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private String newLine() {
		return "\n";
	}

	private String semicolon() {
		return ";";
	}

	private void writeEvent(DriverPerformanceEvent event, BufferedWriter writer) {
		try {
			writer.write(event.driverId.toString() + semicolon());
			writer.write(event.carrierVehicleId.toString() + semicolon());
			writer.write(event.getCarrierId().toString() + semicolon());
			writer.write(Math.round(event.distance) + semicolon());
			writer.write(Math.round(event.time) + semicolon());
			writer.write(round(event.capacityUsage) + semicolon());
			writer.write(Math.round(event.performance) + semicolon());
			writer.write(Math.round(event.volumes) + semicolon());
			writer.write(newLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	private double round(double capacityUsage) {
		return Math.round(capacityUsage*100)/100.0;
	}

	@Override
	public void reset(int iteration) {
		
		
	}

}
