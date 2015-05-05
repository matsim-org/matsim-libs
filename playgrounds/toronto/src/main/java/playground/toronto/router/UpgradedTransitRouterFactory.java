package playground.toronto.router;

import com.google.inject.Provider;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.toronto.exceptions.NetworkFormattingException;
import playground.toronto.router.calculators.IterativeTransitTimeAndDisutility;
import playground.toronto.router.calculators.TransitDataCache;
import playground.toronto.router.routernetwork.TorontoTransitRouterNetworkImprovedEfficiency;

/**
 * Builds a {@link TransitRouter} using {@link IterativeTransitTimeAndDisutility} as its calculators;
 * 
 * @author pkucirek
 *
 */
public class UpgradedTransitRouterFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig config;
	private final TransitDataCache cache;
	private final TransitSchedule schedule;
	private final TransitRouterNetwork routerNetwork;
	
	public UpgradedTransitRouterFactory(Network network, TransitRouterConfig config, TransitSchedule schedule, TransitDataCache data) throws NetworkFormattingException {
		this.config = config;
		this.cache = data;
		this.schedule = schedule;
		this.routerNetwork = TorontoTransitRouterNetworkImprovedEfficiency.createTorontoTransitRouterNetwork(network, schedule, 0.1);
	}
	
	@Override
	public TransitRouter get() {
		IterativeTransitTimeAndDisutility calc =  new IterativeTransitTimeAndDisutility(cache, config);
			
		return new TransitRouterImpl(
				config, 
				new PreparedTransitSchedule(schedule),
				this.routerNetwork, 
				calc, calc);
	}

}
