/* *********************************************************************** *
 * project: org.matsim.*
 * BottleneckTraVol.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.algorithms.EventWriterTXT;

/**
 * this Class offers a possibility, the traffic volume on a link with id "15",
 * which has a bottleneck, to measure und the result in a .txt-file to write.
 * 
 * @author ychen
 * 
 */
public class BottleneckTraVol extends EventWriterTXT {
	private int cnt;

	/**
	 * @param filename -
	 * the filename of the .txt-file to write
	 */
	public BottleneckTraVol(String filename) {
		super(filename);
	}

	/**
	 * measures the amount of the agents on the link with bottleneck by every Event
	 * */
	@Override
	public void handleEvent(BasicEvent event) {
		if (event instanceof EventLinkEnter) {
			EventLinkEnter ele = (EventLinkEnter) event;
			int time = (int) ele.time;
			if (ele.linkId.equals("15")) {
				writeLine(time(time - 1) + "\t" + cnt);
				writeLine(time(time) + "\t" + (++cnt));
			}
		} else if (event instanceof EventLinkLeave) {
			EventLinkLeave ell = (EventLinkLeave) event;
			int time = (int) ell.time;
			if (ell.linkId.equals("15")) {
				writeLine(time(time - 1) + "\t" + cnt);
				writeLine(time(time) + "\t" + (--cnt));
			}
		}
	}

	/**
	 * writes file-head 
	 * @see org.matsim.events.algorithms.EventWriterTXT#init(java.lang.String)
	 */
	@Override
	public void init(String outfilename) {
		super.init(outfilename);
		cnt = 0;
		writeLine("time\tvolume");
	}

	/**
	 * changes the form of time from "ssss.." to "hh:mm:ss"
	 * @param time - time in form "21600" (=6:00)
	 * @return String of the time in form "hh:mm:ss" (e.g. "6:00:00")
	 */
	public String time(int time) {
		int h = time / 3600;
		int m = (time - h * 3600) / 60;
		int s = time - h * 3600 - m * 60;
		return h + ":" + m + ":" + s;
	}
}
