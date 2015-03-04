package playground.balac.aam.router;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class MovingPathwaysTripRouterFactory implements TripRouterFactory{

	private final TripRouterFactory delegate;
	private Scenario scenario;
	
	public MovingPathwaysTripRouterFactory ( TripRouterFactory delegate, Scenario scenario) {
		
		this.scenario = scenario;
		this.delegate = delegate;
		
	}
	
	public MovingPathwaysTripRouterFactory(
			final Scenario scenario ) {
		this( DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(scenario) ,
				scenario );
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(
			RoutingContext routingContext) {
		// TODO Auto-generated method stub		

		final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);
		
		// add our module to the instance
		router.setRoutingModule(
			"movingpathways",
			new AAMRoutingModule(this.scenario));
		
		
		final CharyparNagelScoringParameters scoringParams =
				new CharyparNagelScoringParameters(
					scenario.getConfig().planCalcScore() );
		
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
							final List<PlanElement> tripElements) {
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
