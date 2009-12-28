/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimModuleLoader.java
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

package playground.mrieser.modules;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

public abstract class MatsimModuleLoader {
	
	private final static Logger log = Logger.getLogger(MatsimModuleLoader.class);
	
	public static MatsimModule loadModule(final String classname) {
		try {
			Class<? extends MatsimModule> klas = (Class<? extends MatsimModule>) Class.forName(classname);
			Constructor<? extends MatsimModule> c = klas.getConstructor(new Class[0]);
			MatsimModule module = c.newInstance();
			log.info("Loaded MatsimModule: " + classname);
			return module;
		} catch(NoSuchMethodException e) {
			log.warn("The module must have a default constructor." );
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

}
