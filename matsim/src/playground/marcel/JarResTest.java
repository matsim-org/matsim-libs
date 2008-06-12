/* *********************************************************************** *
 * project: org.matsim.*
 * JarResTest.java
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

package playground.marcel;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class JarResTest {

	public static void main(final String[] args) {

		final String filename = "logo.png";

		File file = new File("res/" + filename);
		if (file.exists()) {
			System.out.println("found file in local file system: " + file.getAbsolutePath());
			System.out.println("file size: " + file.length());
		} else {
			System.out.println("did NOT find file in local file system");
		}

		URL url = JarResTest.class.getResource("/res/" + filename);
		if (url == null) {
			System.out.println("did NOT find file in jar");
		} else {
			System.out.println("found file in jar:");
			System.out.println("  url.getFile() = " + url.getFile());
			try {
				System.out.println("  url.toURI() = " + url.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			System.out.println("  url.toExternalForm() = " + url.toExternalForm());

			try {
				file = new File(url.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			if (file.exists()) {
				System.out.println("file exists in jar: " + file.getAbsolutePath());
				System.out.println("file size: " + file.length());
			} else {
				System.out.println("file does NOT exist in jar.");
			}
		}

	}

}
