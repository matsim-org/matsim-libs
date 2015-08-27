/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class DistanceJourneyDaysTask extends AnalyzerTask {

	private final String mode;

	public DistanceJourneyDaysTask(String mode) {
		this.mode = mode;
	}

	@Override
	public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
		if (outputDirectoryNotNull()) {
			TDoubleArrayList days = new TDoubleArrayList();
			TDoubleArrayList distances = new TDoubleArrayList();

			for (Person person : persons) {
				for (Episode plan : person.getEpisodes()) {
					String dayVal = plan.getAttribute(MiDKeys.JOURNEY_DAYS);
					if (dayVal != null) {
						for (Attributable leg : plan.getLegs()) {
							if (mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
								String distVal = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
								if (distVal != null) {
									double d = Double.parseDouble(distVal);
									double day = Double.parseDouble(dayVal);
									
									days.add(day);
									distances.add(d);
								}
							}
						}
					}
				}
			}

			TDoubleDoubleHashMap map = Correlations.mean(days.toNativeArray(), distances.toNativeArray());
			try {
				TXTWriter.writeMap(map, "days", "distance", String.format("%s/d.days.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
