package org.matsim.vis.otfvis.data.passengers;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vis.otfvis.caching.ClientDataBase;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;

public class OTFPassengerDataReceiver implements OTFDataReceiver {

	public void invalidate(SceneGraph graph) {

	}

	public void tellPassengers(Id driverId, ArrayList<Id> passengerIds) {
		Map<Id, Id> piggyBackingMap = ClientDataBase.getInstance().getPiggyBackingMap();
		for (Id passengerId : passengerIds) {
			piggyBackingMap.put(passengerId, driverId);
		}
	}

}
