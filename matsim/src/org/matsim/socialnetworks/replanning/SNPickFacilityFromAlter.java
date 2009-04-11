package org.matsim.socialnetworks.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class SNPickFacilityFromAlter extends AbstractMultithreadedModule {
	
	private final static Logger log = Logger.getLogger(SNPickFacilityFromAlter.class);
	private String[] factypes={"home","work","shop","education","leisure"};
	private NetworkLayer network=null;
	private TravelCost tcost=null;
	private TravelTime ttime=null;
	
	public SNPickFacilityFromAlter(NetworkLayer network, TravelCost tcost, TravelTime ttime) {
		log.info("initializing SNPickFacility");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    }

    public PlanAlgorithm getPlanAlgoInstance() {

    	return new SNPickFacility(factypes, network, tcost, ttime);
    }
}
