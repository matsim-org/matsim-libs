/* *********************************************************************** *
 * project: org.matsim.*
 * TolledLinksFileParser.java
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

package playground.telaviv.roadpricing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

public class TolledLinksFileParser {

	private final String separator;

	public TolledLinksFileParser(String separator) {
		this.separator = separator;
	}
	
	public List<Tuple<Id<Node>, Id<Node>>> readFile(String fileName) throws IOException {
		
		List<Tuple<Id<Node>, Id<Node>>> tuples = new ArrayList<>();
		
		BufferedReader reader = IOUtils.getBufferedReader(fileName);
		
		// skip first Line
		reader.readLine();
			
		String line;
		while((line = reader.readLine()) != null) {
			
			String[] cols = line.split(separator);
			
			Id<Node> fromNodeId = Id.create(cols[0], Node.class);
			Id<Node> toNodeId = Id.create(cols[1], Node.class);
			
			tuples.add(new Tuple<Id<Node>, Id<Node>>(fromNodeId, toNodeId));
		}
		
		return tuples;
	}
}