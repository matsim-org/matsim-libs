/* *********************************************************************** *
 * project: org.matsim.*
 * LSGWriter.java
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

package playground.anhorni.lsa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkAdaptLength;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.utils.io.IOUtils;

public class LSGWriter {

	private NetworkLayer network;
	private List<Id> lsalinks;

	public LSGWriter() {
		this.lsalinks=new Vector<Id>();
	}

	private void readTLS(){

		try {
			FileReader file_reader = new FileReader("./input/TLS.txt");
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();

			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				this.lsalinks.add(new IdImpl(entries[0].trim()));
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void writeLSG() {

		try {
			System.out.println("writing LSG");
			BufferedWriter out =IOUtils.getBufferedWriter("./output/LSG.txt");
			out.write("Signalgroup\tNode \n");

			int counter=0;
			Iterator<? extends Link> l_it = this.network.getLinks().values().iterator();
			while (l_it.hasNext()) {
				Link link = l_it.next();
				if (this.lsalinks.contains(link.getId())) {
					out.write(counter +"\t" + link.getToNode().getId().toString() + "\n");
					counter++;
				}

			}
			out.flush();
			out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}


	}

	private void readNetwork() {
		this.network = null;
		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile("./input/network.xml");

		// running Network adaptation algorithms
		new NetworkSummary().run(this.network);
		new NetworkAdaptLength().run(this.network);
		new NetworkSummary().run(this.network);
	}


	public static void main(final String[] args) {
		Gbl.startMeasurement();

		LSGWriter ic=new LSGWriter();
		ic.readNetwork();
		ic.readTLS();
		ic.writeLSG();

		Gbl.printElapsedTime();
	}
}
