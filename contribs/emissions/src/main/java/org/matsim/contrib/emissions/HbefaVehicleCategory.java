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

import java.util.List;

/**
 * @author benjamin
 *
 */
public enum HbefaVehicleCategory {
	PASSENGER_CAR(List.of("PC")),
	LIGHT_COMMERCIAL_VEHICLE(List.of("LCV")),
	HEAVY_GOODS_VEHICLE(List.of("HGV")),
	URBAN_BUS(List.of("UBus")),
	COACH(List.of("Coach")),
	MOTORCYCLE(List.of("MC", "SMC", "moped")),
	NON_HBEFA_VEHICLE(List.of());

	public final List<String> ids;

	HbefaVehicleCategory(List<String> ids) {
		this.ids = ids;
	}
}
