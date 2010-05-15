package playground.gregor.sims.shelters.allocation;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.evacuation.base.Building;



public class ShelterCounter implements LinkEnterEventHandler {

	
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	
	public ShelterCounter(NetworkLayer network, HashMap<Id,Building> shelterLinkMapping) {
		for (Link link : network.getLinks().values()) {
			if ((link.getId().toString().contains("sl") && link.getId().toString().contains("b"))) {
				Building b = shelterLinkMapping.get(link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(),link.getId()));
			} else if (link.getId().toString().contains("el2")) {
				Building b = shelterLinkMapping.get(link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(),link.getId()));				
			}
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	private static class LinkInfo {
		int count = 0;
		final int space;
		final Id id;
		public LinkInfo(int shelterSpace, Id id) {
			this.id = id;
			this.space = shelterSpace;

		}
	}
	
}
