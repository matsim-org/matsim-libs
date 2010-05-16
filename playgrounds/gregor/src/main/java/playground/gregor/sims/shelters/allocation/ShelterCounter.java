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
	HashMap<Building,Id> reversMapping = new HashMap<Building, Id>();
	private int inShelter = 0;
	
	public ShelterCounter(NetworkLayer network, HashMap<Id,Building> shelterLinkMapping) {
		for (Link link : network.getLinks().values()) {
			if ((link.getId().toString().contains("sl") && link.getId().toString().contains("b"))) {
				Building b = shelterLinkMapping.get(link.getId());
				this.reversMapping.put(b, link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(),link.getId()));
			} else if (link.getId().toString().equals("el1")) {
				Building b = shelterLinkMapping.get(link.getId());
				this.reversMapping.put(b, link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(),link.getId()));				
			}
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = this.linkInfos.get(event.getLinkId());
		if (li != null) {
			if (!event.getLinkId().equals("el1")) {
				this.inShelter ++;
			}
			li.count++;
		}
		
	}

	
	public Id tryToAddAgent(Building b) {
		Id linkId = this.reversMapping.get(b);
		LinkInfo li = this.linkInfos.get(linkId);
		if (li.count < li.space) {
			li.count++;
			return linkId;
		}
		return null;
	}
	
	@Override
	public void reset(int iteration) {
		for (LinkInfo li : this.linkInfos.values()) {
			li.count = 0;
		}
		this.inShelter = 0;
		
	}

	public int getNumAgentsInShelter(){
		return this.inShelter;
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
