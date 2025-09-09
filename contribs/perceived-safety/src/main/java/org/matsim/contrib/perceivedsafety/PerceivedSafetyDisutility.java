package org.matsim.contrib.perceivedsafety;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class PerceivedSafetyDisutility implements TravelDisutility {
	private final TravelDisutility defaultTravelDisutility;
	private final Scenario scenario;
	private final double sigma;

	public PerceivedSafetyDisutility(Scenario scenario, TravelDisutility defaultTravelDisutility, double sigma) {
		this.scenario = scenario;
		this.defaultTravelDisutility = defaultTravelDisutility;
		this.sigma = sigma;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
//		first step: get normal disutility for link, so without perceived safety
		double defaultTravelDisutilityForLink = this.defaultTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);

		double logNormalRnd = 1. ;
//		randomize if applicable
//		logNormalRnd person attr is written to custom person attrs in RandomizingTimeDistanceTravelDisutility
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}

//		get perceived safety disutility.
//		Use code which we already have in AdditionalPerceivedSafetyLinkScoreDefaultImpl
		AdditionalPerceivedSafetyLinkScoreDefaultImpl additionalPerceivedSafetyLinkScoreDefault = new AdditionalPerceivedSafetyLinkScoreDefaultImpl(scenario);
		double perceivedSafetyDisutility = additionalPerceivedSafetyLinkScoreDefault.computeLinkBasedScore(link, vehicle.getId());

//		travel (dis)utilities are negative, therefore we have to use addition in the following
		return defaultTravelDisutilityForLink + perceivedSafetyDisutility * logNormalRnd;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}
