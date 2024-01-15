/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.bicycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

/**
 * @author smetzler, dziemke
 * based on RandomizingTimeDistanceTravelDisutility and adding more components
 */
class BicycleTravelDisutility implements TravelDisutility {
	private static final Logger LOG = LogManager.getLogger(BicycleTravelDisutility.class);

	private final double marginalCostOfTime_s;
	private final double marginalCostOfDistance_m;
	private final double marginalCostOfInfrastructure_m;
	private final double marginalCostOfComfort_m;
	private final double marginalCostOfGradient_m_100m;
	private final double marginalCostOfUserDefinedNetworkAttribute_m;
	private final String nameOfUserDefinedNetworkAttribute;
	private final double userDefinedNetworkAttributeDefaultValue;

	private final double normalization;
	private final double sigma;

	private final Random random;

	private final TravelTime timeCalculator;

	// "cache" of the random value
	private double normalRndLink;
	private double logNormalRndDist;
	private double logNormalRndInf;
	private double logNormalRndComf;
	private double logNormalRndGrad;
	private double logNormalRndUserDef;
	private Person prevPerson;


	BicycleTravelDisutility(BicycleConfigGroup bicycleConfigGroup, ScoringConfigGroup cnScoringGroup,
													RoutingConfigGroup routingConfigGroup, TravelTime timeCalculator, double normalization) {
		final ScoringConfigGroup.ModeParams bicycleParams = cnScoringGroup.getModes().get(bicycleConfigGroup.getBicycleMode());
		if (bicycleParams == null) {
			throw new NullPointerException("Mode " + bicycleConfigGroup.getBicycleMode() + " is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
		}

		this.marginalCostOfDistance_m = -(bicycleParams.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney())
				- bicycleParams.getMarginalUtilityOfDistance();
		this.marginalCostOfTime_s = -(bicycleParams.getMarginalUtilityOfTraveling() / 3600.0) + cnScoringGroup.getPerforming_utils_hr() / 3600.0;

		this.marginalCostOfInfrastructure_m = -(bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m());
		this.marginalCostOfComfort_m = -(bicycleConfigGroup.getMarginalUtilityOfComfort_m());
		this.marginalCostOfGradient_m_100m = -(bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m());
		this.marginalCostOfUserDefinedNetworkAttribute_m = -(bicycleConfigGroup.getMarginalUtilityOfUserDefinedNetworkAttribute_m());
		this.nameOfUserDefinedNetworkAttribute = bicycleConfigGroup.getUserDefinedNetworkAttributeName();
		this.userDefinedNetworkAttributeDefaultValue = bicycleConfigGroup.getUserDefinedNetworkAttributeDefaultValue();

		this.timeCalculator = timeCalculator;

		this.normalization = normalization;
		this.sigma = routingConfigGroup.getRoutingRandomness();
		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double travelTime = timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		String surface = BicycleUtils.getSurface( link );
		String type = NetworkUtils.getType( link ) ;
		String cyclewaytype = BicycleUtils.getCyclewaytype( link );
		double distance = link.getLength();

		double travelTimeDisutility = marginalCostOfTime_s * travelTime;
		double distanceDisutility = marginalCostOfDistance_m * distance;

		double comfortFactor = BicycleUtils.getComfortFactor(surface );
		double comfortDisutility = marginalCostOfComfort_m * (1. - comfortFactor) * distance;

		double infrastructureFactor = BicycleUtils.getInfrastructureFactor(type, cyclewaytype );
		double infrastructureDisutility = marginalCostOfInfrastructure_m * (1. - infrastructureFactor) * distance;

		double gradientFactor = BicycleUtils.getGradient(link );
		double gradientDisutility = marginalCostOfGradient_m_100m * gradientFactor * distance;

		double userDefinedNetworkAttritubeDisutility = 0.;
		if (nameOfUserDefinedNetworkAttribute != null) {
			String userDefinedNetworkAttributeString = BicycleUtils.getUserDefinedNetworkAttribute(link, nameOfUserDefinedNetworkAttribute);
			double userDefinedNetworkAttributeFactor = BicycleUtils.getUserDefinedNetworkAttributeFactor(userDefinedNetworkAttributeString,
					this.userDefinedNetworkAttributeDefaultValue );
			userDefinedNetworkAttritubeDisutility = marginalCostOfUserDefinedNetworkAttribute_m * (1. - userDefinedNetworkAttributeFactor) * distance;
		}

//		LOG.warn("link = " + link.getId() + "-- travelTime = " + travelTime + " -- distance = " + distance + " -- comfortFactor = "
//				+ comfortFactor	+ " -- infraFactor = "+ infrastructureFactor + " -- gradient = " + gradientFactor);

		// TODO Gender
		// TODO Activity
		// TODO Other influence factors

		// randomize if applicable:
		if (sigma != 0.) {
			if (person==null) {
				throw new RuntimeException("you cannot use the randomzing travel disutility without person.  If you need this without a person, set"
						+ "sigma to zero.") ;
			}
//			normalRndLink = 0.05 * random.nextGaussian();
			// are we sure that this is a good approach?  In high resolution networks, this leads to quirky detours ...  kai, sep'19
			// --> see below.  kai, jul'23
			if (person != prevPerson) {
				prevPerson = person;

				normalRndLink = 0.05 * random.nextGaussian();
				// are we sure that this is a good approach?  In high resolution networks, this leads to quirky detours ...  kai, sep'19
				// --> addressed with moving it to down here, i.e. into the person.  Also caused race conditions. kai, jul'23

				logNormalRndDist = Math.exp(sigma * random.nextGaussian());
				logNormalRndInf = Math.exp(sigma * random.nextGaussian());
				logNormalRndComf = Math.exp(sigma * random.nextGaussian());
				logNormalRndGrad = Math.exp(sigma * random.nextGaussian());
				logNormalRndUserDef = Math.exp(sigma * random.nextGaussian());
				logNormalRndDist *= normalization;
				logNormalRndInf *= normalization;
				logNormalRndComf *= normalization;
				logNormalRndGrad *= normalization;
				logNormalRndUserDef *= normalization;
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
			normalRndLink = 1.;
			logNormalRndDist = 1.;
			logNormalRndInf = 1.;
			logNormalRndComf = 1.;
			logNormalRndGrad = 1.;
			logNormalRndUserDef = 1.;
		}

//		LOG.warn("Person = " + person.getId() + " / link = " + link.getId() + " / ttD = " + travelTimeDisutility	+ " / dD = "+ distanceDisutility
//				+ " / infD = " + infrastructureDisutility + " / comfD = " + comfortDisutility + " / gradD = " + gradientDisutility + " / rnd = " + normalRndLink
//				+ " / rndDist = " + logNormalRndDist + " / rndInf = "	+ logNormalRndInf + " / rndComf = " + logNormalRndComf + " / rndGrad = " + logNormalRndGrad);
		double disutility = (1 + normalRndLink) * travelTimeDisutility + logNormalRndDist * distanceDisutility + logNormalRndInf * infrastructureDisutility
				+ logNormalRndComf * comfortDisutility + logNormalRndGrad * gradientDisutility + logNormalRndUserDef * userDefinedNetworkAttritubeDisutility;
		// note that "normalRndLink" follows a Gaussian distribution, not a lognormal one as the others do!
//		double disutility = travelTimeDisutility + logNormalRndDist * distanceDisutility + (1 + normalRndLink) * logNormalRndInf * infrastructureDisutility
//				+ (1 + normalRndLink) * logNormalRndComf * comfortDisutility + (1 + normalRndLink) * logNormalRndGrad * gradientDisutility;
//		LOG.warn("Disutility = " + disutility);
		return disutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}
