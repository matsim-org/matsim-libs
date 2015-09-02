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
package floetteroed.utilities;

import java.io.PrintStream;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ErrorMsgPrinter {

	private ErrorMsgPrinter() {
		// do note instantiate
	}

	public static void toStream(final Exception e, final PrintStream stream) {
		stream.println();
		stream.println("The program terminated "
				+ "because of an unrecoverable error:");
		stream.println(e.getMessage());
		stream.println();
		stream.println("------------------------------"
				+ "------------------------------");
		stream.println("Stack trace:");
		e.printStackTrace(stream);
	}

	public static void toStdOut(final Exception e) {
		toStream(e, System.out);
	}

	public static void toErrOut(final Exception e) {
		toStream(e, System.err);
	}
}
