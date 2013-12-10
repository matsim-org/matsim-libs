/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.preprocess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class Analyzer {	
	
	public void run(Population population, String outPath, String day) {	
		double avgNumberOfActs = 0;
		double avgHome = 0;
		double avgWork = 0;
		double avgShop = 0;
		double avgLeisure = 0;
		double avgEducation = 0;
				
		for (Person person : population.getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.getType().startsWith("h")) {
						avgHome++;
					} else if (act.getType().startsWith("w")) {
						avgWork++;
					} else if (act.getType().startsWith("s")) {
						avgShop++;
					} else if (act.getType().startsWith("l")) {
						avgLeisure++;
					} else if (act.getType().startsWith("e")) {
						avgEducation++;
					}
				}
			}			
			avgNumberOfActs += ((person.getSelectedPlan().getPlanElements().size() + 1) / 2.0);
		}
		avgHome /= population.getPersons().size();
		avgWork /= population.getPersons().size();
		avgShop /= population.getPersons().size();
		avgLeisure /= population.getPersons().size();
		avgEducation /= population.getPersons().size();
		avgNumberOfActs /= population.getPersons().size();
		this.write(avgNumberOfActs, avgHome, avgWork, avgShop, avgLeisure, avgEducation, outPath, day);
	}
	
	public void writeHeader(String outPath) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt", true)); 
			bufferedWriter.write("day\tsum\th\tw\ts\tl\te");
			bufferedWriter.newLine();			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(double avgNumberOfActs, double avgHome, double avgWork, double avgShop, double avgLeisure, double avgEducation,
			String outPath, String day) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt", true)); 
			bufferedWriter.append(day + "\t" + avgNumberOfActs + "\t" + avgHome  + "\t" + avgWork  + "\t" + avgShop  + "\t" + avgLeisure  +  "\t" + avgEducation);
			bufferedWriter.newLine();			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
