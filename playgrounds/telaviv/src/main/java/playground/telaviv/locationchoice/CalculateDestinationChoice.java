/* *********************************************************************** *
 * project: org.matsim.*
 * CalculateDestinationChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.zones.CreateODDistanceMatrix;
import playground.telaviv.zones.CreateODTravelTimeMatrices;
import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.ZoneMapping;

public class CalculateDestinationChoice {

	private static final Logger log = Logger.getLogger(CalculateDestinationChoice.class);
	private static String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	
	private Scenario scenario;
	private static Coefficients coefficients = new Coefficients();
	private ZoneMapping zoneMapping;
	private Map<Integer, Emme2Zone> zones;
	private	Map<Integer, Integer> zoneToMatrixMapping;	// <TAZ, Index in the probability matrix>
	private Map<Integer, Integer> matrixToZoneMapping;	// <Index in the probability matrix, TAZ>
	private int[] zoneIndices;
	
	private int types = 4;
	private double[][] odDistances; 	// fromZone, toZone (NO TAZ! use tazMapping)
	private double[][][] odTravelTimes;	// fromZone, toZone, timeSlot
	private int numSlots;
	private int binSize;
	private double[][][][] vtod;	// type, origin, destination, time bin
	private double[][][][] ptod;	// type, origin, destination, time bin
	
	private double[][][] sumExp;	// type, toZone, timeSlot
	private double[][] indicators;	// type, zone
	private double[][] sizes;	// type, zone
	private double[][][] distances; // type, fromZone, toZone
	private double[][][][] modeChoices;	// type, fromZone, toZone, time bin
	
//	private Map<Integer, Double>[][][] fromZoneProbabilities;	//<ToZone, Probability>[type][fromZone][departureTime]
	
	public static void main(String[] args) {
		log.info("Reading Network");
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done");
		
		PersonalizableTravelTime travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		
		CalculateDestinationChoice cdc = new CalculateDestinationChoice(scenario);
		
		cdc.calculateConstantFactors();
		cdc.calculateDynamicFactors(travelTime);
		cdc.calculateTotalFactors();
		cdc.checkResults();
//		cdc.calculateFromZoneProbabilities();
	}
	
	public CalculateDestinationChoice(Scenario scenario) {
		this.scenario = scenario;
		
		log.info("Creating Zones Mapping");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		zones = zoneMapping.getParsedZones();
		log.info("done");
	}
	
	public void calculateConstantFactors() {
		log.info("Creating OD Distance Matrix");
		CreateODDistanceMatrix creator = new CreateODDistanceMatrix(scenario, zoneMapping);
		creator.calculateODMatrix();
		zoneToMatrixMapping = creator.getTAZMapping();
		matrixToZoneMapping = creator.getInvertedTAZMapping();
		odDistances = creator.getODDistanceMatrix();
		log.info("done");
		
		/*
		 * Initialize Arrays
		 */
		indicators = new double[types][zones.size()];	// type, zone
		sizes = new double[types][zones.size()];	// type, zone
		distances = new double[types][zones.size()][zones.size()]; // type, fromZone, toZone
		zoneIndices = new int[zones.size()];	// zone
		log.info("done");
				
		/*
		 * Fill Arrays
		 */
		log.info("Fill Helper Arrays");
		for (int type = 0; type < types; type++) {
			for (Emme2Zone fromZone : zones.values()) {
				int fromZoneIndex = zoneToMatrixMapping.get(fromZone.TAZ);
				indicators[type][fromZoneIndex] = calculateIndicators(type, fromZone);
				sizes[type][fromZoneIndex] = calculateSize(type, fromZone);
				zoneIndices[fromZoneIndex] = matrixToZoneMapping.get(fromZoneIndex);
				
				for (Emme2Zone toZone : zones.values()) {
					int toZoneIndex = zoneToMatrixMapping.get(toZone.TAZ);
					distances[type][fromZoneIndex][toZoneIndex] = calculateDistance(type, fromZone, toZone);
				}
			}
		}
		log.info("done");
	}
	
	/*
	 * Time depending Travel Times
	 */
	public void calculateDynamicFactors(PersonalizableTravelTime travelTime) {

		log.info("Creating OD Travel Time Matrix");
		CreateODTravelTimeMatrices creator = new CreateODTravelTimeMatrices(scenario, zoneMapping, travelTime);
		creator.calculateODMatrices();		
		odTravelTimes = creator.getODTravelTimeMatrices();
		numSlots = creator.getNumSlots();
		binSize = creator.getBinSize();
		log.info("done");
		
		modeChoices = new double[types][zones.size()][zones.size()][numSlots]; // type, fromZone, toZone, time bin
		
		log.info("Fill Mode Choice Array");
		for (int type = 0; type < types; type++) {
			for (int fromZoneIndex = 0; fromZoneIndex < zones.size(); fromZoneIndex++) {			
				for (int toZoneIndex = 0; toZoneIndex < zones.size(); toZoneIndex++) {					
					double[] odTravelTime = odTravelTimes[fromZoneIndex][toZoneIndex];
					double[] modeChoice = modeChoices[type][fromZoneIndex][toZoneIndex];
					for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
						modeChoice[timeSlot] = calculateModeChoice(type, odTravelTime[timeSlot]);						
					}
				}
			}
		}
		log.info("done");
	}
	
	public void calculateTotalFactors() {
		
		sumExp = new double[types][zones.size()][numSlots];	// type, toZone
		vtod = new double[types][zones.size()][zones.size()][numSlots]; // type, fromZone, toZone, time bin
		ptod = new double[types][zones.size()][zones.size()][numSlots]; // type, fromZone, toZone, time bin
		
		/*
		 * Fill vtod Array
		 */
		log.info("Fill VTOD Array");
		for (int type = 0; type < types; type++) {
			for (int fromZoneIndex = 0; fromZoneIndex < zones.size(); fromZoneIndex++) {			
				double sum = 0.0;
				for (int toZoneIndex = 0; toZoneIndex < zones.size(); toZoneIndex++) {
					
					double constFactor = indicators[type][toZoneIndex] + 
					sizes[type][toZoneIndex] + 
					distances[type][fromZoneIndex][toZoneIndex];
					
					double[] modeChoice = modeChoices[type][fromZoneIndex][toZoneIndex];
					for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
						double value = constFactor + modeChoice[timeSlot];
						vtod[type][fromZoneIndex][toZoneIndex][timeSlot] = value;		
						sum = sum + Math.exp(value);
					}
					
//					log.info("indicator " + indicators[type][toZoneIndex] + ", size " + sizes[type][toZoneIndex] + ", distance " + distances[type][fromZoneIndex][toZoneIndex]);

				}
//				log.info("sumExp " + sum);
//				sumExp[type][fromZoneIndex][numSlot] = sum;
			}
		}
		modeChoices = null;
		log.info("done");
		
		log.info("Calculate sums");
		for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
			for (int type = 0; type < types; type++) {
				for (int fromZoneIndex = 0; fromZoneIndex < zones.size(); fromZoneIndex++) {			
					double sum = 0.0;
					for (int toZoneIndex = 0; toZoneIndex < zones.size(); toZoneIndex++) {
						double value = vtod[type][fromZoneIndex][toZoneIndex][timeSlot];
						sum = sum + Math.exp(value);
					}
					sumExp[type][fromZoneIndex][timeSlot] = sum;
				}
			}
		}
		log.info("done");
		
		/*
		 * Fill ptod Array
		 */
		log.info("Fill PTOD Array");
		for (int type = 0; type < types; type++) {
			for (int fromZoneIndex = 0; fromZoneIndex < zones.size(); fromZoneIndex++) {			
				for (int toZoneIndex = 0; toZoneIndex < zones.size(); toZoneIndex++) {
					for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
						double value = Math.exp(vtod[type][fromZoneIndex][toZoneIndex][timeSlot]);
						ptod[type][fromZoneIndex][toZoneIndex][timeSlot] = value / sumExp[type][fromZoneIndex][timeSlot];
//					log.info("sumExp " + sumExp[type][fromZoneIndex] + ", value " + value);						
					}
				}
			}
		}
		vtod = null;
		log.info("done");
	}
	
	public void checkResults() {
		/*
		 * Check results
		 */
		double sumAll = 0.0;
		log.info("Check Results");
		for (int type = 0; type < types; type++) {
			double sum = 0.0;
			for (int fromZoneIndex = 0; fromZoneIndex < zones.size(); fromZoneIndex++) {			
				for (int toZoneIndex = 0; toZoneIndex < zones.size(); toZoneIndex++) {
					for (int timeSlot = 0; timeSlot < numSlots; timeSlot++) {
						sum = sum + ptod[type][fromZoneIndex][toZoneIndex][timeSlot];
					}
				}
			}
			log.info("sum type " + type + " " + sum);
			sumAll = sumAll + sum;
		}
		log.info("sumAll " + sumAll);
		log.info("done");
		
		log.info("9107 to 9107: " + ptod[0][zoneToMatrixMapping.get(9107)][zoneToMatrixMapping.get(9107)][0]);
		log.info("9503 to 9107: " + ptod[0][zoneToMatrixMapping.get(9503)][zoneToMatrixMapping.get(9107)][0]);
		log.info("7403 to 7403: " + ptod[0][zoneToMatrixMapping.get(7403)][zoneToMatrixMapping.get(7403)][0]);
		log.info("9100 to 9107: " + ptod[0][zoneToMatrixMapping.get(9100)][zoneToMatrixMapping.get(9107)][0]);
		log.info("9504 to 9107: " + ptod[0][zoneToMatrixMapping.get(9504)][zoneToMatrixMapping.get(9107)][0]);
		log.info("9101 to 9101: " + ptod[0][zoneToMatrixMapping.get(9101)][zoneToMatrixMapping.get(9101)][0]);
		log.info("6703 to 6703: " + ptod[0][zoneToMatrixMapping.get(6703)][zoneToMatrixMapping.get(6703)][0]);
		log.info("7407 to 7403: " + ptod[0][zoneToMatrixMapping.get(7407)][zoneToMatrixMapping.get(7403)][0]);
		log.info("7605 to 7403: " + ptod[0][zoneToMatrixMapping.get(7605)][zoneToMatrixMapping.get(7403)][0]);
//		9107,9107,0.938462
//		9503,9107,0.734435
//		7403,7403,0.644480
//		9100,9107,0.633425
//		9504,9107,0.531351
//		9101,9101,0.528275
//		6703,6703,0.497944
//		7407,7403,0.480291
//		7605,7403,0.480184
	}
	
	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.binSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}
	
	// Tuple<TAZ, Probability>
	public Tuple<int[], double[]> getFromZoneProbabilities(int type, int fromZoneTAZ, double depatureTime) {
		int index = zoneToMatrixMapping.get(fromZoneTAZ);
		int timeSlot = getTimeSlotIndex(depatureTime);
		double[] probabilities = new double[zones.size()];
		double[][] array = ptod[type][index];

		/*
		 * Switch destination and time bin in ptod array to avoid this loop?
		 */
//		private double[][][][] ptod;	// type, origin, destination, time bin
		for (int i = 0; i < zones.size(); i++) {
			probabilities[i] = array[i][timeSlot];
		}
		
		return new Tuple<int[], double[]>(zoneIndices, probabilities);
	}
	
	/*
	 * Zone Indicators
	 */
	private double calculateIndicators(int type, Emme2Zone zone) {
		
		double metroCult 	= coefficients.metroCult[type];		
		double locCultur 	= coefficients.locCultur[type];
		double university 	= coefficients.university[type];
		double highschool 	= coefficients.highschool[type];
		double school 		= coefficients.school[type];
		double majorOffice	= coefficients.majorOffice[type];
		double otherOffice 	= coefficients.otherOffice[type];
		double mall 		= coefficients.mall[type];
		double shopstreet 	= coefficients.shopstreet[type];	
		double market 		= coefficients.market[type];
		double health 		= coefficients.health[type];
		double urban 		= coefficients.urban[type];
		double jew 			= coefficients.jew[type];
		double islam 		= coefficients.islam[type];
		
		double[] betaIndicator = new double[]{metroCult, locCultur, university, highschool, school,
				majorOffice, otherOffice, mall, shopstreet, market, health, urban, jew, islam};
		double[] xIndicator = new double[betaIndicator.length];
		for (int i = 0; i < xIndicator.length; i++) xIndicator[i] = 0.0;
		
		/*
		 * Cultural Areas: 
		 * 0-none
		 * 1-Metropolitan Cultural area
		 * 2-local Cultural area
		 */
		if (zone.CULTURAL == 1) xIndicator[0] = 1.0;
		else if (zone.CULTURAL == 2) xIndicator[1] = 1.0;
		
		/*
		 * Education:
		 * 0-none
		 * 1-higher education: College, university
		 * 2-high school
		 * 3-elementary schools
		 */
		if (zone.EDUCATION == 1) xIndicator[2] = 1.0;
		else if(zone.EDUCATION == 2) xIndicator[3] = 1.0;
		else if(zone.EDUCATION == 3) xIndicator[4] = 1.0;

		/*
		 * Office:
		 * 0-none
		 * 1-Major office concentrations
		 * 2-Other zones with offices use (City center, commercial/industrial)
		 */
		if (zone.OFFICE == 1) xIndicator[5] = 1.0;
		else if (zone.OFFICE == 2) xIndicator[6] = 1.0;
		
		/*
		 * Shopping:
		 * 0-none
		 * 1-Shopping mall
		 * 2-shopping streets
		 * 3-Market
		 */
		if (zone.SHOPPING == 1) xIndicator[7] = 1.0;
		else if (zone.SHOPPING == 2) xIndicator[8] = 1.0;
		else if (zone.SHOPPING == 3) xIndicator[9] = 1.0;
		
		/*
		 * Health Institutions:
		 * 0-none
		 * 1-hospitals
		 */
		if (zone.HEALTH == 1) xIndicator[10] = 1.0;
		
		/*
		 * Urban Cores:
		 * 0-none
		 * 1-Cities over 20,000 inhabits
		 */
		if (zone.URBAN == 1) xIndicator[11] = 1.0;

		/*
		 * Religious Character:
		 * 0-none
		 * 1-Religious Jewish areas
		 * 2-Religious Islamic areas 
		 */
		if (zone.RELIGIOSIT == 1) xIndicator[12] = 1.0;
		else if (zone.RELIGIOSIT == 2) xIndicator[13] = 1.0;
		
		/*
		 * Calculation
		 */
		double sum = 0.0;
		for (int i = 0; i < betaIndicator.length; i++) sum = sum + betaIndicator[i] * xIndicator[i];
		
		return sum;
	}
	
	/*
	 * Size Variables
	 */
	private double calculateSize(int type, Emme2Zone zone) {			
		
		double areaSizeFactor 		= coefficients.areaSizeFactor[type];
		double serviceEmployment	= coefficients.serviceEmployment[type];
		double totalEmployment 		= coefficients.totalEmployment[type];
		double students 			= coefficients.students[type];
		double households			= coefficients.households[type];
		
		double[] betaSize = new double[]{areaSizeFactor, serviceEmployment, totalEmployment, students, households};
		double[] xSize = new double[betaSize.length];
		
		xSize[0] = zone.AREA;
		xSize[1] = zone.EMPL_SERV;
		xSize[2] = zone.EMPL_TOT;
		xSize[3] = zone.STUDENTS;
		xSize[4] = zone.HOUSEHOLDS;

		for (int i = 0; i < xSize.length; i++) if (xSize[i] == 0.0) xSize[i] = 1.0;
		
		/*
		 * Calculation
		 */
		double sum = 0.0;
		
		if (type == 0 || type == 1) {
			for (int i = 0; i < betaSize.length; i++) sum = sum + Math.exp(betaSize[i]) * xSize[i];
		}
		else if (type == 2 || type == 3) {
			for (int i = 0; i < betaSize.length; i++) sum = sum + betaSize[i] * Math.log(xSize[i]);			
		}
		
//		for (int i = 0; i < betaSize.length; i++) sum = sum + Math.log1p(betaSize[i] * xSize[i]);
		sum = Math.log(sum);
		return sum;
	}
	
	/*
	 * Distance Variables
	 */
	private double calculateDistance(int type, Emme2Zone fromZone, Emme2Zone toZone) {
		double autodist		= coefficients.autodist[type]; 
		double autodist2	= coefficients.autodist2[type];
		double autodist3	= coefficients.autodist3[type];
		
		double[] betaDistance = new double[]{autodist, autodist2, autodist3};
		
		double dum10_10	= coefficients.dum10_10[type];
		double dum10_20	= coefficients.dum10_20[type];
		double dum10_3040	= coefficients.dum10_3040[type];
		double dum20_10	= coefficients.dum20_10[type];
		double dum3040_10	= coefficients.dum3040_10[type];
		double dum3040_20	= coefficients.dum3040_20[type];
		
		double[] betaDummy = new double[]{dum10_10, dum10_20, dum10_3040, dum20_10, dum3040_10, dum3040_20}; 
		double[] xDummy = new double[betaDummy.length];
		for (int i = 0; i < xDummy.length; i++) xDummy[i] = 0.0;
		
		int fromSuperZone = fromZone.SUPERZONE;
		int toSuperZone = toZone.SUPERZONE;
		
		/*
		 * Super Zones
		 */
		if (fromSuperZone == 10 && toSuperZone == 10) dum10_10 = 1.0;
		else if (fromSuperZone == 10 && toSuperZone == 20) dum10_20 = 1.0;
		else if (fromSuperZone == 10 && toSuperZone == 30) dum10_3040 = 1.0;
		else if (fromSuperZone == 10 && toSuperZone == 40) dum10_3040 = 1.0;
		else if (fromSuperZone == 20 && toSuperZone == 10) dum20_10 = 1.0;
		else if (fromSuperZone == 30 && toSuperZone == 10) dum3040_10 = 1.0;
		else if (fromSuperZone == 40 && toSuperZone == 10) dum3040_10 = 1.0;
		else if (fromSuperZone == 30 && toSuperZone == 20) dum3040_20 = 1.0;
		else if (fromSuperZone == 40 && toSuperZone == 20) dum3040_20 = 1.0;
		
		int fromIndex = zoneToMatrixMapping.get(fromZone.TAZ);
		int toIndex = zoneToMatrixMapping.get(toZone.TAZ);
		double distance = odDistances[fromIndex][toIndex] / 1000;
				
		/*
		 * Calculation
		 */
		double sum = 0.0;
		for (int pow = 0; pow < betaDistance.length; pow++) sum = sum + betaDistance[pow]*Math.pow(distance, (pow + 1));
		
		for (int i = 0; i < betaDummy.length; i++) sum = sum + betaDummy[i] * xDummy[i];
		
		return sum;
	}
	
	/*
	 * Mode Choice
	 */
	private double calculateModeChoice(int type, double travelTime) {
		double logSumModeChoice 	= coefficients.logSumModeChoice[type];
		double betaDriverTime 		= coefficients.betaDriverTime[type]; 
		
//		return logSumModeChoice * Math.log(Math.exp(betaDriverTime) * travelTime);
		return logSumModeChoice * betaDriverTime * travelTime;
	}
}