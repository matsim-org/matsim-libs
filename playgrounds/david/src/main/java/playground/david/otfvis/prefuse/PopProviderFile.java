/* *********************************************************************** *
 * project: org.matsim.*
 * PopProviderFile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.david.otfvis.prefuse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vis.otfvis.data.fileio.OTFObjectInputStream;


public class PopProviderFile implements PopulationProvider {

	String filename;
	Population population;
	NetworkImpl network;

	public PopProviderFile(String filename) {
		this.filename = filename;
		try {
			File sourceZipFile = new File(filename);
			// Open Zip file for reading
			ZipFile zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry infoEntry = zipFile.getEntry("net+population.bin");
			BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(infoEntry),5000000);
			ObjectInputStream inFile = new OTFObjectInputStream(is);

			network = (NetworkImpl)inFile.readObject();
			population = (Population)inFile.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public SortedSet<Integer> getIdSet() {
		Set<Id> ids = population.getPersons().keySet();
		SortedSet<Integer> idSet = new TreeSet<Integer>();
		for(Id id : ids){
			int i = Integer.parseInt(id.toString());
			idSet.add(i);
		}
		return idSet;
	}

	public Person getPerson(int id) {
		Person p = population.getPersons().get(new IdImpl(id));
		return p;
	}

}
