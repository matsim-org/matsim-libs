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

import java.io.FileNotFoundException;
import java.io.PrintWriter;



/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MatrixWriter {

	// -------------------- MEMBERS --------------------

	private PrintWriter writer = null;

	// -------------------- CONSTRUCTION --------------------

	public MatrixWriter() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void open(final String fileName) throws FileNotFoundException {
		this.writer = new PrintWriter(fileName);
	}

	// TODO new: write a 1x1 matrix
	public void write(final double m) {
		this.writer.println(m);
		this.writer.println();
	}

	// TODO new: write a 1xM matrix
	public void write(final Vector m) {
		this.write(new Matrix(m));
	}

	// TODO new: write a 1xM matrix
	public void write(final double... m) {
		this.write(new Vector(m));
	}

	public void write(final Matrix m) {
		for (int i = 0; i < m.rowSize(); i++) {
			final Vector row = m.getRow(i);
			for (int j = 0; j < m.columnSize(); j++) {
				this.writer.print(row.get(j));
				this.writer.print(" ");
			}
			this.writer.println();
		}
		this.writer.println();
	}

	public void close() {
		this.writer.flush();
		this.writer.close();
	}

	// TODO NEW
	public static void write(final String fileName, final Matrix m)
			throws FileNotFoundException {
		final MatrixWriter writer = new MatrixWriter();
		writer.open(fileName);
		writer.write(m);
		writer.close();
	}

}
