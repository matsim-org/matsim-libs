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
package floetteroed.utilities.math;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MatrixReader implements TabularFileHandler {

	// -------------------- MEMBERS --------------------

	private List<Vector> currentRows = null;

	private List<Matrix> matrixList = null;

	// -------------------- CONSTRUCTION --------------------

	public MatrixReader() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<Matrix> read(final String fileName) throws IOException {

		this.matrixList = null;
		this.currentRows = null;

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.parse(fileName, this);

		return this.matrixList;
	}

	public List<Matrix> getResult() {
		return this.matrixList;
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startDocument() {
		this.currentRows = new ArrayList<Vector>();
		this.matrixList = new ArrayList<Matrix>();
	}

	@Override
	public String preprocess(final String line) {
		return line;
	}

	private Matrix currentRowsToMatrix() {
		final Matrix result = new Matrix(this.currentRows.size(),
				this.currentRows.get(0).size());
		for (int i = 0; i < result.rowSize(); i++) {
			result.getRow(i).copy(this.currentRows.get(i));
		}
		return result;
	}

	@Override
	public void startRow(String[] row) {
		if (row.length == 0) {
			/*
			 * done with current matrix
			 */
			if (this.currentRows.size() > 0) {
				this.matrixList.add(this.currentRowsToMatrix());
				this.currentRows = new ArrayList<Vector>();
			}
		} else {
			/*
			 * start a new matrix or continue with a current one
			 */
			try {
				final Vector newRow = new Vector(row.length);
				for (int i = 0; i < row.length; i++) {
					newRow.set(i, Double.parseDouble(row[i]));
				}
				this.currentRows.add(newRow);
			} catch (NumberFormatException e) {
				if (this.currentRows.size() == 0) {
					this.matrixList.add(null);
				} else {
					this.matrixList.add(this.currentRowsToMatrix());
				}
				this.currentRows = new ArrayList<Vector>();
			}
		}
	}

	@Override
	public void endDocument() {
		if (this.currentRows.size() > 0) {
			this.matrixList.add(this.currentRowsToMatrix());
		}
		this.currentRows = null;
	}
}
