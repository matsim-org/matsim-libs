package playground.vsp.andreas.utils.net;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

public class CountParatransitVehicleHandler implements LinkEnterEventHandler{
	
	private String paratransitVehCode;
	private HashMap<Id, Integer> linkId2CountsTable;

	public CountParatransitVehicleHandler(String paratransitVehCode) {
		this.paratransitVehCode = TransportMode.pt + "_" + paratransitVehCode;
		this.linkId2CountsTable = new HashMap<Id, Integer>();
	}

	@Override
	public void reset(int iteration) {
		this.linkId2CountsTable = new HashMap<Id, Integer>();		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getDriverId().toString().startsWith(this.paratransitVehCode)){
			if(this.linkId2CountsTable.get(event.getLinkId()) == null){
				this.linkId2CountsTable.put(event.getLinkId(), new Integer(0));
			}
			
			int oldValue = this.linkId2CountsTable.get(event.getLinkId());
			this.linkId2CountsTable.put(event.getLinkId(), new Integer(oldValue + 1));
		}		
	}
	
	public int getCountForLinkId(Id linkId){
		Integer count = this.linkId2CountsTable.get(linkId);
		if(count == null){
			return 0;
		} else {
			return count.intValue();
		}
	}
}