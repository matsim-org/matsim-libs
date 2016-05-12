/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas.hafasCreator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the HAFAS-file BETRIEB_DE and provides the operators in a String-String-Map.
 *
 * @author boescpa
 */
public class OperatorReader {

	protected static Map<String, String> readOperators(String BETRIEB_DE) {
		Map<String, String> operators = new HashMap<>();
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BETRIEB_DE), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				String abbrevationOperator = newLine.split("\"")[1].replace(" ","");
				newLine = readsLines.readLine();
				if (newLine == null) break;
				String operatorId = newLine.substring(8, 14).trim();
				operators.put(operatorId, abbrevationOperator);
				// read the next operator:
				newLine = readsLines.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return operators;
	}

}
