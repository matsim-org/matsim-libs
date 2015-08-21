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

package playground.johannes.gsv.synPop.mid.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.synpop.data.PlainPerson;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class MonthTask extends AnalyzerTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.gsv.synPop.analysis.AnalyzerTask#analyze(java.util
	 * .Collection, java.util.Map)
	 */
	@Override
	public void analyze(Collection<PlainPerson> persons, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();
		for (PlainPerson person : persons) {
			String month = person.getAttribute(MiDKeys.PERSON_MONTH);
			if (month != null) {
				values.adjustOrPutValue(month, 1.0, 1.0);
			}
		}

		if (outputDirectoryNotNull()) {
			try {
				TXTWriter.writeMap(values, "month", "count", String.format("%s/months.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
