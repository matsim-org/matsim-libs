/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data.vehtrajectories;

import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;

/**
 * @author mrieser
 */
public class Tester {

	public static void main(final String[] args) {

		String zoneMappingFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/L41_Kreuzlingen_Konstanz_Nachfrage/l41 ZoneNo_TAZ_mapping.csv";
		String vehTrajectoryFilename = "/Volumes/Data/virtualbox/exchange/kreuzlingen/output8/VehTrajectory.dat";

		ZoneIdToIndexMapping zoneMapping = new ZoneIdToIndexMapping();
		new ZoneIdToIndexMappingReader(zoneMapping).readFile(zoneMappingFilename);

		DynamicTravelTimeMatrix matrix = new DynamicTravelTimeMatrix(600, 30*3600.0);
		new VehicleTrajectoriesReader(new CalculateTravelTimeMatrixFromVehTrajectories(matrix), zoneMapping).readFile(vehTrajectoryFilename);
		matrix.dump();
	}
}
