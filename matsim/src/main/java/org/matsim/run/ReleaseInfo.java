/* *********************************************************************** *
 * project: org.matsim.*
 * ReleaseInfo.java
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

package org.matsim.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * This is just a simple class to use as main class in jar-file releases.
 *
 * @author mrieser
 */
public class ReleaseInfo {

	public static void main(final String[] args) {

		// try to load the svn-revision used to build and the build-date from the information in the jar-file
		String revision = null;
		String date = null;
		URL url = ReleaseInfo.class.getResource("/revision.txt");
		if (url != null) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
				revision = reader.readLine();
				date = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// output copyright-message
		System.out.println();
		System.out.println("MATSim");
		System.out.println("    Multi-Agent Transport Simulation Toolkit");
		if (revision == null) {
			System.out.println("    Build: unknown");
		} else {
			System.out.println("    Build: " + revision + " (" + date + ")");
		}

		System.out.println();
		System.out.println("Copyright (C) 2012 by");
		System.out.println("    Kay W. Axhausen, Michael Balmer, Christoph Dobler, Thibaut Dubernet,");
		System.out.println("    Dominik Grether, Andreas Horni, Gregor Laemmel, Nicolas Lefebvre,");
		System.out.println("    Fabrice Marchal, Konrad Meister, Kai Nagel, Andreas Neumann,");
		System.out.println("    Marcel Rieser, David Strippgen, Rashid Waraich, Michael Zilske,");
		System.out.println("    Technische Universitaet Berlin (TU-Berlin) and");
		System.out.println("    Swiss Federal Institute of Technology Zurich (ETHZ)");
		System.out.println();
		System.out.println("This program is distributed under the Gnu Public License (GPL) 2 and");
		System.out.println("comes WITHOUT ANY WARRANTY.");
		System.out.println("Please see the files WARRANTY, LICENSE and COPYING in the distribution.");
		System.out.println();
	}

}
