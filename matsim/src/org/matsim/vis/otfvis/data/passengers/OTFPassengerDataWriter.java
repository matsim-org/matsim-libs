package org.matsim.vis.otfvis.data.passengers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.basic.v01.Id;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueVehicle;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.pt.queuesim.PassengerAgent;
import org.matsim.pt.queuesim.TransitVehicle;
import org.matsim.vis.otfvis.data.OTFDataWriter;

public class OTFPassengerDataWriter extends OTFDataWriter<QueueLink> {

	private static final long serialVersionUID = 1L;

	private ArrayList<TransitVehicle> transitVehicles = new ArrayList<TransitVehicle>();
	
	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		// Nothing to write here.
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		getTransitVehicles();
		int nTransitVehicles = transitVehicles.size();
		out.putInt(nTransitVehicles);
		for (TransitVehicle transitVehicle : transitVehicles) {
			DriverAgent driverAgent = transitVehicle.getDriver();
			Id driverId = driverAgent.getPerson().getId();
			ByteBufferUtils.putString(out, driverId.toString());
			writePassengers(out, transitVehicle);
		}
	}

	private void getTransitVehicles() {
		transitVehicles.clear();
		for (QueueVehicle queueVehicle : this.src.getAllVehicles()) {
			if (queueVehicle instanceof TransitVehicle) {
				transitVehicles.add((TransitVehicle) queueVehicle);
			}
		}
	}

	private void writePassengers(ByteBuffer out, TransitVehicle transitVehicle) {
		Collection<PassengerAgent> passengers = transitVehicle.getPassengers();
		int nPassengers = passengers.size();
		out.putInt(nPassengers);
		for (PassengerAgent passenger : passengers) {
			DriverAgent driverAgent = (DriverAgent) passenger;
			ByteBufferUtils.putString(out, driverAgent.getPerson().getId().toString());
		}
	}	
	
}
