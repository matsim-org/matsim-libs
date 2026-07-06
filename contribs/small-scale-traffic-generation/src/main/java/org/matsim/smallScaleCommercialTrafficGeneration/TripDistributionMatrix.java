/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.TrafficVolumeKey;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.matsim.smallScaleCommercialTrafficGeneration.TrafficVolumeGeneration.makeTrafficVolumeKey;

/**
 * Builds and stores OD matrices for small scale commercial traffic.
 * <p>
 * The matrix values are calculated with a gravity model. Its resistance function can use network-based transport
 * costs, which are expensive because they trigger routing between representative zone links. Therefore, network-based
 * resistance values are precomputed once per zone pair and then reused by the gravity constants and OD matrix
 * calculation.
 *
 * @author Ricardo Ewert
 */
class TripDistributionMatrix {

	private static final Logger log = LogManager.getLogger(TripDistributionMatrix.class);
	private static final Joiner JOIN = Joiner.on("\t");
	private static final boolean USE_NETWORK_ROUTES_FOR_RESISTANCE_FUNCTION = true;

	private final ArrayList<String> listOfZones;
	private final ArrayList<String> listOfModesORvehTypes = new ArrayList<>();
	private final ArrayList<Integer> listOfPurposes = new ArrayList<>();
	private final Map<String, SimpleFeature> zoneFeatureMap;
	private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start;
	private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop;
	private final GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType;

	private record TripDistributionMatrixKey(String fromZone, String toZone, String modeORvehType, int purpose,
											 GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {}

	private record ResistanceFunktionKey(String fromZone, String toZone) {}

	private record GravityConstantKey (String fromZone, String modeORvehType, int purpose) {}

	public static class Builder {

		private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start;
		private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop;
		private final GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType;
		private final ArrayList<String> listOfZones;
		private final Map<String, SimpleFeature> zoneFeatureMap;

		static Builder newInstance(Index indexZones,
										  String shapeFileZoneNameColumn, Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
										  Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
										  GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, ArrayList<String> listOfZones) {
			return new Builder(indexZones, shapeFileZoneNameColumn, trafficVolume_start, trafficVolume_stop, smallScaleCommercialTrafficType, listOfZones);
		}

		private Builder(Index indexZones, String shapeFileZoneNameColumn,
						Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
						Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
						GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, ArrayList<String> listOfZones) {
			super();
			this.zoneFeatureMap = indexZones.getAllFeatures().stream()
				.collect(Collectors.toMap(
					f -> String.valueOf(f.getAttribute(shapeFileZoneNameColumn)),
					f -> f
				));
			this.trafficVolume_start = trafficVolume_start;
			this.trafficVolume_stop = trafficVolume_stop;
			this.smallScaleCommercialTrafficType = smallScaleCommercialTrafficType;
			this.listOfZones = new ArrayList<>(listOfZones);
		}

		TripDistributionMatrix build() {
			return new TripDistributionMatrix(this);
		}
	}

	private TripDistributionMatrix(Builder builder) {
		zoneFeatureMap = builder.zoneFeatureMap;
		trafficVolume_start = builder.trafficVolume_start;
		trafficVolume_stop = builder.trafficVolume_stop;
		smallScaleCommercialTrafficType = builder.smallScaleCommercialTrafficType;
		listOfZones = builder.listOfZones;
	}

	private final ConcurrentHashMap<TripDistributionMatrixKey, Integer> matrixCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<ResistanceFunktionKey, Double> resistanceFunktionCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<GravityConstantKey, Double> gravityConstantACache = new ConcurrentHashMap<>();
	//	private final ConcurrentHashMap<String, Double> gravityConstantBCache = new ConcurrentHashMap<String, Double>();
	private final ConcurrentHashMap<String, Object2DoubleMap<String>> roundingError = new ConcurrentHashMap<>();
	private NetworkBasedTransportCosts netBasedCosts = null;

	/**
	 * Precomputes network-based resistance values for all required unordered zone pairs.
	 * <p>
	 * Only pairs with positive start and stop volumes for at least one mode/purpose combination are routed. The current
	 * resistance function treats the relation symmetrically, so each calculated value is stored for both directions.
	 * This keeps the later gravity model loops from triggering hidden network routings while they iterate over modes,
	 * purposes, and zones.
	 *
	 * @param network          MATSim network used by {@link NetworkBasedTransportCosts}
	 * @param linksPerZone     available links per zone; the first link is used as the representative link as before
	 * @param resistanceFactor factor used in {@code exp(-resistanceFactor * travelCosts)}
	 */
	void precomputeResistanceFunctionValues(Network network, Map<String, Map<Id<Link>, Link>> linksPerZone, double resistanceFactor) {
		if (!USE_NETWORK_ROUTES_FOR_RESISTANCE_FUNCTION)
			return;

		initNetworkBasedCosts(network);
		Set<ResistanceFunktionKey> requiredResistanceKeys = collectRequiredResistanceKeys();
		Map<String, Location> representativeLocationsByZone = createRepresentativeLocationsByZone(linksPerZone, requiredResistanceKeys);

		int totalPairs = requiredResistanceKeys.size();
		Counter counter = new Counter("Precomputing resistance function values: ", " of " + totalPairs + " pairs");
		log.info("Precompute {} network-based resistance values for required zone pairs.", totalPairs);

		for (ResistanceFunktionKey resistanceKey : requiredResistanceKeys) {
			String fromZone = resistanceKey.fromZone();
			String toZone = resistanceKey.toZone();

			if (!resistanceFunktionCache.containsKey(resistanceKey)) {
				double resistanceValue;
				if (fromZone.equals(toZone)) {
					resistanceValue = 1.0;
				} else {
					resistanceValue = calculateNetworkResistanceValue(
						representativeLocationsByZone.get(fromZone),
						representativeLocationsByZone.get(toZone),
						resistanceFactor);
				}
				resistanceFunktionCache.put(resistanceKey, resistanceValue);
				resistanceFunktionCache.put(makeResistanceFunktionKey(toZone, fromZone), resistanceValue);
			}
			counter.incCounter();
		}
	}

	/**
	 * Calculates the traffic volume between two zones for a specific modeORvehType
	 * and purpose.
	 *
	 * @param startZone                       start zone
	 * @param stopZone                        stop zone
	 * @param modeORvehType                   selected mode or vehicle type
	 * @param purpose                         selected purpose
	 * @param smallScaleCommercialTrafficType goodsTraffic or commercialPersonTraffic
	 * @param linksPerZone                    links in each zone
	 */
	void setTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, Network network,
								  Map<String, Map<Id<Link>, Link>> linksPerZone, double resistanceFactor) {
		double volumeStart = trafficVolume_start.get( makeTrafficVolumeKey(startZone, modeORvehType ) ).getDouble(purpose );
		double volumeStop = trafficVolume_stop.get( makeTrafficVolumeKey(stopZone, modeORvehType ) ).getDouble(purpose );
		int roundedVolume;
		if (volumeStart != 0 && volumeStop != 0) {

			double resistanceValue = getResistanceFunktionValue(startZone, stopZone, network, linksPerZone, resistanceFactor);
			double gravityConstantA = getGravityConstant(stopZone, trafficVolume_start, modeORvehType, purpose, network, linksPerZone,
				resistanceFactor);
			roundingError.computeIfAbsent(stopZone, (k) -> new Object2DoubleOpenHashMap<>());

			//Bisher: Gravity model mit fixem Zielverkehr
			double volume = gravityConstantA * volumeStart * volumeStop * resistanceValue;
			roundedVolume = (int) Math.floor(volume);
			double certainRoundingError = (roundedVolume - volume) * -1;
			// roundingError based on stopZone, because gravity model is stopVolume fixed
			roundingError.get(stopZone).merge((modeORvehType + "_" + purpose), certainRoundingError, Double::sum);
			if (roundingError.get(stopZone).getDouble((modeORvehType + "_" + purpose)) >= 1) {
				roundedVolume++;
				roundingError.get(stopZone).merge((modeORvehType + "_" + purpose), -1, Double::sum);
			}
		} else
			roundedVolume = 0;
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
		if (matrixCache.containsKey(matrixKey)) {
			log.warn("Key {} already exists in the trip distribution matrix. Overwriting value.", matrixKey);
		}
		matrixCache.put(matrixKey, roundedVolume);
	}

	/**
	 * Gets value of the need traffic volume.
	 *
	 * @param startZone     start zone
	 * @param stopZone      stop zone
	 * @param modeORvehType selected mode or vehicle type
	 * @param purpose       selected purpose
	 * @param smallScaleCommercialTrafficType   goodsTraffic or commercialPersonTraffic
	 */
	Integer getTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
		return matrixCache.get(matrixKey);
	}

	/**
	 * Calculates the values of the resistance function between two zones.
	 *
	 * @param startZone               start zone
	 * @param stopZone                stop zone
	 * @param linksPerZone          links for each zone
	 */
	private Double getResistanceFunktionValue(String startZone, String stopZone, Network network, Map<String, Map<Id<Link>, Link>> linksPerZone,
											  double resistanceFactor) {

		//if false the calculation is faster; e.g. for debugging
		double resistanceFunktionResult;
		if (netBasedCosts == null && USE_NETWORK_ROUTES_FOR_RESISTANCE_FUNCTION)
			initNetworkBasedCosts(network);
		if (!resistanceFunktionCache.containsKey(makeResistanceFunktionKey(startZone, stopZone))) {
			SimpleFeature startZoneFeature = zoneFeatureMap.get(startZone);
			SimpleFeature stopZoneFeature = zoneFeatureMap.get(stopZone);
			double distance = Double.MAX_VALUE;
			double travelCosts = Double.MAX_VALUE;
			if (startZone.equals(stopZone)) {
				travelCosts = 0.0;
				distance = 0.0;
			} else {

				if (USE_NETWORK_ROUTES_FOR_RESISTANCE_FUNCTION) {
					//TODO: possible optimization: use a center link of the zone instead of a random link
					Location startLocation = Location.newInstance(linksPerZone.get(startZone).keySet().iterator().next().toString());
					Location stopLocation = Location.newInstance(linksPerZone.get(stopZone).keySet().iterator().next().toString());
//							distance = netBasedCosts.getDistance(startLocation, stopLocation, 21600., exampleVehicle);
					travelCosts = calculateNetworkTravelCosts(startLocation, stopLocation);

				} else {
					Point geometryStartZone = ((Geometry) startZoneFeature.getDefaultGeometry()).getCentroid();
					Point geometryStopZone = ((Geometry) stopZoneFeature.getDefaultGeometry()).getCentroid();

					distance = geometryStartZone.distance(geometryStopZone);
				}
			}
			if (USE_NETWORK_ROUTES_FOR_RESISTANCE_FUNCTION)
				resistanceFunktionResult = Math.exp(-resistanceFactor * travelCosts);
			else
				resistanceFunktionResult = Math.exp(-resistanceFactor * distance);
			resistanceFunktionCache.put(makeResistanceFunktionKey(startZone, stopZone), resistanceFunktionResult);
			resistanceFunktionCache.put(makeResistanceFunktionKey(stopZone, startZone), resistanceFunktionResult);
		}
		return resistanceFunktionCache.get(makeResistanceFunktionKey(startZone, stopZone));
	}

	/**
	 * Creates the shared network-based transport costs object for the resistance calculation.
	 * <p>
	 * This preserves the existing example vehicle type and travel-cost setup, but avoids recreating the transport costs
	 * from inside individual resistance lookups.
	 */
	private void initNetworkBasedCosts(Network network) {
		if (netBasedCosts != null)
			return;

		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("vwCaddy", VehicleType.class));
		vehicleType.getCostInformation().setCostsPerMeter(0.00017).setCostsPerSecond(0.0049).setFixedCost(22.73);
		NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, List.of(vehicleType));
		netBasedCosts = netBuilder.build();
	}

	private Set<ResistanceFunktionKey> collectRequiredResistanceKeys() {
		Set<ResistanceFunktionKey> requiredResistanceKeys = new HashSet<>();
		for (Map.Entry<TrafficVolumeKey, Object2DoubleMap<Integer>> stopEntry : trafficVolume_stop.entrySet()) {
			String stopZone = stopEntry.getKey().zone();
			String modeORvehType = stopEntry.getKey().modeORvehType();
			for (Integer purpose : stopEntry.getValue().keySet()) {
				if (stopEntry.getValue().getDouble(purpose) == 0)
					continue;
				for (Map.Entry<TrafficVolumeKey, Object2DoubleMap<Integer>> startEntry : trafficVolume_start.entrySet()) {
					if (startEntry.getKey().modeORvehType().equals(modeORvehType) && startEntry.getValue().getDouble(purpose) != 0)
						requiredResistanceKeys.add(makeSymmetricResistanceFunktionKey(stopZone, startEntry.getKey().zone()));
				}
			}
		}
		return requiredResistanceKeys;
	}

	private ResistanceFunktionKey makeSymmetricResistanceFunktionKey(String fromZone, String toZone) {
		if (fromZone.compareTo(toZone) <= 0)
			return makeResistanceFunktionKey(fromZone, toZone);
		return makeResistanceFunktionKey(toZone, fromZone);
	}

	/**
	 * Selects one representative routing location for every zone used by the precomputed resistance pairs.
	 * <p>
	 * The selection intentionally mirrors the previous behavior and uses the first available link in each zone. The
	 * method only makes that choice explicit and reusable during resistance precomputation.
	 */
	private Map<String, Location> createRepresentativeLocationsByZone(Map<String, Map<Id<Link>, Link>> linksPerZone, Set<ResistanceFunktionKey> requiredResistanceKeys) {
		Map<String, Location> representativeLocationsByZone = new HashMap<>();
		for (ResistanceFunktionKey resistanceKey : requiredResistanceKeys) {
			addRepresentativeLocation(representativeLocationsByZone, linksPerZone, resistanceKey.fromZone());
			addRepresentativeLocation(representativeLocationsByZone, linksPerZone, resistanceKey.toZone());
		}
		return representativeLocationsByZone;
	}

	private void addRepresentativeLocation(Map<String, Location> representativeLocationsByZone, Map<String, Map<Id<Link>, Link>> linksPerZone, String zone) {
		if (representativeLocationsByZone.containsKey(zone))
			return;
		Map<Id<Link>, Link> links = linksPerZone.get(zone);
		if (links == null || links.isEmpty())
			throw new IllegalStateException("No representative link available for zone " + zone + " although the zone has positive traffic volume.");
		representativeLocationsByZone.put(zone, Location.newInstance(links.keySet().iterator().next().toString()));
	}

	/**
	 * Calculates the gravity-model resistance value for one routed zone relation.
	 */
	private double calculateNetworkResistanceValue(Location startLocation, Location stopLocation, double resistanceFactor) {
		return Math.exp(-resistanceFactor * calculateNetworkTravelCosts(startLocation, stopLocation));
	}

	/**
	 * Routes between two representative zone links and returns the resulting transport costs.
	 */
	private double calculateNetworkTravelCosts(Location startLocation, Location stopLocation) {
		Vehicle exampleVehicle = getExampleVehicle(startLocation);
		return netBasedCosts.getTransportCost(startLocation, stopLocation, 21600., null, exampleVehicle);
	}

	/**
	 * Corrects missing trafficVolume in the OD because of roundingErrors based on trafficVolume_stop
	 */
	void clearRoundingError(Random rnd) {
		for (String stopZone : getListOfZones()) {
			for (String modeORvehType : getListOfModesOrVehTypes()) {
				loopForEachPurpose:
				for (Integer purpose : getListOfPurposes()) {
					double trafficVolume = trafficVolume_stop.get( makeTrafficVolumeKey(stopZone, modeORvehType ) ).getDouble(
						purpose );
					int generatedTrafficVolume = getSumOfServicesForStopZone(stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
					if (trafficVolume > generatedTrafficVolume) {
						ArrayList<String> shuffledZones = new ArrayList<>(getListOfZones());
						Collections.shuffle(shuffledZones, rnd);
						// find a startZone which has a trip to the stopZone and increase it by one
						for (String startZone : shuffledZones) {
							TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType,
								purpose, smallScaleCommercialTrafficType);
							if (matrixCache.get(matrixKey) > 0) {
								matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
								continue loopForEachPurpose;
							}
						}
						// if no possible startZone was found, add a inner trip in the stopZone
						TripDistributionMatrixKey matrixKey = makeKey(stopZone, stopZone, modeORvehType, purpose,
							smallScaleCommercialTrafficType);
						matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
					}
				}
			}
		}
	}


	/**
	 * Create example vehicle for netBasedCosts calculation.
	 *
	 * @param fromId from location
	 */
	private VehicleImpl getExampleVehicle(Location fromId) {
		return VehicleImpl.Builder.newInstance("vwCaddy").setType(
						com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl.Builder.newInstance("vwCaddy").build())
				.setStartLocation(fromId).build();
	}

	/**
	 * Calculates the gravity constant.
	 *
	 * @param baseZone                base zone
	 * @param trafficVolume           volume of the traffic
	 * @param modeORvehType           selected mode or vehicle type
	 * @param purpose                 selected purpose
	 * @param linksPerZone          links for each zone
	 * @return gravity constant
	 */
	private double getGravityConstant(String baseZone,
									  Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume, String modeORvehType,
									  Integer purpose, Network network, Map<String, Map<Id<Link>, Link>> linksPerZone, double resistanceFactor) {

		GravityConstantKey gravityKey = makeGravityKey(baseZone, modeORvehType, purpose);
		if (!gravityConstantACache.containsKey(gravityKey)) {
			double sum = 0;
			for (TrafficVolumeKey trafficVolumeKey : trafficVolume.keySet()) {
				if (trafficVolumeKey.modeORvehType().equals(modeORvehType)) {
					double volume = trafficVolume.get(trafficVolumeKey).getDouble(purpose);
					if (volume != 0) {
						double resistanceValue = getResistanceFunktionValue(baseZone, trafficVolumeKey.zone(), network,
							linksPerZone, resistanceFactor);
						sum = sum + (volume * resistanceValue);
					}
				}
			}
			double gravityConstant = 1 / sum;
			gravityConstantACache.put(gravityKey, gravityConstant);
		}
		return gravityConstantACache.get(gravityKey);

	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 *
	 * @param fromZone      from zone
	 * @param toZone        to zone
	 * @param modeORvehType selected mode or vehicle type
	 * @param purpose       selected purpose
	 * @param smallScaleCommercialTrafficType   goodsTraffic or commercialPersonTraffic
	 * @return TripDistributionMatrixKey
	 */
	private TripDistributionMatrixKey makeKey(String fromZone, String toZone, String modeORvehType, int purpose, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {
		return new TripDistributionMatrixKey(fromZone, toZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
	}

	/**
	 * Creates a key for the resistance function.
	 *
	 * @param fromZone from zone
	 * @param toZone   to zone
	 * @return ResistanceFunktionKey
	 */
	private ResistanceFunktionKey makeResistanceFunktionKey(String fromZone, String toZone) {
		return new ResistanceFunktionKey(fromZone, toZone);
	}

	/**
	 * Creates a key for a gravity constant.
	 *
	 * @param fromZone      from zone
	 * @param modeOrVehType selected mode or vehicle type
	 * @param purpose       selected purpose
	 * @return GravityConstantKey
	 */
	private GravityConstantKey makeGravityKey(String fromZone, String modeOrVehType, int purpose) {
		return new GravityConstantKey(fromZone, modeOrVehType, purpose);
	}

	/**
	 * Returns all zones being used as a start and/or stop location
	 *
	 * @return listOfZones
	 */
	ArrayList<String> getListOfZones() {
		return listOfZones;
	}

	/**
	 * Returns all modes being used.
	 *
	 * @return listOfModesORvehTypes
	 */
	ArrayList<String> getListOfModesOrVehTypes() {
		if (listOfModesORvehTypes.isEmpty()) {
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfModesORvehTypes.contains(key.modeORvehType()))
					listOfModesORvehTypes.add(key.modeORvehType());
				Collections.sort(listOfModesORvehTypes);
			}
		}
		return listOfModesORvehTypes;
	}

	/**
	 * Returns all purposes being used.
	 *
	 * @return listOfPurposes
	 */
	ArrayList<Integer> getListOfPurposes() {
		if (listOfPurposes.isEmpty()) {
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfPurposes.contains(key.purpose()))
					listOfPurposes.add(key.purpose());
				Collections.sort(listOfPurposes);
			}
		}
		return listOfPurposes;
	}

	/**
	 * @param startZone                       start Zone
	 * @param modeORvehType                   selected mode or vehicle type
	 * @param purpose                         selected purpose
	 * @param smallScaleCommercialTrafficType goodsTraffic or commercialPersonTraffic
	 * @param occupancyRate                   occupancy rates for the specific mode or vehicle type
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStartZone(String startZone, String modeORvehType, int purpose,
									 GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType,
									 double occupancyRate) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String stopZone : zones) {
			int trips = getTripDistributionValue(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
			numberOfTrips += (int) Math.ceil(trips / occupancyRate);
		}
		return numberOfTrips;
	}

	/**
	 * Gets the sum of services for a stop zone based on the trip distribution matrix.
	 *
	 * @param stopZone                        stop zone
	 * @param modeORvehType                   selected mode or vehicle type
	 * @param purpose                         selected purpose
	 * @param smallScaleCommercialTrafficType goodsTraffic or commercialPersonTraffic
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStopZone(String stopZone, String modeORvehType, int purpose, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String startZone : zones)
			numberOfTrips = numberOfTrips + matrixCache.get(makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType));
		return numberOfTrips;
	}

	/**
	 * Writes every matrix for each mode and purpose
	 */
	void writeODMatrices(Path output, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) throws UncheckedIOException, MalformedURLException {
		ArrayList<String> usedModesORvehTypes = getListOfModesOrVehTypes();
		ArrayList<String> usedZones = getListOfZones();
		Collections.sort(usedZones);
		ArrayList<Integer> usedPurposes = getListOfPurposes();

		for (String modeORvehType : usedModesORvehTypes) {
			for (int purpose : usedPurposes) {

				Path outputFolder = output.resolve("calculatedData")
						.resolve("odMatrix_" + smallScaleCommercialTrafficType + "_" + modeORvehType + "_purpose" + purpose + ".csv");

				BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.toUri().toURL(), StandardCharsets.UTF_8,
						true);
				try {

					List<String> headerRow = new ArrayList<>();
					headerRow.add("O/D");
					headerRow.addAll(usedZones);
					JOIN.appendTo(writer, headerRow);
					writer.write("\n");

					for (String startZone : usedZones) {
						List<String> row = new ArrayList<>();
						row.add(startZone);
						for (String stopZone : usedZones) {
							if (getTripDistributionValue(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType) == null)
								throw new RuntimeException("OD pair is missing; start: " + startZone + "; stop: "
										+ stopZone + "; modeORvehType: " + modeORvehType + "; purpose: " + purpose + "; smallScaleCommercialTrafficType: " + smallScaleCommercialTrafficType);
							row.add(String
									.valueOf(getTripDistributionValue(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType)));
						}
						JOIN.appendTo(writer, row);
						writer.write("\n");
					}
					writer.close();

				} catch (IOException e) {
					log.error("Problem to write OD matrix", e);
				}
				log.info("Write OD matrix for mode {} and for purpose {} to {}", modeORvehType, purpose, outputFolder);
			}
		}
	}
}
