/* *********************************************************************** *
 * project: org.matsim.*
 * OutputDirectoryHierarchy.java
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;

/**
 *
 * Represents the directory hierarchy where the MATSim output goes in.
 *
 * @author dgrether, michaz
 *
 */
public final class OutputDirectoryHierarchy {

	public enum OverwriteFileSetting {failIfDirectoryExists, overwriteExistingFiles, deleteDirectoryIfExists}

	private static final String DIRECTORY_ITERS = "ITERS";

	public static final String MATSIM_TEMP_DIR_PROPERTY = "matsim.tempDir";

	private static final  Logger log = LogManager.getLogger(OutputDirectoryHierarchy.class);

	private String runId = null;

	private final String outputPath;

	private final ControllerConfigGroup.CompressionType defaultCompressionType;

	private OverwriteFileSetting overwriteFiles = OverwriteFileSetting.failIfDirectoryExists;

	@Inject
	OutputDirectoryHierarchy(ControllerConfigGroup config) {

		this(config.getOutputDirectory(),
				config.getRunId(),
				config.getOverwriteFileSetting(),
				config.getCompressionType());
	}

	/**
	 * A constructor with a fairly powerful argument so that it can be adapted to functionality changes without having to change the API.
	 *
	 * @param config
	 */
	public OutputDirectoryHierarchy( Config config ) {
		this( config.controller().getOutputDirectory(), config.controller().getRunId(), config.controller().getOverwriteFileSetting(), config.controller().getCompressionType() );
	}

	public OutputDirectoryHierarchy(String outputPath, OverwriteFileSetting overwriteFiles, ControllerConfigGroup.CompressionType defaultCompressionType) {
		this(outputPath, null, overwriteFiles, true, defaultCompressionType);
	}

	public OutputDirectoryHierarchy(String outputPath, String runId, OverwriteFileSetting overwriteFiles, ControllerConfigGroup.CompressionType defaultCompressionType) {
		this(outputPath, runId, overwriteFiles, true, defaultCompressionType);
	}
	/**
	 *
	 * @param runId the runId, may be null
	 * @param overwriteFiles overwrite existing files instead of crashing
	 * @param outputPath the path to the output directory
	 * @param createDirectories create the directories or abort if they exist
	 */
	public OutputDirectoryHierarchy(String outputPath, String runId, OverwriteFileSetting overwriteFiles, boolean createDirectories, ControllerConfigGroup.CompressionType compressionType){
		this.overwriteFiles = overwriteFiles;
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		this.outputPath = outputPath;
		this.runId = runId;
		this.defaultCompressionType = compressionType;
		if (createDirectories){
			this.createDirectories();
		}
	}

	/**
	 * Returns the path to a directory where temporary files can be stored.
	 *
	 * @return path to a temp-directory.
	 */
	public final String getTempPath() {
		String matsimTempDir = System.getProperty(MATSIM_TEMP_DIR_PROPERTY);
		if (matsimTempDir == null) {
			matsimTempDir = outputPath + "/tmp";
		}
		return matsimTempDir;
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
	 * @return complete path and filename to a file in a iteration directory. if rundId is set then it is prefixed with it
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

	public String getIterationFilename(int iteration, String filename, ControllerConfigGroup.CompressionType compression) {
		if (compression == null) {
			return getIterationFilename(iteration, filename);
		}
		return getIterationFilename(iteration, filename + compression.fileEnding);
	}

	public final String getIterationFilename(int iteration, Controler.DefaultFiles file) {
		return getIterationFilename(iteration, file, this.defaultCompressionType);
	}

	public final String getIterationFilename(int iteration, Controler.DefaultFiles file, ControllerConfigGroup.CompressionType compression) {
		if (compression == null) {
			return getIterationFilename(iteration, file.filename);
		}
		return getIterationFilename(iteration, file.filename + compression.fileEnding);
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

	public String getOutputFilename(String filename, ControllerConfigGroup.CompressionType compression) {
		if (compression == null) {
			return getOutputFilename(filename);
		}
		return getOutputFilename(filename + compression.fileEnding);
	}

	public final String getOutputFilenameWithOutputPrefix(final String filename) {
		return getOutputFilename(Controler.OUTPUT_PREFIX + filename);
	}

	public final String getOutputFilename(Controler.DefaultFiles file) {
		return getOutputFilename(file, this.defaultCompressionType);
	}

	public final String getOutputFilename(Controler.DefaultFiles file, ControllerConfigGroup.CompressionType compression) {
		if (compression == null) {
			return getOutputFilename(Controler.OUTPUT_PREFIX + file.filename);
		}
		return getOutputFilename(Controler.OUTPUT_PREFIX + file.filename + compression.fileEnding);
	}


	public String getOutputPath() {
		return outputPath;
	}

	/**
	 * Creates the path where all iteration-related data should be stored.
	 */
	public final void createIterationDirectory(final int iteration) {
		File dir = new File(getIterationPath(iteration));
		if (!dir.mkdir()) {
			if (this.overwriteFiles == OverwriteFileSetting.overwriteExistingFiles && dir.exists()) {
				log.info("Iteration directory "
						+ getIterationPath(iteration)
						+ " exists already.");
			} else {
				log.warn("Could not create iteration directory "
						+ getIterationPath(iteration) + ".");
			}
		}
	}

	/**
	 * Delete the iteration directory including data for all iterations.
	 */
	public void deleteIterationDirectory() {

		Path path = Path.of(outputPath + "/" + DIRECTORY_ITERS);
		try (Stream<Path> stream = Files.walk(path)) {
			stream
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			log.warn("Could not delete iteration directory " + path + ".");
		}
	}

	private void createDirectories() {
		File outputDir = new File(outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				throw new RuntimeException("Cannot create output directory. "
						+ outputPath + " is a file and cannot be replaced by a directory.");
			}
			if (outputDir.list().length > 0) {
				switch ( overwriteFiles ) {
					case failIfDirectoryExists:
						// the directory is not empty, we do not overwrite any
						// files!
						throw new RuntimeException(
								"The output directory " + outputPath
								+ " (full path: "
								+ outputDir.getAbsolutePath()
								+")"
								+ " already exists and is not empty!"
								+ " Please either delete or empty the directory or"
								+ " configure the services via setOverwriteFileSetting()"
								+ " or the \"overwriteFiles\" parameter of the \"services\" config group.");
					case overwriteExistingFiles:
						// log a warning, even if at the time the user sees it,
						// it is too late to change his mind...
						// I still have problems understanding why people want such a setting.
						// (the above comment is from Thibaut; it was erronously (I am nearly certain) under "deleteDirectoryIfItExists".  kai, may'25)
						System.out.flush();
						log.warn("###########################################################");
						log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
						log.warn("### " + outputPath);
						log.warn("### full path: " + outputDir.getAbsolutePath());
						log.warn("###########################################################");
						System.err.flush();
						break;
					case deleteDirectoryIfExists:
						System.out.flush();
						log.info("###########################################################");
						log.info("### THE CONTROLER WILL DELETE THE EXISTING OUTPUT DIRECTORY:");
						log.info("### " + outputPath);
						log.info("### full path: " + outputDir.getAbsolutePath());
						log.info("###########################################################");
						System.out.flush();
						IOUtils.deleteDirectoryRecursively(outputDir.toPath());
						break;
					default:
						throw new RuntimeException( "unknown setting "+overwriteFiles );
				}
			}
		}

		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new RuntimeException(
					"The output directory path " + outputPath
					+ " could not be created. Check pathname and permissions! Full path: " + new File(outputPath).getAbsolutePath());
		}

		File tmpDir = new File(getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory "
					+ getTempPath() + " could not be created.");
		}
		File itersDir = new File(outputPath + "/" + Controler.DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory "
					+ (outputPath + "/" + Controler.DIRECTORY_ITERS)
					+ " could not be created.");
		}
	}

}
