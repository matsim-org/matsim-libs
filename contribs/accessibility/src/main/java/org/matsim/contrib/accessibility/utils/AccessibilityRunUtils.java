package org.matsim.contrib.accessibility.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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

/**
 * @author dziemke
 */
public class AccessibilityRunUtils {
	
	/**
	 * Collects all facilities of a given type that have been loaded to the sceanrio.
	 * 
	 * @param scenario
	 * @param activityFacilityType
	 * @return
	 */
	public static ActivityFacilities collectActivityFacilitiesOfType(Scenario scenario, String activityFacilityType) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(activityFacilityType) ;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : fac.getActivityOptions().values()) {
				if ( option.getType().equals(activityFacilityType) ) {
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
	public static List<String> collectAllFacilityTypes(Scenario scenario) {
		List<String> activityTypes = new ArrayList<String>() ;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : fac.getActivityOptions().values()) {
				// collect all activity types that are contained within the provided facilities file
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
			}
		}
		return activityTypes;
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
}