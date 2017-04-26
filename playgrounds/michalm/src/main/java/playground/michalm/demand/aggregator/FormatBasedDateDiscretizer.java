/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand.aggregator;

import java.text.*;
import java.util.Date;

public class FormatBasedDateDiscretizer implements DateDiscretizer {
	public static final String YMDH = "yy_MM_dd_HH";
	public static final String YMD = "yy_MM_dd";
	public static final String H = "HH";
	public static final String u = "u";
	public static final String uH = "u_HH";

	private final DateFormat dateFormat;

	public FormatBasedDateDiscretizer(String pattern) {
		this.dateFormat = new SimpleDateFormat(pattern);
	}

	public FormatBasedDateDiscretizer(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	@Override
	public String discretize(Date date) {
		return dateFormat.format(date);
	}

	public Date parseDiscretizedDate(String date) {
		try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
