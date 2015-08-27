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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SimpleLogFormatter extends Formatter {

	// -------------------- CONSTANTS --------------------

	private final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	// -------------------- MEMBERS --------------------

	private final String prefix;

	// -------------------- CONSTRUCTION --------------------

	public SimpleLogFormatter(final String prefix) {
		this.prefix = prefix;
	}

	// -------------------- OVERRIDING OF Formatter --------------------

	@Override
	public String format(LogRecord rec) {
		final StringBuffer buf = new StringBuffer(512);
		/*
		 * (1) say who this is, and when this happened
		 */
		if (this.prefix != null) {
			buf.append(this.prefix);
		}
		buf.append(this.sdf.format(new Date(rec.getMillis())));
		buf.append(" ");
		/*
		 * (2) say how bad things are
		 */
		buf.append(rec.getLevel());
		/*
		 * (3) say what's going on
		 */
		buf.append(" ");
		buf.append(formatMessage(rec));
		/*
		 * (4) say where this happened only if things are bad
		 */
		if (rec.getLevel().intValue() >= Level.WARNING.intValue()
				|| rec.getLevel().intValue() <= Level.FINE.intValue()) {
			buf.append(" -> ");
			buf.append(rec.getSourceClassName());
			buf.append(".");
			buf.append(rec.getSourceMethodName());
		}
		buf.append("\n");
		return buf.toString();
	}
}
