/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaVehicleCategory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions;

import java.util.*;

/**
 * @author benjamin
 *
 */
public enum HbefaVehicleCategory {
		PASSENGER_CAR ("pass. car"),
		LIGHT_COMMERCIAL_VEHICLE ("LCV"),
		HEAVY_GOODS_VEHICLE ("HGV"),
		URBAN_BUS ("urban bus"),
		COACH("coach"),
		MOTORCYCLE ("motorcycle"),
		NON_HBEFA_VEHICLE("NON");

		private String identifier;
		HbefaVehicleCategory(final String identifier){
				this.identifier = identifier;
		}

		public String identifier() {
				return identifier;
		}


		public List<HbefaWarmEmissionFactorKey> getWarmEmissionEntries(Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable    ) {
				Set<String> roadCategories = new HashSet<>();
				Set<HbefaTrafficSituation> trafficSituations = EnumSet.noneOf(HbefaTrafficSituation.class);
				Set<HbefaVehicleAttributes> vehicleAttributes = new HashSet<>();
				Set<Pollutant> pollutantsInTable = EnumSet.noneOf(Pollutant.class);
				for (HbefaWarmEmissionFactorKey emissionFactorKey : detailedHbefaWarmTable.keySet()) {
						roadCategories.add(emissionFactorKey.getRoadCategory());
						trafficSituations.add(emissionFactorKey.getTrafficSituation());
						vehicleAttributes.add(emissionFactorKey.getVehicleAttributes());
						pollutantsInTable.add(emissionFactorKey.getComponent());
				}
				List<HbefaWarmEmissionFactorKey> key = new ArrayList<>();
				for (HbefaTrafficSituation trafficSituation : trafficSituations) {
						for (String roadCategory : roadCategories) {
								for (HbefaVehicleAttributes vehicleAttribute : vehicleAttributes) {
										if (vehicleAttribute.toString().contains(HbefaVehicleCategory.this.identifier())) {
												for (Pollutant pollutant : pollutantsInTable) {
														HbefaWarmEmissionFactorKey keyelement = new HbefaWarmEmissionFactorKey();
														keyelement.setRoadCategory(roadCategory);
														keyelement.setVehicleAttributes(vehicleAttribute);
														keyelement.setVehicleCategory(this);
														keyelement.setTrafficSituation(trafficSituation);
														keyelement.setComponent(pollutant);
														key.add(keyelement);

												}
										}
								}
						}
				}
				return key;
		}

		public List<HbefaColdEmissionFactorKey> getColdEmissionEntries( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable    ) {
				Set<HbefaVehicleAttributes> vehicleAttributes = new HashSet<>();
				Set<Pollutant> pollutantsInTable = EnumSet.noneOf(Pollutant.class);
				for (HbefaColdEmissionFactorKey emissionFactorKey : detailedHbefaColdTable.keySet()) {
						vehicleAttributes.add(emissionFactorKey.getVehicleAttributes());
						pollutantsInTable.add(emissionFactorKey.getComponent());
				}
				List<HbefaColdEmissionFactorKey> key = new ArrayList<>();
				for (HbefaVehicleAttributes vehicleAttribute : vehicleAttributes) {
						if (vehicleAttribute.toString().contains(HbefaVehicleCategory.this.identifier())) {
								for (Pollutant pollutant : pollutantsInTable) {
										HbefaColdEmissionFactorKey keyelement = new HbefaColdEmissionFactorKey();
										keyelement.setVehicleAttributes(vehicleAttribute);
										keyelement.setVehicleCategory(this);
										keyelement.setComponent(pollutant);
										key.add(keyelement);

								}
						}


				}
				return key;
		}
}




















































