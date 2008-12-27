/* *********************************************************************** *
 * project: org.matsim.*
 * GeneralConfig.java
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

package org.matsim.utils.vis.netvis.config;


/**
 * 
 * @author gunnar
 * 
 */
public class GeneralConfig extends ConfigModule {

    // -------------------- CONSTANTS --------------------

    // IDENTIFIERS

		private static final String MODULE_NAME = "general";

		private static final String NET_FILE = "netfile";

    // DEFAULT VALUES

//		private static final String DEFAULT_VERBOSE = "true";

    // -------------------- CONSTRUCTION --------------------

    public GeneralConfig(String fileName) {
        super(MODULE_NAME, fileName);
    }

    public GeneralConfig(boolean verbose, String netFileName) {
        super(MODULE_NAME);
        set(NET_FILE, netFileName);
    }
//
//    @Override
//    public boolean isComplete() {
//        return containsKey(NET_FILE);
//    }
//
//    // -------------------- CACHING --------------------
//
//    @Override
//    protected void cache(String name, String value) {
//    }
//
//    @Override
//    protected void completeCache() {
//    }

    // -------------------- CONTENT ACCESS --------------------

    public String getNetFileName() {
        return getPath(NET_FILE);
    }

}
