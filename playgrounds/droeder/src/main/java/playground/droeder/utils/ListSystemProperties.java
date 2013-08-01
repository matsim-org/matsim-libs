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
package playground.droeder.utils;

import java.util.Enumeration;
import java.util.Properties;


/**
 * @author droeder
 *
 */
class ListSystemProperties {


	private ListSystemProperties() {

	}
	
	public static void main(String[] args) {
		Properties props = System.getProperties();
		Enumeration<?> names = props.propertyNames();
		int max = 0;
		while (names.hasMoreElements()) {
			String name = (String)names.nextElement();
			name.trim();
			if(name.length() > max){
				max = name.length();
			}
		}
		max += 2;
		names = props.propertyNames();
		while (names.hasMoreElements()) {
			String name = (String)names.nextElement();
			name.trim();
			System.out.print(name);
			for(int i = name.length(); i < max ; i++){
				System.out.print(" ");
			}
			System.out.println("= " +System.getProperty(name));
		}
	}
}

