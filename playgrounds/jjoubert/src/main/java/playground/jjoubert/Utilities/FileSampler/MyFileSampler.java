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
import java.util.List;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyPermutator;

/**
 * This class allows you to randomly sample files from a specified <i>input</i> folder.
 * It is also possible, depending on the constructor used, to copy the selected files to 
 * a specified <i>output</i> folder.
 * 
 * @author jwjoubert
 */
public class MyFileSampler {
	private final static Logger log = Logger.getLogger(MyFileSampler.class);
	private File fromFolder;
	private File toFolder;
	
	/**
	 * The constructor instantiates an instance of of the class.
	 * 
	 * @param fromFoldername the absolute path name of the folder from which files will 
	 * 		be considered.
	 * @param toFoldername the absolute path name of the folder to which sampled files 
	 * 		will be copied (if selected).
	 */
	public MyFileSampler(String fromFoldername, String toFoldername) {
		File fromFolder = new File(fromFoldername);	
		if(!fromFolder.isDirectory()){
			throw new RuntimeException("The given folder " + fromFoldername + " is not a valid source directory!");
		} else{
			this.fromFolder = fromFolder;
		}
		if(toFoldername != null){
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
		}else{
			this.toFolder = null;
		}
	}

	/**
	 * The constructor instantiates an instance of of the class. If you do want to copy
	 * the selected files to a separate folder, you have to use another constructor. 
	 * 
	 * @param fromFoldername the absolute path name of the folder from which files will 
	 * 		be considered.
	 */
	public MyFileSampler(String fromFoldername){
		this(fromFoldername, null);
	}

	/**
	 * The method samples from a filtered file list. 
	 * @param number the number of files that must be sampled. 
	 * @param filter the type <code>java.io.FilenameFilter</code> filter that will be 
	 * 		used to filter the files from the source folder. In my case, I have written
	 * 		my own <code>playground.jjoubert.Utilities.FileSampler.MyFileFilter</code>
	 * 		that uses the file extension as filter.
	 * @return a <code>List</code> of <code>File</code>s sampled.
	 * @author jwjoubert
	 */
	public List<File> sampleFiles(int number, FilenameFilter filter){
		log.info("Sampling " + number + " files from " + fromFolder.toString());
		if(toFolder != null){
			log.info("Copying sampled files to " + toFolder.toString());
		}
		List<File> result = null;
		File[] fileList = fromFolder.listFiles(filter);
		if(fileList.length > 0){
			result = new ArrayList<File>();
			if(fileList.length < number){
				log.warn("Although " + number + " files were requested, only " + fileList.length + " are available");
			}
			MyPermutator mp = new MyPermutator();
			ArrayList<Integer> permutation = mp.permutate(fileList.length);
			for(int i = 0; i < Math.min(number, fileList.length); i++){
				File theFile = fileList[permutation.get(i)-1];
				if(toFolder != null){
					boolean checkMove = copyFile(toFolder, theFile);
					if(!checkMove){
						log.warn("Could not successfully relocate " + theFile.toString());
					}
				}
				result.add(theFile);
			}
		} else{
			log.warn("The folder contains no relevant files. A null list is returned!");
		}
		log.info("File sampling complete.");
		return result;
	}
	
	/**
	 * The method copies a given file to a given folder, instead of just relocating the
	 * file to the destination as is done with the <code>File.renameTo()</code> method.
	 * @param destinationFolder the folder of type <code>File</code> to which the given
	 * 		file will be copied.
	 * @param fromFile the <code>File</code> that will be copied.
	 * @return <code>true</code> if and only if the file was copied successfully. 
	 */
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
