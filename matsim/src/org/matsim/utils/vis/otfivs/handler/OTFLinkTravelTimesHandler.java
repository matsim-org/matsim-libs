/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLinkTravelTimesHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfivs.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueLink;
import org.matsim.trafficmonitoring.LinkTravelTimeCounter;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfivs.data.OTFDataWriter;


public class OTFLinkTravelTimesHandler extends OTFDefaultLinkHandler {

	private static transient LinkTravelTimeCounter count =null;
	static public class Writer extends  OTFDefaultLinkHandler.Writer {
		public Writer() {
		}
		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			Double erg = count.getLastLinkTravelTime(this.src.getLink().getId().toString());
			if (erg != null) out.putFloat((float)(this.src.getLink().getLength()/erg.doubleValue()));
			else out.putFloat((float)this.src.getLink().getFreespeed(Time.UNDEFINED_TIME));

		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			if (count == null) {
				LinkTravelTimeCounter.init(server.events, 1000000);
				count = LinkTravelTimeCounter.getInstance();
			}
			return new Writer();
		}
	}


}
