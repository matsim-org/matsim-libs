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

package playground.yu.bottleneck;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

/**
 * this Class offers a possibility, the traffic volume on a link with id "15",
 * which has a bottleneck, to measure und the result in a .txt-file to write.
 *
 * @author ychen
 *
 */
public class BottleneckTraVol implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI {
	private BufferedWriter out = null;
	private int cnt;

	/**
	 * @param filename
	 * 				the filename of the .txt-file to write
	 */
	public BottleneckTraVol(final String filename) {
		init(filename);
	}

	/**
	 * measures the amount of the agents on the link with bottleneck by every Event
	 */
	public void handleEvent(final EventLinkEnter event) {
		if (event.linkId.equals("15")) {
			writeLine(Gbl.writeTime(event.time - 1) + "\t" + this.cnt);
			writeLine(Gbl.writeTime(event.time) + "\t" + (++this.cnt));
		}
	}

	public void handleEvent(final EventLinkLeave event) {
		if (event.linkId.equals("15")) {
			writeLine(Gbl.writeTime(event.time - 1) + "\t" + this.cnt);
			writeLine(Gbl.writeTime(event.time) + "\t" + (--this.cnt));
		}
	}

	private void init(final String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.cnt = 0;
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			writeLine("time\tvolume");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeLine(final String line) {
		try {
			this.out.write(line);
			this.out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(final int iteration) {
		this.cnt = 0;
	}

	public void closefile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
