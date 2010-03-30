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

package org.matsim.vis.otfvis.handler;

//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//import org.matsim.ptproject.qsim.QLink;
//import org.matsim.core.trafficmonitoring.LinkTravelTimeCounter;
//import org.matsim.core.utils.misc.Time;
//import org.matsim.vis.otfvis.data.OTFDataWriter;
//
//
///**
// * OTFLinkTravelTimesHandler behaves like the DefulatHandler but transfers the link's free speed
// * as dynamic data. That does not make much sense, as the free speed will not change
// * @author david
// *
// */
//@Deprecated
//public class OTFLinkTravelTimesHandler extends OTFDefaultLinkHandler {
//
//	private static transient LinkTravelTimeCounter count =null;
//	static public class Writer extends  OTFDefaultLinkHandler.Writer {
//
//		private static final long serialVersionUID = -7249785000303972319L;
//
//		{
//			LinkTravelTimeCounter.init(server.getEvents(), 1000000);
//			count = LinkTravelTimeCounter.getInstance();
//		}
//
//		public Writer() {
//		}
//		@Override
//		public void writeDynData(ByteBuffer out) throws IOException {
//			Double erg = count.getLastLinkTravelTime(this.src.getLink().getId());
//			if (erg != null) out.putFloat((float)(this.src.getLink().getLength()/erg.doubleValue()));
//			else out.putFloat((float)this.src.getLink().getFreespeed(Time.UNDEFINED_TIME));
//
//		}
//
//		@Override
//		public OTFDataWriter<QLink> getWriter() {
//			if (count == null)
//			{
//				LinkTravelTimeCounter.init(server.getEvents(), 1000000);
//				count = LinkTravelTimeCounter.getInstance();
//			}
//			return new Writer();
//		}
//	}
//
//
//}
