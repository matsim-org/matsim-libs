package playground.toronto.router;

import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Builds a {@link TransitRouter} using {@link UpgradedTransitNetworkTravelTimeAndDisutility} as its calculators;
 * 
 * @author pkucirek
 *
 */
public class UpgradedTransitRouterFactory implements TransitRouterFactory{

	private final TransitRouterConfig config;
	private final TransitDataCache cache;
	private final TransitSchedule schedule;
	private final TransitRouterNetwork routerNetwork;
	
	public UpgradedTransitRouterFactory(TransitRouterConfig config, TransitSchedule schedule, TransitDataCache data) {
		this.config = config;
		this.cache = data;
		this.schedule = schedule;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, config.beelineWalkConnectionDistance);
	}
	
	@Override
	public TransitRouter createTransitRouter() {
		UpgradedTransitNetworkTravelTimeAndDisutility calc =  new UpgradedTransitNetworkTravelTimeAndDisutility(cache, config);
			
		return new TransitRouterImpl(
				config, 
				this.routerNetwork,
				calc, 
				calc);
	}

}
