package org.matsim.contrib.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.roadpricing.RoadPricingModuleDefaults.TravelDisutilityIncludingTollFactoryProvider;

import com.google.inject.Singleton;

public final class RoadPricingModule extends AbstractModule {
	final private static Logger LOG = Logger.getLogger(RoadPricingModule.class);
	
	@Override
	public void install() {
//		Thread.dumpStack();
		LOG.warn(" !!! Creating RoadPricingConfigGroup !!!");
//		ConfigUtils.addOrGetModule(getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);


		// TODO sort out different ways to set toll schemes; reduce automagic
		// TODO JWJ: is this still too "automagic"?
//		if ( scheme != null) {
//			// scheme has come in from the constructor, use that one:
//			bind(RoadPricingScheme.class).toInstance(scheme);
//		} else {
//			throw new RuntimeException("no road pricing scheme loaded when controler is started; aborting") ;
			bind(RoadPricingScheme.class).toProvider( RoadPricingModuleDefaults.RoadPricingSchemeProvider.class ).in( Singleton.class ) ;
//		}
		// also add RoadPricingScheme as ScenarioElement.  yyyy TODO might try to get rid of this; binding it is safer
//		bind(RoadPricingInitializer.class).asEagerSingleton();

		// this is what makes the mobsim compute tolls and generate money events
		// TODO yyyy could probably combine the following two:
		addControlerListenerBinding().to(RoadPricingControlerListener.class).in( Singleton.class ) ;

		// add the toll to the routing disutility.  also includes "randomizing":
		addTravelDisutilityFactoryBinding(TransportMode.car).toProvider(TravelDisutilityIncludingTollFactoryProvider.class).in( Singleton.class ) ;

		// specific re-routing strategy for area toll:
		// yyyy TODO could probably combine them somewhat
//		bind(PlansCalcRouteWithTollOrNot.class);
//		addPlanStrategyBinding("ReRouteAreaToll").toProvider(ReRouteAreaToll.class);
//		addTravelDisutilityFactoryBinding("car_with_payed_area_toll").toInstance(new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, getConfig().planCalcScore()));
//		addRoutingModuleBinding("car_with_payed_area_toll").toProvider(new RoadPricingNetworkRouting());

		// yyyy TODO It might be possible that the area stuff is adequately resolved by the randomizing approach.  Would need to try
		// that out.  kai, sep'16




//		bind(RoadPricingTollCalculator.class).in(Singleton.class);

		// this is for analysis only:
//		bind(CalcAverageTolledTripLength.class).in(Singleton.class);
	}
}
