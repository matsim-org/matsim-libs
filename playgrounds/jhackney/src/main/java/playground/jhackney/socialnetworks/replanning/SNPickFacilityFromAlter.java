package playground.jhackney.socialnetworks.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.SocNetConfigGroup;

public class SNPickFacilityFromAlter extends AbstractMultithreadedModule {

	private final static Logger log = Logger.getLogger(SNPickFacilityFromAlter.class);
	private String[] factypes={"home","work","shop","education","leisure"};
	private Network network=null;
	private PersonalizableTravelCost tcost=null;
	private TravelTime ttime=null;
	private Knowledges knowledges;
	private final SocNetConfigGroup snConfig;

	public SNPickFacilityFromAlter(Config config, Network network, PersonalizableTravelCost tcost, TravelTime ttime, Knowledges kn) {
		super(config.global());
		log.info("initializing SNPickFacility");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    	this.knowledges = kn;
    	this.snConfig = (SocNetConfigGroup) config.getModule(SocNetConfigGroup.GROUP_NAME);
    }

    @Override
		public PlanAlgorithm getPlanAlgoInstance() {

    	return new SNPickFacility(factypes, (NetworkLayer) network, tcost, ttime, this.knowledges, this.snConfig);
    }
}
