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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author johannes
 * 
 */
public class ComposeTimeTask implements EpisodeTask {

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.synpop.processing.EpisodeTask#apply(playground.johannes
	 * .gsv.synPop.PlainEpisode)
	 */
	@Override
	public void apply(Episode plan) {
		for (Attributable leg : plan.getLegs()) {
			setStartTime(leg);
			setEndTime(leg);
		}

	}

	private void setEndTime(Attributable leg) {
		StringBuilder builder = new StringBuilder(100);

		boolean valid = true;

		String value = leg.removeAttribute("endTimeYear");
		if (value == null) {
			valid = false;
		} else {
			if (value.equals("1"))
				value = "2001";
			if (value.equals("0"))
				value = "2000";
			if (value.equals("2"))
				value = "2002";
			if (value.equals("99"))
				value = "1999";
			if (value.equals("3899"))
				value = "1999";
			if (value.equals("0082"))
				value = "1982";
			builder.append(value);
			builder.append("-");
		}

		value = leg.removeAttribute("endTimeMonth");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append("-");

		value = leg.removeAttribute("endTimeDay");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append(" ");

		value = leg.removeAttribute("endTimeHour");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append(":");

		value = leg.removeAttribute("endTimeMin");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));

		if (valid) {
			try {
				Date date = dateFormat.parse(builder.toString());
				String out = dateFormat.format(date);
				leg.setAttribute("endTime", out);

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setStartTime(Attributable leg) {
		StringBuilder builder = new StringBuilder(100);

		boolean valid = true;

		String value = leg.removeAttribute("startTimeYear");
		if (value == null) {
			valid = false;
		} else {
			if (value.equals("1"))
				value = "2001";
			if (value.equals("0"))
				value = "2000";
			if (value.equals("2"))
				value = "2002";
			if (value.equals("99"))
				value = "1999";
			if (value.equals("3899"))
				value = "1999";
			if (value.equals("82"))
				value = "1982";
			builder.append(value);
			builder.append("-");
		}

		value = leg.removeAttribute("startTimeMonth");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append("-");

		value = leg.removeAttribute("startTimeDay");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append(" ");

		value = leg.removeAttribute("startTimeHour");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));
		builder.append(":");

		value = leg.removeAttribute("startTimeMin");
		if (value == null) {
			valid = false;
		}
		builder.append(makeTwoDigit(value));

		if (valid) {
			try {
				Date date = dateFormat.parse(builder.toString());
				String out = dateFormat.format(date);
				leg.setAttribute("startTime", out);

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String makeTwoDigit(String number) {
		if (number == null)
			return "";

		int num = Integer.parseInt(number);
		return String.format("%02d", num);
	}
}
