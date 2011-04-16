/* *********************************************************************** *
 * project: org.matsim.*
 * SheltersReader.java
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
import org.matsim.vis.otfvis.interfaces.OTFDataReader;


public class SheltersReader extends OTFDataReader {

	private OTFSheltersDrawer drawer;
	private TimeDependentTrigger receiver;

	@Override
	public void connect(OTFDataReceiver receiver) {
		this.receiver = (TimeDependentTrigger) receiver;
	}

	@Override
	public void invalidate(SceneGraph graph) {
//		this.drawer.invalidate(graph);
		this.receiver = new TimeDependentTrigger();
		this.receiver.setDrawer(this.drawer);
		graph.addItem(this.receiver);
		this.receiver.setTime(graph.getTime());

//		this.receiver.invalidate(graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {

		int size = in.getInt();

		 byte[] byts = new byte[size];

		    in.get(byts);

		    ObjectInputStream istream = null;

		    try {
		        istream = new ObjectInputStream(new ByteArrayInputStream(byts));
		        Object obj = istream.readObject();

		        if(obj instanceof OTFSheltersDrawer){
		        	this.drawer = (OTFSheltersDrawer) obj;
		            System.out.println("deserialization successful");
		        }
		    }
		    catch(IOException e){
		        e.printStackTrace();
		    }
		    catch(ClassNotFoundException e){
		        e.printStackTrace();
		    }
//		this.drawer.setData(data);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// no dyn data
	}
}


