package playground.toronto.transitfares;

import java.util.HashMap;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.toronto.transitfares.FareBasedTravelTimeCost;

/**
 * Modified generalized-cost multi-node Dijkstra algorithm (as used for pt)
 * for including the disutility of transfer fares. Currently in development.
 * 
 *  @author pkucirek
*/

public class FareBasedTransitRouterFactory implements TransitRouterFactory {
	
	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final HashMap<String, HashMap<Tuple<String,String>, Double>> farelookuptable;
	private final ObjectAttributes StopZoneMap;
	private final ObjectAttributes PersonFareClassMap;
	private final Double tvm;
	
	public FareBasedTransitRouterFactory(final TransitSchedule schedule, 
			final TransitRouterConfig config,
			HashMap<String, HashMap<Tuple<String,String>, Double>> farelookuptable,
			final ObjectAttributes StopZoneMap,
			final ObjectAttributes PersonFareClassMap,
			final Double timevalueofmoney) {
		
		this.schedule = schedule;
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, 
				this.config.beelineWalkConnectionDistance);
		this.farelookuptable = farelookuptable;
		this.StopZoneMap = StopZoneMap;
		this.PersonFareClassMap = PersonFareClassMap;
		this.tvm = timevalueofmoney;
	}
	
	@Override
	public TransitRouter createTransitRouter() {
		FareBasedTravelTimeCost f = new FareBasedTravelTimeCost(this.config, this.farelookuptable, this.StopZoneMap, this.PersonFareClassMap, this.tvm);
		return new TransitRouterImpl(this.config, routerNetwork, f, f);
		
		
		/*
		return new TransitRouterImpl(this.schedule, this.config, 
				new FareBasedTravelTimeCost(this.config,this.farelookuptable,this.StopZoneMap,this.PersonFareClassMap,this.tvm),
				this.routerNetwork);
		*/
	}
}

	
