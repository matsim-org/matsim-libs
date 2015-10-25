/* *********************************************************************** *
 * project: org.matsim.*
 * TestCaseUtils.java
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
package org.matsim.contrib.socnetgen.sna;

/**
 * Utility methods for test cases.
 * 
 * @author jillenberger
 *
 */
public class TestCaseUtils {

	private static final String OUTPUT_DIR = "test/output/";
	
	private static final String INPUT_DIR = "test/input/";

	/**
	 * Returns the input directory path to the package containing the class
	 * <tt>aClass</tt>, e.g. for a calls <tt>org.myself.myclass</tt> the
	 * returned path is <tt>test/input/org/myself/</tt>.
	 * 
	 * @param aClass
	 *            a class in the package of interest.
	 * @return the input directory path to the package.
	 */
	public static String getPackageInputDirecoty(Class<?> aClass) {
		String classPath = aClass.getCanonicalName().replace(".", "/");
		String packagePath = classPath.substring(0, classPath.lastIndexOf("/") + 1);
		StringBuilder builder = new StringBuilder(INPUT_DIR.length() + packagePath.length() + 1);
		builder.append(INPUT_DIR);
		builder.append(packagePath);
		return builder.toString();
	}
	
	/**
	 * Returns the output directory.
	 * 
	 * @return the output directory.
	 */
	public static String getOutputDirectory() {
		return OUTPUT_DIR;
	}
}
