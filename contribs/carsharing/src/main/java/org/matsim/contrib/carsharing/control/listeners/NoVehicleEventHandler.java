package org.matsim.contrib.carsharing.control.listeners;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.events.handlers.NoVehicleCarSharingEventHandler;

public class NoVehicleEventHandler implements NoVehicleCarSharingEventHandler {
	ArrayList<NoVehicleInfo> noVehicle = new ArrayList<NoVehicleInfo>();

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		noVehicle = new ArrayList<NoVehicleInfo>();
	}

	@Override
	public void handleEvent(NoVehicleCarSharingEvent event) {
		// TODO Auto-generated method stub
		NoVehicleInfo info = new NoVehicleInfo();
		info.originLinkId = event.getOriginLinkId();
		info.destinationLinkId = event.getDestinationLinkId();
		info.type = event.getCarsharingType();
		noVehicle.add(info);
	}

	public ArrayList<NoVehicleInfo> info() {
		return this.noVehicle;
	}

	public class NoVehicleInfo {
		Id<Link> originLinkId = null;

		Id<Link> destinationLinkId = null;

		String type = null;

		public String toString() {
			return originLinkId.toString() + " - " + destinationLinkId.toString() + " " + type;
		}
	}
}
