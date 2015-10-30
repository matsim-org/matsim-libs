/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.apache.log4j.helpers.Loader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * can be then modified and used using <tt>-Dlog4j.configuration=file://\<path\></tt>
 * @author thibautd
 */
public class DropDefaultLog4jXml {
	public static void main(final String[] args) throws IOException {
		final String out = args.length == 0 ? "log4j.xml" : args[0];

		final URL url = Loader.getResource("log4j.xml");

		final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		try (final BufferedWriter writer = IOUtils.getBufferedWriter(out) ) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				writer.write(line);
				writer.newLine();
			}
		}
	}
}
