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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ricardo Ewert
 */
public class TripDistributionMatrix {

	private static final Logger log = LogManager.getLogger(TripDistributionMatrix.class);
	private static final Joiner JOIN = Joiner.on("\t");

	private final ArrayList<String> listOfZones;
	private final ArrayList<String> listOfModesORvehTypes = new ArrayList<>();
	private final ArrayList<Integer> listOfPurposes = new ArrayList<>();
	private final List<SimpleFeature> zonesFeatures;
	private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start;
	private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop;
	private final String smallScaleCommercialTrafficType;

	private static class TripDistributionMatrixKey {
		private final String fromZone;
		private final String toZone;
		private final String modeORvehType;
		private final int purpose;
		private final String smallScaleCommercialTrafficType;

		public TripDistributionMatrixKey(String fromZone, String toZone, String modeORvehType, int purpose, String smallScaleCommercialTrafficType) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;
			this.modeORvehType = modeORvehType;
			this.purpose = purpose;
			this.smallScaleCommercialTrafficType = smallScaleCommercialTrafficType;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		public String getModesORvehType() {
			return modeORvehType;
		}

		public int getPurpose() {
			return purpose;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			long temp;
			temp = Double.doubleToLongBits(purpose);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			result = prime * result + ((modeORvehType == null) ? 0 : modeORvehType.hashCode());
			result = prime * result + ((smallScaleCommercialTrafficType == null) ? 0 : smallScaleCommercialTrafficType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TripDistributionMatrixKey other = (TripDistributionMatrixKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (Double.doubleToLongBits(purpose) != Double.doubleToLongBits(other.purpose))
				return false;
			if (toZone == null) {
				if (other.toZone != null)
					return false;
			} else if (!toZone.equals(other.toZone))
				return false;
			if (modeORvehType == null) {
				if (other.modeORvehType != null)
					return false;
			} else if (!modeORvehType.equals(other.modeORvehType))
				return false;
			if (smallScaleCommercialTrafficType == null) {
				return other.smallScaleCommercialTrafficType == null;
			} else return smallScaleCommercialTrafficType.equals(other.smallScaleCommercialTrafficType);
		}
	}

	private static class ResistanceFunktionKey {
		private final String fromZone;
		private final String toZone;

		public ResistanceFunktionKey(String fromZone, String toZone) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResistanceFunktionKey other = (ResistanceFunktionKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (toZone == null) {
				return other.toZone == null;
			} else return toZone.equals(other.toZone);
		}
	}

	private static class GravityConstantKey {
		private final String fromZone;
		private final String modeORvehType;
		private final int purpose;

		public GravityConstantKey(String fromZone, String mode, int purpose) {
			super();
			this.fromZone = fromZone;
			this.modeORvehType = mode;
			this.purpose = purpose;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			long temp;
			temp = Double.doubleToLongBits(purpose);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((modeORvehType == null) ? 0 : modeORvehType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GravityConstantKey other = (GravityConstantKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (Double.doubleToLongBits(purpose) != Double.doubleToLongBits(other.purpose))
				return false;
			if (modeORvehType == null) {
				return other.modeORvehType == null;
			} else return modeORvehType.equals(other.modeORvehType);
		}
	}

	public static class Builder {

		private final List<SimpleFeature> zonesFeatures;
		private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start;
		private final Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop;
		private final String smallScaleCommercialTrafficType;
		private final ArrayList<String> listOfZones;

		public static Builder newInstance(Index indexZones,
										  Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
										  Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
										  String smallScaleCommercialTrafficType, ArrayList<String> listOfZones) {
			return new Builder(indexZones, trafficVolume_start, trafficVolume_stop, smallScaleCommercialTrafficType, listOfZones);
		}

		private Builder(Index indexZones,
						Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
						Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
						String smallScaleCommercialTrafficType, ArrayList<String> listOfZones) {
			super();
			this.zonesFeatures = indexZones.getAllFeatures();
			this.trafficVolume_start = trafficVolume_start;
			this.trafficVolume_stop = trafficVolume_stop;
			this.smallScaleCommercialTrafficType = smallScaleCommercialTrafficType;
			this.listOfZones = new ArrayList<>(listOfZones);
		}

		public TripDistributionMatrix build() {
			return new TripDistributionMatrix(this);
		}
	}

	private TripDistributionMatrix(Builder builder) {
		zonesFeatures = builder.zonesFeatures;
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
	 * Calculates the traffic volume between two zones for a specific modeORvehType
	 * and purpose.
	 *
	 * @param startZone                       start zone
	 * @param stopZone                        stop zone
	 * @param modeORvehType                   selected mode or vehicle type
	 * @param purpose                         selected purpose
	 * @param smallScaleCommercialTrafficType goodsTraffic or commercialPersonTraffic
	 * @param linksPerZone                    links in each zone
	 * @param shapeFileZoneNameColumn         Name of the unique column of the name/Id of each zone in the zones shape file
	 */
	void setTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, String smallScaleCommercialTrafficType, Network network,
								  Map<String, Map<Id<Link>, Link>> linksPerZone, double resistanceFactor, String shapeFileZoneNameColumn) {
		double volumeStart = trafficVolume_start.get(TrafficVolumeGeneration.makeTrafficVolumeKey(startZone, modeORvehType)).getDouble(purpose);
		double volumeStop = trafficVolume_stop.get(TrafficVolumeGeneration.makeTrafficVolumeKey(stopZone, modeORvehType)).getDouble(purpose);
		int roundedVolume;
		if (volumeStart != 0 && volumeStop != 0) {

			double resistanceValue = getResistanceFunktionValue(startZone, stopZone, network, linksPerZone, resistanceFactor, shapeFileZoneNameColumn);
			double gravityConstantA = getGravityConstant(stopZone, trafficVolume_start, modeORvehType, purpose, network, linksPerZone,
				resistanceFactor, shapeFileZoneNameColumn);
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
	Integer getTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, String smallScaleCommercialTrafficType) {
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
		return matrixCache.get(matrixKey);
	}

	/**
	 * Calculates the values of the resistance function between two zones.
	 *
	 * @param startZone               start zone
	 * @param stopZone                stop zone
	 * @param linksPerZone          links for each zone
	 * @param shapeFileZoneNameColumn Name of the unique column of the name/Id of each zone in the zones shape file
	 */
	private Double getResistanceFunktionValue(String startZone, String stopZone, Network network, Map<String, Map<Id<Link>, Link>> linksPerZone,
											  double resistanceFactor, String shapeFileZoneNameColumn) {

		//if false the calculation is faster; e.g. for debugging
		boolean useNetworkRoutesForResistanceFunction = true;
		double resistanceFunktionResult;
		if (netBasedCosts == null) {
			VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("vwCaddy", VehicleType.class));
			vehicleType.getCostInformation().setCostsPerMeter(0.00017).setCostsPerSecond(0.00948).setFixedCost(22.73);
			NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, List.of(vehicleType));
			netBasedCosts = netBuilder.build();
		}
		if (!resistanceFunktionCache.containsKey(makeResistanceFunktionKey(startZone, stopZone)))
			for (SimpleFeature startZoneFeature : zonesFeatures) {
				String zone1 = String.valueOf(startZoneFeature.getAttribute(shapeFileZoneNameColumn));
				if (!startZone.equals(zone1))
					continue;
				for (SimpleFeature stopZoneFeature : zonesFeatures) {
					String zone2 = String.valueOf(stopZoneFeature.getAttribute(shapeFileZoneNameColumn));
					if (!stopZone.equals(zone2))
						continue;
					double distance = Double.MAX_VALUE;
					double travelCosts = Double.MAX_VALUE;
					if (zone1.equals(zone2)) {
						travelCosts = 0;
						distance = 0;
					} else {

						if (useNetworkRoutesForResistanceFunction) {
							Location startLocation = Location.newInstance(linksPerZone.get(startZone).keySet().iterator().next().toString());
							Location stopLocation = Location.newInstance(linksPerZone.get(stopZone).keySet().iterator().next().toString());
							Vehicle exampleVehicle = getExampleVehicle(startLocation);
//							distance = netBasedCosts.getDistance(startLocation, stopLocation, 21600., exampleVehicle);
							travelCosts = netBasedCosts.getTransportCost(startLocation, stopLocation, 21600., null, exampleVehicle);
						}
						else {
							Point geometryStartZone = ((Geometry) startZoneFeature.getDefaultGeometry()).getCentroid();
							Point geometryStopZone = ((Geometry) stopZoneFeature.getDefaultGeometry()).getCentroid();

							distance = geometryStartZone.distance(geometryStopZone);

						}
					}
					if (useNetworkRoutesForResistanceFunction)
						resistanceFunktionResult = Math.exp(-resistanceFactor*travelCosts);
					else
						resistanceFunktionResult = Math.exp(-distance);
					resistanceFunktionCache.put(makeResistanceFunktionKey(zone1, zone2), resistanceFunktionResult);
					resistanceFunktionCache.put(makeResistanceFunktionKey(zone2, zone1), resistanceFunktionResult);
				}
			}
		return resistanceFunktionCache.get(makeResistanceFunktionKey(startZone, stopZone));
	}

	/**
	 * Corrects missing trafficVolume in the OD because of roundingErrors based on trafficVolume_stop
	 */
	void clearRoundingError() {

		for (String stopZone : getListOfZones()) {
			for (String modeORvehType : getListOfModesOrVehTypes()) {
				for (Integer purpose : getListOfPurposes()) {
					double trafficVolume = trafficVolume_stop.get(TrafficVolumeGeneration.makeTrafficVolumeKey(stopZone, modeORvehType)).getDouble(purpose);
					int generatedTrafficVolume = getSumOfServicesForStopZone(stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType);
					if (trafficVolume > generatedTrafficVolume) {
						if (generatedTrafficVolume == 0) {
							TripDistributionMatrixKey matrixKey = makeKey(stopZone, stopZone, modeORvehType, purpose,
								smallScaleCommercialTrafficType);
							matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
							generatedTrafficVolume = getSumOfServicesForStopZone(stopZone, modeORvehType, purpose,
								smallScaleCommercialTrafficType);
						} else {
							ArrayList<String> shuffledZones = new ArrayList<>(getListOfZones());
							Collections.shuffle(shuffledZones, MatsimRandom.getRandom());
							for (String startZone : shuffledZones) {
								TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType,
										purpose, smallScaleCommercialTrafficType);
								if (matrixCache.get(matrixKey) > 0) {
									matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
									break;
								}
							}
						}
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
	 * @param shapeFileZoneNameColumn Name of the unique column of the name/Id of each zone in the zones shape file
	 * @return gravity constant
	 */
	private double getGravityConstant(String baseZone,
									  Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume, String modeORvehType,
									  Integer purpose, Network network, Map<String, Map<Id<Link>, Link>> linksPerZone, double resistanceFactor, String shapeFileZoneNameColumn) {

		GravityConstantKey gravityKey = makeGravityKey(baseZone, modeORvehType, purpose);
		if (!gravityConstantACache.containsKey(gravityKey)) {
			double sum = 0;
			for (TrafficVolumeKey trafficVolumeKey : trafficVolume.keySet()) {
				if (trafficVolumeKey.getModeORvehType().equals(modeORvehType)) {
					double volume = trafficVolume.get(trafficVolumeKey).getDouble(purpose);
					if (volume == 0)
						continue;
					else {
						double resistanceValue = getResistanceFunktionValue(baseZone, trafficVolumeKey.getZone(), network,
							linksPerZone, resistanceFactor, shapeFileZoneNameColumn);
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
	private TripDistributionMatrixKey makeKey(String fromZone, String toZone, String modeORvehType, int purpose, String smallScaleCommercialTrafficType) {
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
				if (!listOfModesORvehTypes.contains(key.getModesORvehType()))
					listOfModesORvehTypes.add(key.getModesORvehType());
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
				if (!listOfPurposes.contains(key.getPurpose()))
					listOfPurposes.add(key.getPurpose());
			}
		}
		return listOfPurposes;
	}

	/**
	 * @param startZone     start Zone
	 * @param modeORvehType selected mode or vehicle type
	 * @param purpose       selected purpose
	 * @param smallScaleCommercialTrafficType   goodsTraffic or commercialPersonTraffic
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStartZone(String startZone, String modeORvehType, int purpose, String smallScaleCommercialTrafficType) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String stopZone : zones)
			numberOfTrips = numberOfTrips + Math.round(matrixCache.get(makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType)));
		return numberOfTrips;
	}

	/**
	 * @param stopZone      stop zone
	 * @param modeORvehType selected mode or vehicle type
	 * @param purpose       selected purpose
	 * @param smallScaleCommercialTrafficType   goodsTraffic or commercialPersonTraffic
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStopZone(String stopZone, String modeORvehType, int purpose, String smallScaleCommercialTrafficType) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String startZone : zones)
			numberOfTrips = numberOfTrips + Math.round(matrixCache.get(makeKey(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType)));
		return numberOfTrips;
	}

	/**
	 * Writes every matrix for each mode and purpose
	 */
	void writeODMatrices(Path output, String smallScaleCommercialTrafficType) throws UncheckedIOException, MalformedURLException {
		ArrayList<String> usedModesORvehTypes = getListOfModesOrVehTypes();
		ArrayList<String> usedZones = getListOfZones();
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
