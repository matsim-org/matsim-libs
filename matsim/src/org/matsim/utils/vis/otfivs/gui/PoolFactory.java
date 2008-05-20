/* *********************************************************************** *
 * project: org.matsim.*
 * PoolFactory.java
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

package org.matsim.utils.vis.otfivs.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PoolFactory<ObjectType> {
	private static Map<Class, PoolFactory> allFactories = new HashMap<Class, PoolFactory>();
	
	private final ArrayList<ObjectType> array;
	private static final int initialSize = 10000;

	private final Class<ObjectType> classObject;
	private int usage = 0;
	
	private PoolFactory(Class c, int initialSize){
		this.array = new ArrayList<ObjectType> (initialSize);
		this.classObject = c;
	}

	public void reset() {
		usage= 0;
	}
	
	public void remove() {
		allFactories.remove(this.classObject);
	}
	
	public Class<ObjectType> getClientClass() {
		return classObject;
	}
	
	public ObjectType getOne(){
		ObjectType result = null;
		array.ensureCapacity(usage);
			try {
				if (usage >= array.size()) {
					result = classObject.newInstance();
					array.add(result);
					usage++;
				} else 	{
					result = array.get(usage);
					usage++;
				}
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Class object of pool factory could not be created!");
				throw new RuntimeException();
			} catch (IllegalAccessException e) {
				System.out.println("Class object of pool factory could not be created!");
				throw new RuntimeException();
			}
		return result;
	}
	
	public static PoolFactory get(Class c) {
		PoolFactory fac = allFactories.get(c);
		if (fac == null) {
			fac = new PoolFactory(c, initialSize);
			allFactories.put(c, fac);
		}
		return fac;
	}
	
	public static void resetAll() {
		for (PoolFactory fac : allFactories.values()) fac.reset();
	}
	
}
