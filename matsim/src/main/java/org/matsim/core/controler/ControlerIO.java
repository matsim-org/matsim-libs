/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerIo
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
package org.matsim.core.controler;

import org.matsim.api.core.v01.Id;
/**
 * @author dgrether
 *
 */
public class ControlerIO {
	
	private static final String DIRECTORY_ITERS = "ITERS";
	
	private Id runId = null;
	
	private final String outputPath;
	
	public ControlerIO(String outputDirectory){
		this.outputPath = outputDirectory;
	}
	
	/**
	 * 
	 * @param outputDirectory the path to the output directory
	 * @param runId the runId, may be null
	 */
	public ControlerIO(String outputDirectory, Id runId){
		this(outputDirectory);
		this.runId = runId;	
	}
		
	/**
	 * Returns the path to a directory where temporary files can be stored.
	 *
	 * @return path to a temp-directory.
	 */
	public final String getTempPath() {
		return outputPath + "/tmp";
	}

	/**
	 * Returns the path to the specified iteration directory. The directory path
	 * does not include the trailing '/'.
	 *
	 * @param iteration
	 *            the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final String getIterationPath(final int iteration) {
		return outputPath + "/" + DIRECTORY_ITERS + "/it." + iteration;
	}

	/**
	 * Returns the complete filename to access an iteration-file with the given
	 * basename.
	 *
	 * @param filename
	 *            the basename of the file to access
	 * @return complete path and filename to a file, if set prefixed with the runId, in a iteration directory
	 */
	public final String getIterationFilename(final int iteration, final String filename) {
		StringBuilder s = new StringBuilder(getIterationPath(iteration));
		s.append('/');
		if (runId != null) {
			s.append(runId);
			s.append('.');
		}
		s.append(iteration);
		s.append(".");
		s.append(filename);
		return s.toString();
	}
	
	/**
	 * Returns the complete filename to access a file in the output-directory.
	 *
	 * @param filename
	 *            the basename of the file to access
	 * @return complete path and filename to a file, if set prefixed with the runId,  in the output-directory
	 */
	public final String getOutputFilename(final String filename) {
		StringBuilder s = new StringBuilder(outputPath);
		s.append('/');
		if (runId != null) {
			s.append(runId);
			s.append('.');
		}
		s.append(filename);
		return s.toString();
	}

	
	public String getOutputPath() {
		return outputPath;
	}
}
