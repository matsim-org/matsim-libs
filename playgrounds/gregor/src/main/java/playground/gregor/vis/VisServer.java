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

package playground.gregor.vis;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import playground.gregor.proto.ProtoFrame;
import playground.gregor.proto.ProtoFrame.Frame;
import playground.gregor.proto.ProtoFrame.Frame.Event;
import playground.gregor.proto.ProtoFrame.Frame.Event.Type;

public class VisServer {

	public static void main(String[] args) throws IOException {

		Event e = ProtoFrame.Frame.Event.newBuilder()
				.setEvntType(Type.LINK_INF).build();
		Frame f = ProtoFrame.Frame.newBuilder().addEvnt(e).setTime(0).build();

		FileOutputStream output = new FileOutputStream(
				"/Users/laemmel/tmp/frame.ser");
		f.writeTo(output);
		output.close();

		Frame f2 = Frame.parseFrom(new FileInputStream(
				"/Users/laemmel/tmp/frame.ser"));
		System.out.println(f2);
	}

}
