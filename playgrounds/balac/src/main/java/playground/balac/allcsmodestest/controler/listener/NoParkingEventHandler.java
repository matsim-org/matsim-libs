package playground.balac.allcsmodestest.controler.listener;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.balac.allcsmodestest.events.NoParkingSpaceEvent;
import playground.balac.allcsmodestest.events.handler.NoParkingSpotEventHandler;

public class NoParkingEventHandler implements NoParkingSpotEventHandler {

	ArrayList<NoParkingInfo> noParking = new ArrayList<NoParkingInfo>();
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		noParking = new ArrayList<NoParkingInfo>();
		
	}
	
	public ArrayList<NoParkingInfo> info() {
		
		return this.noParking;
	}
	
	public class NoParkingInfo {
		
		Id<Link> linkId = null;
		String type = null;
		
		public String toString() {
			
			return linkId.toString() + " " + type;
		}		
		
	}

	@Override
	public void handleEvent(NoParkingSpaceEvent event) {
		// TODO Auto-generated method stub
		NoParkingInfo info = new NoParkingInfo();
		info.linkId = event.getLinkId();
		info.type = event.getCarsharingType();
		noParking.add(info);
		
	}

}
