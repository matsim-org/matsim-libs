package org.matsim.contrib.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.roadpricing.RoadPricingModuleDefaults.RoadPricingInitializer;
import org.matsim.contrib.roadpricing.RoadPricingModuleDefaults.RoadPricingSchemeProvider;
import org.matsim.contrib.roadpricing.RoadPricingModuleDefaults.TravelDisutilityIncludingTollFactoryProvider;

import com.google.inject.Singleton;

public final class RoadPricingModule extends AbstractModule {
	final private static Logger LOG = Logger.getLogger(RoadPricingModule.class);
	
	private RoadPricingScheme scheme;

	public RoadPricingModule() {	}

	/* For the time being this has to be public, otherwise the roadpricing TollFactor
	cannot be considered, rendering integration tests useless, JWJ Jan'20 */
	public RoadPricingModule( RoadPricingScheme scheme ) {
		this.scheme = scheme;
	}
	
	@Override
	public void install() {
		ConfigUtils.addOrGetModule(getConfig(), RoadPricingConfigGroup.class);
		
		// TODO sort out different ways to set toll schemes; reduce automagic
		// TODO JWJ: is this still too "automagic"?
		if ( scheme != null) {
			// scheme has come in from the constructor, use that one:
			bind(RoadPricingScheme.class).toInstance(scheme);
		} else {
			// no scheme has come in from the constructor, use a class that reads it from file:
			bind(RoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
		}
		// also add RoadPricingScheme as ScenarioElement.  yyyy TODO might try to get rid of this; binding it is safer
		// (My personal preference is actually to have it as scenario element ... since then it can be set before controler is even called.  Which
		// certainly makes more sense for a clean build sequence.  kai, oct'19)
		bind(RoadPricingInitializer.class).in( Singleton.class );

		// add the toll to the routing disutility.  also includes "randomizing":
		addTravelDisutilityFactoryBinding(TransportMode.car).toProvider(TravelDisutilityIncludingTollFactoryProvider.class);

//		// specific re-routing strategy for area toll:
//		// yyyy TODO could probably combine them somewhat
		addPlanStrategyBinding("ReRouteAreaToll").toProvider(ReRouteAreaToll.class);
		addTravelDisutilityFactoryBinding( PlansCalcRouteWithTollOrNot.CAR_WITH_PAYED_AREA_TOLL ).toInstance(new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, getConfig()) );
		addRoutingModuleBinding( PlansCalcRouteWithTollOrNot.CAR_WITH_PAYED_AREA_TOLL ).toProvider(new RoadPricingNetworkRouting() );
		
		// yyyy TODO It might be possible that the area stuff is adequately resolved by the randomizing approach.  Would need to try
		// that out.  kai, sep'16

		// this is what makes the mobsim compute tolls and generate money events
		addControlerListenerBinding().to(RoadPricingControlerListener.class);
		
	}
}
