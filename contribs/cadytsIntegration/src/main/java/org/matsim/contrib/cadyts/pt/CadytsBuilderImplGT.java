/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.pt;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsBuilderImpl;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.core.config.Config;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;

/**
 * @author nagel
 * @author mrieser
 */
public final class CadytsBuilderImplGT {
	private static Logger log = Logger.getLogger( CadytsBuilderImplGT.class ) ;

	public static <T> AnalyticalCalibrator<T> buildCalibratorAndAddMeasurements(final Config config, final Counts<T> occupCounts,
																		 LookUpItemFromId<T> lookUp, Class<T> idType ) {

		if (occupCounts.getCounts().size() == 0) {
			log.warn("Counts container is empty.");
		}

		AnalyticalCalibrator<T> matsimCalibrator = CadytsBuilderImpl.buildCalibrator(config);

		//add counts data into calibrator
		int numberOfAddedMeasurements = 0 ;
		for (Map.Entry<Id<T>, Count<T>> entry : occupCounts.getCounts().entrySet()) {
			// (loop over all counting "items" (usually locations/stations)

			T item = lookUp.getItem(Id.create(entry.getKey(), idType)) ;
			if ( item==null ) {
				throw new RuntimeException("item is null; entry=" + entry + " idType=" + idType ) ;
			}

			double sum = 0; 
			for (Volume volume : entry.getValue().getVolumes().values()){
				// (loop over the different time slots)
				sum += volume.getValue() ;
			}
			numberOfAddedMeasurements++ ;
			matsimCalibrator.addMeasurement(item, 0, 86400, sum, SingleLinkMeasurement.TYPE.COUNT_VEH );
		}

		if ( numberOfAddedMeasurements==0 ) {
			log.warn("No measurements were added.");
		}

		if ( matsimCalibrator.getProportionalAssignment() ) {
			throw new RuntimeException("Gunnar says that this may not work so do not set to true. kai, sep'14") ;
		}
		return matsimCalibrator;
	}
}