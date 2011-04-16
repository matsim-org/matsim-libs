/* *********************************************************************** *
 * project: org.matsim.*
 * InundationDataReader_v2.java
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
package playground.gregor.snapshots.writers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.fileio.OTFObjectInputStream;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

import playground.gregor.otf.readerwriter.InundationData;

public class InundationDataReader_v2 extends OTFDataReader {

	private final OTFInundationDrawer drawer;
	private TimeDependentTrigger receiver;

	public InundationDataReader_v2() {
		this.drawer = new OTFInundationDrawer();
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.receiver = (TimeDependentTrigger) receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.receiver = new TimeDependentTrigger();
		this.receiver.setDrawer(this.drawer);
		graph.addItem(this.receiver);
		this.receiver.setTime(graph.getTime());
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {

		double startTime = in.getDouble();
		this.drawer.setStartTime(startTime);
		int size = in.getInt();

		 byte[] byts = new byte[size];
		    in.get(byts);
		    ObjectInputStream istream = null;

		    try {
		    	istream = new OTFObjectInputStream(new ByteArrayInputStream(byts));
		        Object obj = istream.readObject();

		        if(obj instanceof InundationData){
		        	this.drawer.setData((InundationData) obj);
		            System.out.println("deserialization successful");
		        }
		    }
		    catch(IOException e){
		        e.printStackTrace();
		    }
		    catch(ClassNotFoundException e){
		        e.printStackTrace();
		    }
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// no dyn data
	}

}
