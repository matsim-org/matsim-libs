package org.matsim.vis.otfvis.data.passengers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class OTFPassengerDataReader extends OTFDataReader {

	private OTFPassengerDataReceiver receiver;
	
	private ArrayList<Id> passengerIds = new ArrayList<Id>();

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.receiver = (OTFPassengerDataReceiver) receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.receiver.invalidate(graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		// Nothing to read here.
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		int nTransitVehicles = in.getInt();
		for (int i = 0; i < nTransitVehicles; i++) {
			passengerIds.clear();
			Id driverId = getId(in);
			int nPassengers = in.getInt();
			for (int j = 0; j < nPassengers; j++) {
				Id passengerId = getId(in);
				passengerIds.add(passengerId);
			}
			this.receiver.tellPassengers(driverId, passengerIds);
		}
	}

	private static IdImpl getId(ByteBuffer in) {
		return new IdImpl(ByteBufferUtils.getString(in));
	}
	
}
