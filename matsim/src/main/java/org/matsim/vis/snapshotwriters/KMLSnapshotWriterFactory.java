/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSnapshotWriterFactory
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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class KMLSnapshotWriterFactory implements SnapshotWriterFactory {

	@Override
	public SnapshotWriter createSnapshotWriter(String filename, Scenario scenario) {
		String coordSystem = scenario.getConfig().global().getCoordinateSystem();
		return new KmlSnapshotWriter(filename, TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84));
	}

	@Override
	public String getPreferredBaseFilename() {
		return "googleearth.kmz";
	}

}
