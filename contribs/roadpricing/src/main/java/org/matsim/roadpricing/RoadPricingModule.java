package org.matsim.roadpricing;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule.RoadPricingInitializer;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule.RoadPricingSchemeProvider;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule.TravelDisutilityIncludingTollFactoryProvider;

import com.google.inject.Singleton;

final class RoadPricingModule extends AbstractModule {
	
	private RoadPricingScheme scheme;

	RoadPricingModule() {
		
	}
	
	RoadPricingModule( RoadPricingScheme scheme ) {
		this.scheme = scheme;
		
	}
	
	@Override
	public void install() {
		ConfigUtils.addOrGetModule(getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		
		
		// TODO sort out different ways to set toll schemes; reduce automagic 
		if ( scheme != null) {
			bind(RoadPricingScheme.class).toInstance(scheme);
		} else {
			bind(RoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
		}
		bind(RoadPricingInitializer.class).asEagerSingleton();
		
		// TODO can probably combine the following two:
		bind(PlansCalcRouteWithTollOrNot.class);
		addPlanStrategyBinding("ReRouteAreaToll").toProvider(ReRouteAreaToll.class);

		addTravelDisutilityFactoryBinding(TransportMode.car).toProvider(TravelDisutilityIncludingTollFactoryProvider.class).asEagerSingleton();

		addTravelDisutilityFactoryBinding("car_with_payed_area_toll").toInstance(new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, getConfig().planCalcScore()));
		addRoutingModuleBinding("car_with_payed_area_toll").toProvider(new RoadPricingNetworkRouting());

		addControlerListenerBinding().to(RoadPricingControlerListener.class);

		// add the events handler to calculate the tolls paid by agents
		bind(CalcPaidToll.class).in(Singleton.class);

		bind(CalcAverageTolledTripLength.class).in(Singleton.class);
	}
}