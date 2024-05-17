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
package org.matsim.contrib.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.facilities.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dziemke
 */
public class AccessibilityUtils {
	public static final Logger LOG = LogManager.getLogger(AccessibilityUtils.class);

	/**
	 * Aggregates disutilities Vjk to get from node j to all k that are attached to j and assign sum(Vjk) is to node j.
	 *
	 *     j---k1
	 *     |\
	 *     | \
	 *     k2 k3
	 */
	public static final Map<Id<? extends BasicLocation>, AggregationObject> aggregateOpportunitiesWithSameNearestNode(
			final ActivityFacilities opportunities, Network network, Config config ) {
		// yyyy this method ignores the "capacities" of the facilities. kai, mar'14
		// for now, we decided not to add "capacities" as it is not needed for current projects. dz, feb'16

		double walkSpeed_m_h = config.routing().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with same nearest node...");
		Map<Id<? extends BasicLocation>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
			Node nearestNode = NetworkUtils.getNearestNode(network, opportunity.getCoord());
			double distance_m = NetworkUtils.getEuclideanDistance(opportunity.getCoord(), nearestNode.getCoord());

			// in MATSim this is [utils/h]: cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr()
			double walkBetaTT_utils_h = config.scoring().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling()
					- config.scoring().getPerforming_utils_hr(); // default values: -12 = (-6.) - (6.)
			double VjkWalkTravelTime = walkBetaTT_utils_h * (distance_m / walkSpeed_m_h);

			double expVjk = Math.exp(config.scoring().getBrainExpBeta() * VjkWalkTravelTime);

			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get(nearestNode.getId()); // Why "jco"?
			if (jco == null) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestNode, 0.);
				opportunityClusterMap.put(nearestNode.getId(), jco);
			}
			if (acg.isUseOpportunityWeights()) {
				if (opportunity.getAttributes().getAttribute( Labels.WEIGHT ) == null) {
					throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + Labels.WEIGHT + ".");
				} else {
					double weight = Double.parseDouble(opportunity.getAttributes().getAttribute( Labels.WEIGHT ).toString() );
					jco.addObject(opportunity.getId(), expVjk * Math.pow(weight, acg.getWeightExponent()));
				}
			} else {
				jco.addObject(opportunity.getId(), expVjk);
			}
		}
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " nodes.");
		return opportunityClusterMap;
	}


	public static Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregateMeasurePointsWithSameNearestNode(ActivityFacilities measuringPoints, Network network) {
		Map<Id<? extends BasicLocation>,ArrayList<ActivityFacility>> aggregatedOrigins = new ConcurrentHashMap<>();

		Gbl.assertNotNull(measuringPoints);
		Gbl.assertNotNull(measuringPoints.getFacilities()) ;
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {

			Node nearestNode = NetworkUtils.getCloserNodeOnLink(measuringPoint.getCoord(),	NetworkUtils.getNearestLinkExactly(network, measuringPoint.getCoord()));
			Id<Node> nearestNodeId = nearestNode.getId();

			if(!aggregatedOrigins.containsKey(nearestNodeId)) {
				aggregatedOrigins.put(nearestNodeId, new ArrayList<>());
			}
			aggregatedOrigins.get(nearestNodeId).add(measuringPoint);
		}
		LOG.info("Number of measuring points: " + measuringPoints.getFacilities().values().size());
		LOG.info("Number of aggregated measuring points: " + aggregatedOrigins.size());
		return aggregatedOrigins;
	}


	public static Network createModeSpecificSubNetwork(Network network, String mode, NetworkConfigGroup networkConfigGroup) {
		LOG.warn("Full network has " + network.getNodes().size() + " nodes.");
		Network subNetwork = NetworkUtils.createNetwork(networkConfigGroup);
		Set<String> modeSet = new HashSet<>();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		if (mode.equals(Modes4Accessibility.freespeed.name())) {
			modeSet.add(TransportMode.car);
		} else {
			modeSet.add(mode);
		}
		filter.filter(subNetwork, modeSet);
		if (subNetwork.getNodes().size() == 0) {throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");}
		LOG.warn("sub-network for mode " + modeSet.toString() + " now has " + subNetwork.getNodes().size() + " nodes.");
		return subNetwork;
	}


	public static double getModeSpecificConstantForAccessibilities(String mode, ScoringConfigGroup scoringConfigGroup) {
		double modeSpecificConstant;
		if (mode.equals(Modes4Accessibility.freespeed.name())) {
			modeSpecificConstant = scoringConfigGroup.getModes().get(TransportMode.car).getConstant();
		} else {
			modeSpecificConstant = scoringConfigGroup.getModes().get(mode).getConstant();
		}
		return modeSpecificConstant;
	}

	/**
	 * Collects all facilities of a given type that have been loaded to the sceanrio.
	 */
	public static ActivityFacilities collectActivityFacilitiesWithOptionOfType(Scenario scenario, String activityOptionType) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(activityOptionType) ;
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			if (activityOptionType == null) { // no activity option type for facility given, use all of them
				activityFacilities.addActivityFacility(facility);
			} else {
				for (ActivityOption option : facility.getActivityOptions().values()) {
					if (option.getType().equals(activityOptionType)) {
						activityFacilities.addActivityFacility(facility);
					}
				}
			}
		}
		return activityFacilities;
	}


	/**
	 * Collects the types of all facilities that have been loaded to the scenario.
	 */
	public static List<String> collectAllFacilityOptionTypes(Scenario scenario) {
		List<String> activityOptionTypes = new ArrayList<>() ;
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : facility.getActivityOptions().values()) {
				// collect all activity types that are contained within the provided facilities file
				if (!activityOptionTypes.contains(option.getType())) {
					activityOptionTypes.add(option.getType()) ;
				}
			}
		}
		LOG.warn("The following activity option types where found within the activity facilities: " + activityOptionTypes);
		return activityOptionTypes;
	}


	public static void combineDifferentActivityOptionTypes(final Scenario scenario, String combinedType, final List<String> activityOptionsToBeIncluded) {
		ActivityOption markerOption = new ActivityOptionImpl(combinedType);

		// Memorize all facilities that have certain activity options in a activity facilities container
		final ActivityFacilities consideredFacilities = FacilitiesUtils.createActivityFacilities();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : facility.getActivityOptions().values()) {
				if (activityOptionsToBeIncluded.contains(option.getType())) {
					// if (!option.getType().equals(FacilityTypes.HOME) && !option.getType().equals(FacilityTypes.WORK) && !option.getType().equals("minor")) {
					if (!consideredFacilities.getFacilities().containsKey(facility.getId())) {
						consideredFacilities.addActivityFacility(facility);
					}
				}
			}
		}

		// Add  marker option to facilities to be considered
		for (ActivityFacility facility : consideredFacilities.getFacilities().values()) {
			facility.addActivityOption(markerOption);
		}
	}


	public static final ActivityFacilities createFacilityForEachLink(String facilityContainerName, Network network) {
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities(facilityContainerName);
		ActivityFacilitiesFactory aff = facilities.getFactory();
		for (Link link : network.getLinks().values()) {
			ActivityFacility facility = aff.createActivityFacility(Id.create(link.getId(),ActivityFacility.class), link.getCoord(), link.getId());
			facilities.addActivityFacility(facility);
		}
		return facilities ;
	}


	public static final ActivityFacilities createFacilityFromBuildingShapefile(String shapeFileName, String identifierCaption, String numberOfHouseholdsCaption) {
		GeoFileReader shapeFileReader = new GeoFileReader();
		Collection<SimpleFeature> features = shapeFileReader.readFileAndInitialize(shapeFileName);

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("DensitiyFacilities");
		ActivityFacilitiesFactory aff = facilities.getFactory();

		for (SimpleFeature feature : features) {
			String featureId = (String) feature.getAttribute(identifierCaption);
			Integer numberOfHouseholds = Integer.parseInt((String) feature.getAttribute(numberOfHouseholdsCaption));
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Coord coord = CoordUtils.createCoord(geometry.getCentroid().getX(), geometry.getCentroid().getY());

			for (int i = 0; i < numberOfHouseholds; i++) {
				ActivityFacility facility = aff.createActivityFacility(Id.create(featureId + "_" + i, ActivityFacility.class), coord);
				facilities.addActivityFacility(facility);
			}
		}
		return facilities ;
	}


	/**
	 * Creates measuring points based on the scenario's network and a specified cell size.
	 */
	public static ActivityFacilities createMeasuringPointsFromNetworkBounds(Network network, int cellSize) {
		BoundingBox boundingBox = BoundingBox.createBoundingBox(network);
		double xMin = boundingBox.getXMin();
		double xMax = boundingBox.getXMax();
		double yMin = boundingBox.getYMin();
		double yMax = boundingBox.getYMax();

		ActivityFacilities measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(xMin, yMin, xMax, yMax, cellSize);
		return measuringPoints;
	}


	/**
	 * Calculates the sum of the values of a given list.
	 *
	 * @param valueList
	 * @return sum
	 */
	public static double calculateSum(List<Double> valueList) {
		double sum = 0.;
		for (double i : valueList) {
			sum = sum + i;
		}
		return sum;
	}


	/**
	 * Calculates Gini coefficient of the values of a given values. The Gini Coefficient is equals to the half of
	 * the relative mean absolute difference (RMD).
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Gini_coefficient">
	 * @see <a href="https://en.wikipedia.org/wiki/Mean_absolute_difference#Relative_mean_absolute_difference">
	 * @param valueList
	 * @return giniCoefficient
	 */
	public static double calculateGiniCoefficient(List<Double> valueList) {
		int numberOfValues = valueList.size();
		double sumOfValues = calculateSum(valueList);
		double arithmeticMean = sumOfValues / numberOfValues;

		double sumOfAbsoluteDifferences = 0.;
		for (double i : valueList) {
			for (double j : valueList) {
				double absoulteDifference = Math.abs( i - j );
				sumOfAbsoluteDifferences = sumOfAbsoluteDifferences + absoulteDifference;
			}
		}
		double giniCoefficient = sumOfAbsoluteDifferences / (2 * Math.pow(numberOfValues, 2) * arithmeticMean);
		return giniCoefficient;
	}


	/**
	 * Creates facilities from plans. Note that a new additional facility is created for each activity.
	 * @param population
	 * @return
	 */
	public static ActivityFacilities createFacilitiesFromPlans(Population population) {
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				Id <Person> personId = person.getId();

				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;

						Coord coord= activity.getCoord();
						if (coord == null) {
							throw new NullPointerException("Activity does not have any coordinates.");
						}

						String activityType = activity.getType();

						// In case an agent visits the same activity location twice, create another activity facility with a modified ID
						Integer i = 1;
						Id<ActivityFacility> facilityId = Id.create(activityType + "_" + personId.toString() + "_" + i.toString(), ActivityFacility.class);
						while (facilities.getFacilities().containsKey(facilityId)) {
							i++;
							facilityId = Id.create(activityType + "_" + personId.toString() + "_" + i.toString(), ActivityFacility.class);
						}

						ActivityFacility facility = aff.createActivityFacility(facilityId, activity.getCoord());

						facility.addActivityOption(aff.createActivityOption(activityType));
						facilities.addActivityFacility(facility);
//						log.info("Created activity with option of type " + activityType + " and ID " + facilityId + ".");
					}
				}
			}
		}
		return facilities;
	}


	public static String getDate() {
		Calendar cal = Calendar.getInstance ();
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-"
				+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
		return date;
	}


	public static void assignAdditionalFacilitiesDataToMeasurePoint(ActivityFacilities measurePoints, Map<Id<ActivityFacility>, Geometry> measurePointGeometryMap,
			Map<String, ActivityFacilities> additionalFacilityData) {
		LOG.info("Start assigning additional facilities data to measure point.");
		GeometryFactory geometryFactory = new GeometryFactory();

		for (ActivityFacilities additionalDataFacilities : additionalFacilityData.values()) { // Iterate over all additional data collections
			String additionalDataName = additionalDataFacilities.getName();
			int additionalDataFacilitiesToAssign = additionalDataFacilities.getFacilities().size();

			for (Id<ActivityFacility> measurePointId : measurePoints.getFacilities().keySet()) { // Iterate over all measure points
				ActivityFacility measurePoint = measurePoints.getFacilities().get(measurePointId);
				measurePoint.getAttributes().putAttribute(additionalDataName, 0);
				Geometry geometry = measurePointGeometryMap.get(measurePointId);

				for (ActivityFacility facility : additionalDataFacilities.getFacilities().values()) { // Iterate over additional-data facilities
					Point point = geometryFactory.createPoint(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
					if (geometry.contains(point)) {
						measurePoint.getAttributes().putAttribute(additionalDataName, (int) measurePoint.getAttributes().getAttribute(additionalDataName) + 1);
						additionalDataFacilitiesToAssign--;
					}
				}
			}
			LOG.warn(additionalDataFacilitiesToAssign + " additional data facilies have not been assigned to a measure point geometry.");
		}
		LOG.info("Finished assigning additional facilities data to measure point.");
	}
}
