package playground.balmermi.datapuls.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.Gbl;

public class FrequencyAnalyser implements LinkLeaveEventHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(FrequencyAnalyser.class);
	private final Map<Id,Set<Id>> freqs;
	private long entryCnt = 0;
	private long hour = -1;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public FrequencyAnalyser(final Network network) {
		this(network,network.getLinks().keySet());
	}

	public FrequencyAnalyser(final Network network, Set<Id> linkIdSet) {
		log.info("init " + this.getClass().getName() + " module...");
		if (linkIdSet == null) { throw new NullArgumentException("linkIdSet"); }
		freqs = new HashMap<Id, Set<Id>>((int)(network.getLinks().size()*1.4));
		for (Id lid : linkIdSet) { freqs.put(lid,new HashSet<Id>()); }
		log.info("=> "+freqs.size()+" sets allocated.");
		log.info("done. (init)");
	}

	//////////////////////////////////////////////////////////////////////
	// interface methods
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Set<Id> pids = freqs.get(event.getLinkId());
		if (pids == null) { return; }
		if (pids.add(event.getPersonId())) { entryCnt++; }
		// logging info
		if ((entryCnt != 0) && (entryCnt % freqs.size() == 0)) {
			log.info(entryCnt+" entries added to the frequency map.");
			Gbl.printMemoryUsage();
		}
		if (((int)(event.getTime()/3600.0)) != hour) { hour++; log.info("at hour "+hour); }
	}

	@Override
	public void reset(int iteration) {
		log.info("reset " + this.getClass().getName() + " module...");
		for (Set<Id> idSet : freqs.values()) { idSet.clear(); }
		entryCnt = 0;
		hour = -1;
		log.info("done. (reset)");
	}
	
	public void resetLog() {
		log.info("reset log of " + this.getClass().getName() + " module...");
		entryCnt = 0;
		hour = -1;
		log.info("done. (reset log)");
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	
	public final Map<Id,Set<Id>> getFrequencies() {
		return freqs;
	}
}
