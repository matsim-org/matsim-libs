/**
 * se.vti.utils
 * 
 * Copyright (C) 2015-2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.utils.misc.statisticslogging;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param D
 *            the data type to be logged
 */
public class StatisticsMultiWriter<D extends Object> {

	// -------------------- MEMBERS --------------------

	private Map<String, StatisticsWriter<D>> fileName2statsWriter = new LinkedHashMap<String, StatisticsWriter<D>>();

	private final boolean append;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public StatisticsMultiWriter(final boolean append) {
		this.append = append;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void addStatistic(final String logFileName,
			final Statistic<D> statistic) {
		StatisticsWriter<D> statsWriter = this.fileName2statsWriter
				.get(logFileName);
		if (statsWriter == null) {
			statsWriter = new StatisticsWriter<D>(logFileName, this.append);
			this.fileName2statsWriter.put(logFileName, statsWriter);
		}
		statsWriter.addSearchStatistic(statistic);
	}

	// -------------------- FILE WRITING --------------------

	public void writeToFile(final D data,
			final String... labelOverrideValueSequence) {
		for (StatisticsWriter<D> statsWriter : this.fileName2statsWriter
				.values()) {
			statsWriter.writeToFile(data, labelOverrideValueSequence);
		}
	}

}
