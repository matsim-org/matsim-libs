package kid;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class KiDStatWriter {
	
	private ScheduledVehicles scheduledVehicles;

	public KiDStatWriter(ScheduledVehicles scheduledVehicles) {
		super();
		this.scheduledVehicles = scheduledVehicles;
	}
	
	public void write(String filename){
		BufferedWriter writer = openFile(filename);
		writeHead(writer);
		for(ScheduledVehicle vehicle : scheduledVehicles.getScheduledVehicles().values()){
			writeVehicle(vehicle,writer);
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

	private void writeVehicle(ScheduledVehicle vehicle, BufferedWriter writer) {
		try {
			writer.write((vehicle.getVehicle().getId().toString() + ";"));
			writer.write((vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TYPE) + ";"));
			writer.write((vehicle.getVehicle().getAttributes().get(KiDSchema.COMPANY_WIRTSCHAFTSZWEIG) + ";"));
			writer.write((vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTEN) + ";"));
			writer.write((vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_ANZAHLFAHRTENKETTEN) + ";"));
			writer.write((vehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_TAGESFAHRLEISTUNG) + ";"));
//			writer.write((vehicle.getVehicle().getAttributes().get(getAbfahrtsZeit(vehicle) + ";")));
			writer.write(newLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private String getAbfahrtsZeit(ScheduledVehicle vehicle) {
		ScheduledTransportChain scheduledTransportChain = vehicle.getScheduledTransportChains().get(0);
		TransportLeg transportLeg = null;
		if(scheduledTransportChain != null){
			transportLeg = scheduledTransportChain.getTransportLegs().get(0);
		}
		if(transportLeg != null){
			return transportLeg.getAttributes().get(KiDSchema.LEG_DEPARTURETIME);
		}
		return null;
	}

	private void writeHead(BufferedWriter writer) {
		try {
			writer.write("id;typ;wz;#fahrten;#ketten;leistung");
			writer.write(newLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private String newLine() {
		return "\n";
	}

	private BufferedWriter openFile(String filename) {
		return IOUtils.getBufferedWriter(filename);
	}

}
