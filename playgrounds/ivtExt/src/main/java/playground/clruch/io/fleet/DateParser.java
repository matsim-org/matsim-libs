// code by jph
package playground.clruch.io.fleet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

enum DateParser {
	;
	private static DateFormat DATEFORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public static long from(String string) {
		try {
			Date date = DATEFORMAT.parse(string);
			return date.getTime();
		} catch (Exception exception) {
			throw new RuntimeException(exception.getMessage());
		}
	}
}
