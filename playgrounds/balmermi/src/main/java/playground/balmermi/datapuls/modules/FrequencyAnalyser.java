package playground.balmermi.datapuls.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public class FrequencyAnalyser implements LinkLeaveEventHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(FrequencyAnalyser.class);
	private final Map<Id,Set<Id>> freqs;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public FrequencyAnalyser(final Network network) {
		log.info("init " + this.getClass().getName() + " module...");
		freqs = new HashMap<Id, Set<Id>>((int)(network.getLinks().size()*1.4));
		for (Id lid : network.getLinks().keySet()) {
			freqs.put(lid,new HashSet<Id>());
		}
		log.info("done. (init)");
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		freqs.get(event.getLinkId()).add(event.getPersonId());
	}

	@Override
	public void reset(int iteration) {
		log.info("reset " + this.getClass().getName() + " module...");
		for (Set<Id> idSet : freqs.values()) { idSet.clear(); }
		log.info("done. (reset)");
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	
	public final Map<Id,Set<Id>> getFrequencies() {
		return freqs;
	}
}
