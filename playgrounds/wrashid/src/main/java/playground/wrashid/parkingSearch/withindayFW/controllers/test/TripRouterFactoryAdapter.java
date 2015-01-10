package playground.wrashid.parkingSearch.withindayFW.controllers.test;

import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;

import javax.inject.Provider;

public class TripRouterFactoryAdapter implements Provider<TripRouter> {

	public TripRouterFactoryAdapter(TripRouterFactory multimodalTripRouterFactory) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public TripRouter get() {
		// TODO Auto-generated method stub
		return null;
	}

}
