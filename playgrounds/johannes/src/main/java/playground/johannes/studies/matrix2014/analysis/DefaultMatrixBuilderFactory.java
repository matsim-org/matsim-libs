/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.analysis;

import playground.johannes.studies.matrix2014.gis.ActivityLocationLayer;
import playground.johannes.studies.matrix2014.matrix.DefaultMatrixBuilder;
import playground.johannes.synpop.gis.ZoneCollection;

/**
 * @author jillenberger
 */
public class DefaultMatrixBuilderFactory implements MatrixBuilderFactory {

    @Override
    public MatrixBuilder create(ActivityLocationLayer locations, ZoneCollection zones) {
        return new DefaultMatrixBuilder(locations, zones);
    }
}
