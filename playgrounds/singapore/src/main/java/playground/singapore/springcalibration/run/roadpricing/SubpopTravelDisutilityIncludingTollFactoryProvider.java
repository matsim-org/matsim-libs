package playground.singapore.springcalibration.run.roadpricing;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;

public class SubpopTravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {
	private final Scenario scenario;
    private final RoadPricingScheme scheme;
    private TravelDisutilityFactory originalTravelDisutilityFactory;
    private CharyparNagelScoringParametersForPerson parameters;

    @Inject
    SubpopTravelDisutilityIncludingTollFactoryProvider(Scenario scenario, RoadPricingScheme scheme, TravelDisutilityFactory originalTravelDisutilityFactory, CharyparNagelScoringParametersForPerson parameters) {
        this.scenario = scenario;
        this.scheme = scheme;
        this.originalTravelDisutilityFactory = originalTravelDisutilityFactory;
        this.parameters = parameters;
    }

    @Override
    public TravelDisutilityFactory get() {
    	    	
        RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
        
        SubpopRoadPricingTravelDisutilityFactory travelDisutilityFactory = new SubpopRoadPricingTravelDisutilityFactory(
                originalTravelDisutilityFactory, scheme, parameters);
        travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
        return travelDisutilityFactory;
    }

}

