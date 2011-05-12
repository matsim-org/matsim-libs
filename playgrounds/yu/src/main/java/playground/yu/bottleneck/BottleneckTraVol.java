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

import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * this Class offers a possibility, the traffic volume on a link with id "15",
 * which has a bottleneck, to measure und the result in a .txt-file to write.
 *
 * @author ychen
 *
 */
public class BottleneckTraVol implements LinkEnterEventHandler, LinkLeaveEventHandler {
//	--------------------------MEMBER VARIABLES---------------------------------
	private BufferedWriter out = null;
	private int cnt;
//-----------------------------CONSTRUCTOR-------------------------------------
	/**
	 * @param filename
	 * 				the filename of the .txt-file to write
	 */
	public BottleneckTraVol(final String filename) {
		init(filename);
	}

	/**
	 * measures the amount of the agents on the link with bottleneck by every "entering"-Event
	 */
	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (event.getLinkId().toString().equals("15")) {
			writeLine(Time.writeTime(event.getTime() - 1) + "\t" + cnt);
			writeLine(Time.writeTime(event.getTime()) + "\t" + (++cnt));
		}
	}
	/**
	 * measures the amount of the agents on the link with bottleneck by every "leaving"-Event
	 */
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (event.getLinkId().toString().equals("15")) {
			writeLine(Time.writeTime(event.getTime() - 1) + "\t" + cnt);
			writeLine(Time.writeTime(event.getTime()) + "\t" + (--cnt));
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
		this.out = IOUtils.getBufferedWriter(outfilename);
		writeLine("time\tvolume");
	}

	private void writeLine(final String line) {
		try {
			this.out.write(line);
			this.out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(final int iteration) {
		this.cnt = 0;
	}

	public void closeFile() {
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
