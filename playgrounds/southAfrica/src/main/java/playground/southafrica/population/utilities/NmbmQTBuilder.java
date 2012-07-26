/* *********************************************************************** *
 * project: org.matsim.*
 * NmbmQTBuilder.java
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

package playground.southafrica.population.utilities;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.QuadTree;

import playground.southafrica.utilities.Header;

public class NmbmQTBuilder {
	private final static Logger LOG = Logger.getLogger(NmbmQTBuilder.class);
	private final String inputFolder;
	private Map<Id, QuadTree<Plan>> qtMap; 
	private Census2001SampleReader cr;
	
	private final String[] genderClasses = {"m","f"};
	private final String[] ageClasses = {"6","18","30","60",""};
	private final String[] incomeClasses = {"0", };

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmQTBuilder.class.toString(), args);
		
		String inputFolder = args[0];
		String dummy1 = args[1];
		String dummy2 = args[2];
		String dummy3 = args[3]; 
		
		NmbmQTBuilder nqtb = new NmbmQTBuilder(inputFolder);
		
		Header.printFooter();
	}
	
	public NmbmQTBuilder(String inputFolder) {
		this.inputFolder = inputFolder;
		this.cr = new Census2001SampleReader();
		cr.parse(this.inputFolder);
	}
	
	
	
	

}

