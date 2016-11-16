/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriterHandlerImpl_v0.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicorePosition;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleWriterHandlerImpl_v2 implements
		DigicoreVehicleWriterHandler {

	@Override
	public void startVehicle(DigicoreVehicle vehicle, BufferedWriter out)
			throws IOException {
		out.write("\n<digicoreVehicle");
		out.write(" id=\"" + vehicle.getId().toString() + "\"");
		out.write(" type=\"" + vehicle.getType().getId().toString() + "\"");
		out.write(" timezone=\"GMT+2\" locale=\"en\"");
		out.write(">\n");
	}

	@Override
	public void endVehicle(BufferedWriter out) throws IOException {
		out.write("</digicoreVehicle>");
	}

	@Override
	public void startChain(BufferedWriter out)
			throws IOException {
		out.write("\t<chain>\n");
	}

	@Override
	public void endChain(BufferedWriter out) throws IOException {
		out.write("\t</chain>\n\n");
	}

	@Override
	public void startActivity(DigicoreActivity activity, BufferedWriter out)
			throws IOException {
		/* Check for required attributes. */
		if(activity.getType() == null){
			throw new RuntimeException("An activity must have a type, even if it is empty.");
		}
		if(activity.getCoord() == null){
			throw new RuntimeException("An activity must have a coordinate.");
		}
		if(activity.getStartTimeGregorianCalendar() == null){
			throw new RuntimeException("An activity must have a start time.");
		}
		if(activity.getEndTimeGregorianCalendar() == null){
			throw new RuntimeException("An activity must have an end time.");
		}
		
		out.write("\t\t<activity");
		out.write(" type=\"" + activity.getType() + "\"\n");
		out.write("\t\t\tstart=\"" + getDateString(activity.getStartTimeGregorianCalendar()) + "\"");
		out.write(" end=\"" + getDateString(activity.getEndTimeGregorianCalendar()) + "\"\n");
		out.write("\t\t\tx=\"" + String.format("%.2f", activity.getCoord().getX()) + "\"");
		out.write(" y=\"" + String.format("%.2f", activity.getCoord().getY()) + "\"");
		if(activity.getFacilityId() != null){
			out.write(" facility=\"" + activity.getFacilityId().toString() + "\"");
		}
		if(activity.getLinkId() != null){
			out.write(" link=\"" + activity.getLinkId().toString() + "\"");
		}
	}

	@Override
	public void endActivity(BufferedWriter out) throws IOException {
		out.write("/>\n");
		
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		/* Don't think a separator will make the file more readable. */
	}
	
	@Override
	public void startTrace(DigicoreTrace trace, BufferedWriter out) throws IOException {
		out.write("\t\t<trace crs=\"" + trace.getCrs() + "\" >\n");
	}

	@Override
	public void endTrace(BufferedWriter out) throws IOException {
		out.write("\t\t</trace>\n");
	}

	@Override
	public void startPosition(DigicorePosition pos, BufferedWriter out) throws IOException {
		out.write("\t\t\t<position");
		out.write(" time=\"" + getDateString(pos.getTimeAsGregorianCalendar()) + "\"");
		out.write(" x=\"" + String.format("%.6f", pos.getCoord().getX()) + "\"");
		out.write(" y=\"" + String.format("%.6f", pos.getCoord().getY()) + "\"");
		if(pos.getCoord().hasZ()){
			out.write(" z=\"" + String.format("%.1f", pos.getCoord().getZ()) + "\"");
		}
	}

	@Override
	public void endPosition(BufferedWriter out) throws IOException {
		out.write(" />\n");
	}

	private String getDateString(GregorianCalendar cal){
		String s = "";
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1; // Seems to be a java thing that month is started at 0... 
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		
		s = String.format("%04d%02d%02d %02d:%02d:%02d", 
				year, month, day, hour, minute, second);
		
		return s;
	}
	
}

