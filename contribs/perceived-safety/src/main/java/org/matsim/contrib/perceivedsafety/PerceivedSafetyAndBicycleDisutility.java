package org.matsim.contrib.perceivedsafety;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleParams;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

public class PerceivedSafetyAndBicycleDisutility implements TravelDisutility {
	private static final Logger log = LogManager.getLogger(PerceivedSafetyAndBicycleDisutility.class);

	@Inject
	private final BicycleParams bicycleParams;
	private final TravelDisutility defaultTravelDisutility;
	private final Scenario scenario;
	private final double sigma;
	private final double normalization;

	private final double marginalCostOfInfrastructurePerM;
	private final double marginalCostOfComfortPerM;
	private final double marginalCostOfGradientPer100M;

	// "cache" of the random value
	private double logNormalRndInf;
	private double logNormalRndComf;
	private double logNormalRndGrad;
	private Person prevPerson;

	private final Random random;


	public PerceivedSafetyAndBicycleDisutility(Scenario scenario, TravelDisutility defaultTravelDisutility, double sigma, BicycleParams bicycleParams, double normalization) {
		this.scenario = scenario;
		this.defaultTravelDisutility = defaultTravelDisutility;
		this.sigma = sigma;
		this.bicycleParams = bicycleParams;
		this.normalization = normalization;

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), BicycleConfigGroup.class);

		this.marginalCostOfInfrastructurePerM = -(bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m());
		this.marginalCostOfComfortPerM = -(bicycleConfigGroup.getMarginalUtilityOfComfort_m());
		this.marginalCostOfGradientPer100M = -(bicycleConfigGroup.getMarginalUtilityOfGradient_pct_m());

		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
//		first step: get normal disutility for link, so without perceived safety
		double defaultTravelDisutilityForLink = this.defaultTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);

		double distance = link.getLength();

//		get disutility from bicycle contrib
//		in BicycleContrib, each component of the bicycleDisutility (comfort, infrastructure and gradient)
//		is randomized individually. Therefore, we also do it here.
//		Although I think that a single randomization of bicycleScore would be enough and easier. -sm0925
		double bicycleDisutility = calcBicycleDisutility(link, person, distance);

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

		//		bicycleDisutility already is randomized here
		double disutility = defaultTravelDisutilityForLink + perceivedSafetyDisutility * logNormalRnd + bicycleDisutility;

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

	private double calcBicycleDisutility(Link link, Person person, double distance) {
		double comfortFactor = bicycleParams.getComfortFactor(BicycleUtils.getSurface(link));
		double comfortDisutility = marginalCostOfComfortPerM * (1. - comfortFactor) * distance;

		double infrastructureFactor = bicycleParams.getInfrastructureFactor(NetworkUtils.getType(link), BicycleUtils.getCyclewaytype(link));
		double infrastructureDisutility = marginalCostOfInfrastructurePerM * (1. - infrastructureFactor) * distance;

		double gradientFactor = bicycleParams.getGradient_pct(link);
		double gradientDisutility = marginalCostOfGradientPer100M * gradientFactor * distance;

		// randomize if applicable:
		if (sigma != 0.) {
			if (person ==null) {
				throw new NullPointerException("you cannot use the randomzing travel disutility without person. If you need this without a person, set"
					+ "sigma to zero.") ;
			}

			if (person != prevPerson) {
				prevPerson = person;

				logNormalRndInf = Math.exp(sigma * random.nextGaussian());
				logNormalRndComf = Math.exp(sigma * random.nextGaussian());
				logNormalRndGrad = Math.exp(sigma * random.nextGaussian());
				logNormalRndInf *= normalization;
				logNormalRndComf *= normalization;
				logNormalRndGrad *= normalization;
				// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
				// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

				/* The argument is something like this:<ul>
				 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
				 * <li> The mean of this is exp( mu + sigma^2/2 ) .
				 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
				 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
				 * </ul>
				 * Should be tested. kai, jan'14 */
			}
		} else {
			logNormalRndInf = 1.;
			logNormalRndComf = 1.;
			logNormalRndGrad = 1.;
		}
		return logNormalRndInf * infrastructureDisutility + logNormalRndComf * comfortDisutility + logNormalRndGrad * gradientDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}
