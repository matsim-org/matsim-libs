/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigModuleI.java
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
public interface ConfigModuleI {

    /**
     * xml element
     */
    public static final String CONFIG_ELEM = "config";

    /**
     * xml element
     */
    public static final String MODULE_ELEM = "module";

    /**
     * xml attribute
     */
    public static final String MODULE_NAME_ATTR = "name";
    
    public static final String MODULE_CLASS_ATTR = "class";

    /**
     * Returns this sub-configuration's name.
     * 
     * @return this sub-configuration's name
     */
    public String getName();

    /**
     * Returns this sub-configuration as an xml segment.
     * 
     * @param indentCnt
     * 
     * @return
     */
    public String asXmlSegment(int indentCnt);

}
