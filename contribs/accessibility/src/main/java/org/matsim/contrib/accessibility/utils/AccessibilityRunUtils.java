package org.matsim.contrib.accessibility.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author dziemke
 */
public class AccessibilityRunUtils {
	public static final Logger log = Logger.getLogger(AccessibilityRunUtils.class);
	
	/**
	 * Collects all facilities of a given type that have been loaded to the sceanrio.
	 * 
	 * @param scenario
	 * @param activityOptionType
	 * @return
	 */
	public static ActivityFacilities collectActivityFacilitiesWithOptionOfType(Scenario scenario, String activityOptionType) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(activityOptionType) ;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : fac.getActivityOptions().values()) {
				if (option.getType().equals(activityOptionType)) {
					activityFacilities.addActivityFacility(fac);
				}
			}
		}
		return activityFacilities;
	}

	
	/**
	 * Collects the types of all facilities that have been loaded to the scenario.
	 * 
	 * @param scenario
	 * @return
	 */
	public static List<String> collectAllFacilityOptionTypes(Scenario scenario) {
		List<String> activityOptionTypes = new ArrayList<String>() ;
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : facility.getActivityOptions().values()) {
				// collect all activity types that are contained within the provided facilities file
				if (!activityOptionTypes.contains(option.getType())) {
					activityOptionTypes.add(option.getType()) ;
				}
			}
		}
		return activityOptionTypes;
	}

	
	/**
	 * Goes through a given set of measuring points and creates a facility on the measuring point if the
	 * nearest link from that measure point lies within a specified maximum allowed distance. The presence
	 * of such networkDensityFacilites can then be used to plot a density layer, e.g. to excluded tiles
	 * from being drawn if there is no network. The network density is thereby used as a proxy for settlement
	 * density if no information of settlement density is available.
	 * 
	 * @param network
	 * @param measuringPoints
	 * @param cellSize
	 * @return
	 */
	public static ActivityFacilities createNetworkDensityFacilities(Network network,
			ActivityFacilities measuringPoints, double maximumAllowedDistance) {
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities networkDensityFacilities = FacilitiesUtils.createActivityFacilities("network_densities");

		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values() ) {
			Coord coord = measuringPoint.getCoord();
			Link link = NetworkUtils.getNearestLink(network, coord);
			double distance = ((LinkImpl) link).calcDistance(coord);
			if (distance <= maximumAllowedDistance) {
				ActivityFacility facility = aff.createActivityFacility(measuringPoint.getId(), coord);
				networkDensityFacilities.addActivityFacility(facility);
			}
		}
		return networkDensityFacilities;
	}


	/**
	 * Creates measuring points based on the scenario's network and a specified cell size.
	 * 
	 * @param scenario
	 * @param cellSize
	 * @return
	 */
	public static ActivityFacilities createMeasuringPointsFromNetwork(Network network, double cellSize) {
		BoundingBox boundingBox = BoundingBox.createBoundingBox(network);
		double xMin = boundingBox.getXMin();
		double xMax = boundingBox.getXMax();
		double yMin = boundingBox.getYMin();
		double yMax = boundingBox.getYMax();
		
		ActivityFacilities measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(
				xMin, yMin, xMax, yMax, cellSize);
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
//		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(facilities);
//		facilitiesWriter.write("../../../public-svn/matsim/specifiy_location/...xml.gz");
		return facilities;
	}
}