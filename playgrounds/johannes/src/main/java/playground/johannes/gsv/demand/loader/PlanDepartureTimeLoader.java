/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.demand.loader;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.tasks.PlanDepartureTime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PlanDepartureTimeLoader extends AbstractTaskWrapper {

	public PlanDepartureTimeLoader(String file, Random random) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		ChoiceSet<Integer> choiceSet = new ChoiceSet<Integer>(random);
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			/*
			 * read only the first record
			 */
			String tokens[] = line.split(";");
			for(int i = 2; i < 26; i++) {
				choiceSet.addChoice(i-2, Double.parseDouble(tokens[i].replace(",", ".")));
			}
			break;
		}
		reader.close();
		
		delegate = new PlanDepartureTime(choiceSet, random);
	}
}
