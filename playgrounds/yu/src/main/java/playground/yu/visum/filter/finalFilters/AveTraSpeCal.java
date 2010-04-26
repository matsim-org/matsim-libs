/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.visum.filter.finalFilters;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LinkEnterEventImpl;

import playground.yu.visum.writer.UserDefAtt;

/**
 * A AveTraSpeCal can calculate the average travelspeed, which can be exported
 * as the attribut defined by user for VISUM 9.3, on a link in a timeBin(15 min)
 * and also output corresponding result to print.
 *
 * @author ychen
 */
public class AveTraSpeCal extends LinkAveCalA {

	/* ---------------------CONSTRUCTOR-------------------- */
	/**
	 * the complete Information of some events can not be read
	 *            regularly without the constructor
	 * @param plans -
	 *            contains useful information from plans-file
	 * @param network -
	 *            contains useful information from network-file
	 */
	public AveTraSpeCal(final Population plans, final Network network) {
		super(plans, network);
	}

	/*------------------------GETTER---------------------- */
	/**
	 * Is called by double atxCal(int linkID, String timeBin)
	 *
	 * @param linkId -
	 *            the id of the LINK in VISUM 9.3.
	 * @attention LinkId isn't the linkId in MATSIM!
	 * @param time_s -
	 *            any definite point of time with unit second. e.g. 06:00:00 is
	 *            here 21600.
	 * @return the value of the travelspeed direct measured on a link during a
	 *         timeBin
	 */
	public double getLinkTraSpeed(final String linkId, final int time_s) {
		return getLinkCalResult(linkId, time_s, "speed", "m/s");
	}

	/**
	 * Accumulates the travelspeed; Is called in void
	 * org.matsim.playground.filters.filter.finalFilters.LinkAveCalA.handleEvent(Event
	 * event)
	 */
	@Override
	public void compute(final LinkEnterEventImpl event, final double leaveTime) {
		try {
			computeInside(network.getLinks().get(event.getLinkId()).getLength()
					/ (leaveTime - event.getTime()), event.getLinkId().toString(),
					(long)(event.getTime() / 900));
		} catch (NullPointerException e) {
//			System.err.println(e);
		}
	}

	/**
	 * exports a set of attribut, which is defined by user of VISUM 9.3. The
	 * corresponding ATTID is called timeBin-No.+"AVETS", and the corresponding
	 * CODE and NAME is called e.g. "aveTraSpeed06:00-06:14",
	 * "aveTraSpeed08:30-08:44" etc.; Is called in void
	 * org.matsim.playground.filters.writer.PrintStreamUDANET.output(FinalEventFilterI
	 * fef).
	 *
	 * @return a set of attribut, which is defined by user of VISUM 9.3. The
	 *         corresponding ATTID is called timeBin-No.+"AVETS", and the
	 *         corresponding CODE and NAME is called e.g.
	 *         "aveTraSpeed06:00-06:14", "aveTraSpeed08:30-08:44" etc.;
	 */
	@Override
	public List<UserDefAtt> UDAexport() {
		return UDAexport("aveTraSpeed", "AVETS");
	}

	/**
	 * Determines the average travelspeed on a link during a timeBin; If ats(the
	 * average travelspeed)=0, then ats=freespeed on the link; If ats>0, the
	 * ats=ats.
	 *
	 * @return the value of average travelspeed on a link during a timeBin.
	 */
	@Override
	public double atxCal(final String linkID, final String timeBin) {
		double ats = getLinkTraSpeed(linkID, Integer.parseInt(timeBin) * 900);
		ats = (ats != 0) ? ats : (this.network.getLinks().get(new IdImpl(linkID))).getFreespeed();
		return ats;
	}
}
