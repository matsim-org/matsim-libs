/* *********************************************************************** *
 * project: org.matsim.*
 * InstanceCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.misc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class InstanceCreator {

    // -------------------- ORDINARY OBJECT CREATION --------------------

    public static List newInstances(String[] classNames) {
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < classNames.length; i++)
            if (classNames[i].length() > 0) {
                Object newInstance = newInstance(classNames[i]);
                if (newInstance != null)
                    result.add(newInstance);
            }
        return result;
    }

    public static Object newInstance(String className) {
        Object newInstance = null;
        try {
            // if (Config.instance().getBool(Config.VERBOSE))
            // System.out.println("loading \"" + className + "\"");
            Class resultClass = Class.forName(className);
            newInstance = resultClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return newInstance;
    }

    // -------------------- SINGLETON CREATION --------------------

    public static Object singletonInstance(String className) {
        Object instance = null;
        try {
            // if (Config.instance().getBool(Config.VERBOSE))
            // System.out.println("loading \"" + className + "\" singleton");

            Class resultClass = Class.forName(className);
            Method createMethod = resultClass.getMethod("create", (Class[])null);
            instance = createMethod.invoke(null, (Object[])null);
            assert (instance != null);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return instance;
    }

}
