/* *********************************************************************** *
 * project: org.matsim.*
 * InundationDataFromBinaryFileReader.java
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
package playground.gregor.otf.readerwriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.matsim.evacuation.otfvis.readerwriter.InundationData;

import playground.gregor.snapshots.OTFSnapshotGenerator;


public class InundationDataFromBinaryFileReader {
	String file = "test.dat";
	
	public InundationData readData(){
		ObjectInputStream o;
		try {
//			o = new ObjectInputStream(new FileInputStream("../../inputs/flooding/flooding_old.dat"));
//			o = new ObjectInputStream(new FileInputStream(this.file));
			o = new ObjectInputStream(new FileInputStream(OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/inundation/20100201_sz_pc_2b_tide_subsidence/flooding.dat"));
			InundationData data = (InundationData) o.readObject();
			o.close();
			return data;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException("Error");
	}
	
	
	
	public static void main(String [] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		String file = "test.dat";
		ObjectInputStream o = new ObjectInputStream(new FileInputStream("test.dat"));
		InundationData data = (InundationData) o.readObject();
		o.close();
		
	}
}
