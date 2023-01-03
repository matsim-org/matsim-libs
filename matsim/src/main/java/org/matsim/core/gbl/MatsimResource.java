/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimResource.java
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

package org.matsim.core.gbl;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class to load data from files in the resource directory
 * (<code>./res/</code>). Because the resource directory may be included into
 * jar-files in releases, one must not directly access them with the hard-coded
 * path (e.g. <code>new File("res/foo.bar");</code>), as the file would not be
 * found if it is inside a jar-file. Instead, use the methods in this class to
 * access files in the resource directory. The methods in this class ensure
 * that the files can be loaded no matter whether they are inside a jar-file or
 * not.<br>
 *
 * All filenames must be given relative to the resource directory. This means,
 * that the name of the resource-directory must not be part of the filenames
 * passed to the methods. E.g., to access the file <code>res/foo/bar.txt</code>,
 * where <code>res/</code> is the resource directory, one must only pass
 * <code>foo/bar.txt</code> as filename.
 *
 * @author mrieser
 */
public abstract class MatsimResource {

	/** The path where resources are located in jar-files. */
	private static final String RES_PATH_JARFILE = "/res/";

	/** The path where resources are located in a local file system. */
	private static final String RES_PATH_LOCAL = "./res/";  //NOPMD // this line should be ignored for PMD analysis
	private static final String RES_PATH_LOCAL2 = "./src/main/resources/res/";  //NOPMD // this line should be ignored for PMD analysis

	private static final Logger log = LogManager.getLogger(MatsimResource.class);

	/**
	 * @param filename relative path from within the resource directory to a file to be loaded
	 * @return an URL pointing to the requested resource file, or <code>null</code> if no such file exists.
	 */
	public final static URL getAsURL(final String filename) {
		// look for the file locally
		{
		File file = new File(RES_PATH_LOCAL + filename);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.warn("Found resource-file, but could not return URL for it.", e);				// just continue, maybe we have more luck in the classpath
			}
		}
		}
		{
		File file = new File(RES_PATH_LOCAL2 + filename);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.warn("Found resource-file, but could not return URL for it.", e);				// just continue, maybe we have more luck in the classpath
			}
		}
		}
		// maybe we find the file in the classpath, possibly inside a jar-file
		URL url = MatsimResource.class.getResource(RES_PATH_JARFILE + filename);
		if (url == null) {
			log.warn("Resource '" + filename + "' not found!");
		}
		return url;
	}

	/**
	 * @param filename relative path from within the resource directory to a file to be loaded
	 * @return a Stream to the requested resource file, or <code>null</code> if no such file exists.
	 */
	public final static InputStream getAsInputStream(final String filename) {
		// look for the file locally
		try {
			return new FileInputStream(RES_PATH_LOCAL + filename);
		} catch (FileNotFoundException e) {
			log.info("Resource '" + filename + "' not found locally. May not be fatal.");
			// just continue, maybe we have more luck in the classpath
		}
		// maybe we find the file in the classpath, possibly inside a jar-file
		InputStream stream = MatsimResource.class.getResourceAsStream(RES_PATH_JARFILE + filename);
		if (stream == null) {
			log.warn("Resource '" + filename + "' not found!");
		}
		return stream;
	}

	/**
	 * @param filename relative path from within the resource directory to a file to be loaded
	 * @return a Stream to the requested resource file, or <code>null</code> if no such file exists.
	 */
	public final static Image getAsImage(final String filename) {
		final URL url = getAsURL(filename);
		if (url == null) {
			return null;
		}
		try {
			return ImageIO.read(url);
		} catch (IOException e) {
			log.error("Could not load requested image", e);
			return null;
		}
	}
}
