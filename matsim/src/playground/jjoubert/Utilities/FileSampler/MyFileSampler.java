/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleSampler.java
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

package playground.jjoubert.Utilities.FileSampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyPermutator;

public class MyFileSampler {
	private final static Logger log = Logger.getLogger(MyFileSampler.class);
	private File fromFolder;
	private File toFolder;
	
	public MyFileSampler(String fromFoldername, String toFoldername) {
		File fromFolder = new File(fromFoldername);	
		if(!fromFolder.isDirectory()){
			throw new RuntimeException("The given folder " + fromFoldername + " is not a valid source directory!");
		} else{
			this.fromFolder = fromFolder;
		}
		File toFolder = new File(toFoldername);
		if(!toFolder.exists()){
			boolean checkCreate = toFolder.mkdirs();
			if(!checkCreate){
				throw new RuntimeException("Could not successfully create the destination folder " + toFoldername);
			} else{
				this.toFolder = toFolder;
			}
		} else{
			this.toFolder = toFolder;
		}
	}

	public ArrayList<File> sampleFiles(int numberOfSamples, FilenameFilter filter){
		ArrayList<File> result = null;
		File[] fileList = fromFolder.listFiles(filter);
		if(fileList.length > 0){
			if(fileList.length < numberOfSamples){
				log.warn("Although " + numberOfSamples + " files were requested, only " + fileList.length + " are available");
			}
			MyPermutator mp = new MyPermutator();
			ArrayList<Integer> permutation = mp.permutate(fileList.length);
			for(int i = 0; i < Math.min(numberOfSamples, fileList.length); i++){
				File theFile = fileList[permutation.get(i)-1];
				boolean checkMove = copyFile(toFolder, theFile);
				if(!checkMove){
					log.warn("Could not successfully relocate " + theFile.toString());
				}
			}
		} else{
			log.warn("The folder contains no relevant files. A null list is returned!");
		}

		return result;
	}
	
	public boolean copyFile(File destinationFolder, File fromFile) {
		boolean result = false;
		String toFileName = destinationFolder.getAbsolutePath() + "/" + fromFile.getName();
		File toFile = new File(toFileName);

		FileInputStream from = null;
		FileOutputStream to = null;
		try{
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (from != null){
				try {
					from.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				if (to != null){
					try {
						to.close();
						result = true;
					} catch (IOException e3) {
						e3.printStackTrace();
					}
				}
			}
		}
		return result;
	}


}
