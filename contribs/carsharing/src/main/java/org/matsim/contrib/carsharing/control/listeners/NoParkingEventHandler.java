package org.matsim.contrib.carsharing.control.listeners;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.events.NoParkingSpaceEvent;
import org.matsim.contrib.carsharing.events.handlers.NoParkingSpotEventHandler;

public class NoParkingEventHandler implements NoParkingSpotEventHandler {

	ArrayList<NoParkingInfo> noParking = new ArrayList<>();
	
	@Override
	public void reset(int iteration) {
		noParking = new ArrayList<>();
	}
	
	public ArrayList<NoParkingInfo> info() {
		return this.noParking;
	}
	
	public static class NoParkingInfo {
		
		Id<Link> linkId = null;
		String type = null;
		
		public String toString() {
			return linkId.toString() + " " + type;
		}		
		
	}

	@Override
	public void handleEvent(NoParkingSpaceEvent event) {
		NoParkingInfo info = new NoParkingInfo();
		info.linkId = event.getLinkId();
		info.type = event.getCarsharingType();
		noParking.add(info);
	}

}
