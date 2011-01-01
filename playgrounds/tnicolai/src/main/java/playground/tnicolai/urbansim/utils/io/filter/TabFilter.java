/* *********************************************************************** *
 * project: org.matsim.*
 * TabFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.tnicolai.urbansim.utils.io.filter;

import java.io.File;
import java.io.FileFilter;

/**
 * @author thomas
 *
 */
public class TabFilter implements FileFilter{
	
	private final String tab = "tab";
	
	public boolean accept(File file){
		if(file.getName().toLowerCase().endsWith(tab))
			return true;
		
		return false;
	}

}

