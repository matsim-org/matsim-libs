/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoPlanAnalyzer.java
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
package playground.johannes.socialnetworks.survey.mz2005;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author illenberger
 *
 */
public class PseudoPlanAnalyzer {

	public static TObjectIntHashMap<String> makeChainHistogram(Collection<PersonContainer> persons) {
		TObjectIntHashMap<String> hist = new TObjectIntHashMap<String>();
		for(PersonContainer person : persons) {
			hist.adjustOrPutValue(person.plan.activityChain(), 1, 1);
		}
		return hist;
	}
	
	public static void writeChainHistogram(TObjectIntHashMap<String> hist, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("chain\tcount");
		writer.newLine();
		
		TObjectIntIterator<String> it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(it.value()));
			writer.newLine();
		}
		writer.close();
	}
}
