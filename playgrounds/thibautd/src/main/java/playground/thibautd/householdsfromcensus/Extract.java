/* *********************************************************************** *
 * project: org.matsim.*
 * Extract.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.householdsfromcensus;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class Extract {
	private static final String file = "testcases/equil/fakeHouseholds.txt";
	private static final String output = "testcases/equil/fakeHouseholds.XML";

	public static void main(String[] args) {
		Map<Id, List<Id>> cliques;
		CliquesWriter householdWriter;

		cliques = (new ExtractHousholdInfo(file).getCliques());

		householdWriter = new CliquesWriter(cliques);
		try {
			householdWriter.writeFile(output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

