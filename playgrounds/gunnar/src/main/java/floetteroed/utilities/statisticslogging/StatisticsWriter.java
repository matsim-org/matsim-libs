/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.statisticslogging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param D
 *            the data type to be logged
 */
public class StatisticsWriter<D extends Object> {

	// -------------------- CONSTANTS --------------------

	private final String fileName;

	private final List<Statistic<D>> statistics = new ArrayList<Statistic<D>>();

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public StatisticsWriter(final String fileName) {
		this.fileName = fileName;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void addSearchStatistic(final Statistic<D> statistic) {
		this.statistics.add(statistic);
	}

	public String getFileName() {
		return this.fileName;
	}

	// -------------------- FILE WRITING --------------------

	private boolean wroteHeader = false;

	public void writeToFile(final D data) {
		try {
			final BufferedWriter writer;
			if (!this.wroteHeader) {
				writer = new BufferedWriter(
						new FileWriter(this.fileName, false));
				for (Statistic<D> stat : this.statistics) {
					writer.write(stat.label() + "\t");
				}
				writer.newLine();
				this.wroteHeader = true;
			} else {
				writer = new BufferedWriter(new FileWriter(this.fileName, true));
			}
			for (Statistic<D> stat : this.statistics) {
				writer.write(stat.value(data) + "\t");
			}
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
