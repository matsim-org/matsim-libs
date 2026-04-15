package org.matsim.contrib.perceivedsafety;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class PerceivedSafetyDisutility implements TravelDisutility {
	private static final Logger log = LogManager.getLogger(PerceivedSafetyDisutility.class);

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

		String currentMode = vehicle.getType().getNetworkMode();

		PerceivedSafetyConfigGroup perceivedSafetyConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), PerceivedSafetyConfigGroup.class);
		PerceivedSafetyConfigGroup.PerceivedSafetyModeParams params = perceivedSafetyConfigGroup.getModes().get(currentMode);

//		we have to define the following params down here instead of the constructor
//		because we do not have the vehicle and therefore the mode there
		double marginalCostOfPerceivedSafety = -(params.getMarginalUtilityOfPerceivedSafetyPerM());
		// set sd beta_psafe, depends on the transport mode
		double sdPerceivedSafety = params.getMarginalUtilityOfPerceivedSafetyPerMSd();
		// set dmax, depends on the transport mode
		double dMax = params.getDMaxPerM();

		double distance = link.getLength();
		double perceivedSafetyValueOnLink = additionalPerceivedSafetyLinkScoreDefault
			.computePerceivedSafetyValueOnLink(link, currentMode, additionalPerceivedSafetyLinkScoreDefault.inputPerceivedSafetyThreshold);
		double distanceBasedPerceivedSafety = perceivedSafetyValueOnLink * distance;

		// in case you want to estimate the additional score based on the weighted mean
		if (dMax == 0) {
			dMax = distance;
		}

		//divide by dmax at the end
		distanceBasedPerceivedSafety = distanceBasedPerceivedSafety / dMax;
		// run Monte Carlo Simulation for the safety perceptions
		double r = additionalPerceivedSafetyLinkScoreDefault.generator.nextGaussian();
		// multiply with the random beta parameter
		double perceivedSafetyDisutility = (marginalCostOfPerceivedSafety + r * sdPerceivedSafety) * distanceBasedPerceivedSafety;

		double disutility = defaultTravelDisutilityForLink + perceivedSafetyDisutility * logNormalRnd;

		if (disutility < 0.) {
//			right now, perceived safety travel disutility can be either positive or negative.
//			while I am not entirely sure if this is conceptually a good idea for now we are going to allow it.
//			At least, as long as the perceived safety disutility component does not make the overall
//			link disutility negative, which would mean that an agent gains utility from traveling that link. -sm0925
			log.fatal("Disutility of {} for link {} was calculated. " +
				"A negative disutility (-1 * -1) means that there is additional utility when traveling on link {}. This should not happen. Aborting!",
				disutility, link.getId(), link.getId());
			throw new IllegalStateException();
		}
		return disutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}
