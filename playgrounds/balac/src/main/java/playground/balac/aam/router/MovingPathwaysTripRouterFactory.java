package playground.balac.aam.router;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.*;

import javax.inject.Provider;


public class MovingPathwaysTripRouterFactory implements Provider<TripRouter>{

	private final Provider<TripRouter> delegate;
	private Scenario scenario;
	
	public MovingPathwaysTripRouterFactory (Provider<TripRouter> delegate, Scenario scenario) {
		
		this.scenario = scenario;
		this.delegate = delegate;
		
	}
	
	public MovingPathwaysTripRouterFactory(
			final Scenario scenario ) {
		this(TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(scenario),
				scenario );
	}
	
	@Override
	public TripRouter get() {
		// TODO Auto-generated method stub		

		final TripRouter router = delegate.get();
		
		// add our module to the instance
		router.setRoutingModule(
			"movingpathways",
			new AAMRoutingModule(this.scenario));


	/*
		router.setRoutingModule(TransportMode.pt,
				new TransitMultiModalAccessRoutingModule(
						scenario,
						new InitialNodeRouter(
							router.getRoutingModule( "movingpathways" ),
							scenario.getConfig().transitRouter().getSearchRadius(),
							1,
							scoringParams )
						
						) );
		
		*/
		final MainModeIdentifier defaultModeIdentifier = router.getMainModeIdentifier();
		router.setMainModeIdentifier(
				new MainModeIdentifier() {
					@Override
					public String identifyMainMode(
							final List<? extends PlanElement> tripElements) {
						boolean hadMovingPathway = false;
						for ( PlanElement pe : tripElements ) {
							if ( pe instanceof Leg ) {
								final Leg l = (Leg) pe;
								if ( l.getMode().equals( "movingpathways" ) ) {
									hadMovingPathway = true;
								}
								if ( l.getMode().equals( TransportMode.transit_walk ) ) {
									return TransportMode.pt;
								}
							}
						}

						if ( hadMovingPathway ) {
							// there were bike sharing legs but no transit walk
							return "movingpathways";
						}

						return defaultModeIdentifier.identifyMainMode( tripElements );
					}
				});
		
		return router;
					
	}

}
