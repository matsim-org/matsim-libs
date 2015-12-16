/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package tutorial.programming.scenarioElement;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author nagel
 *
 */
public class MyScenarioElement {
	public static final String NAME = "myScenarioElement" ;
	
	private Collection<String> information = new ArrayList<>() ;

	public void addInformation(String string) {
		information.add( string ) ;
	}
	
	public Collection<String> retrieveInformation() {
		return information ;
	}
}
