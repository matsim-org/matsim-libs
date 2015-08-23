/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data.file;

import org.xml.sax.Attributes;


public class ReaderUtils
{
    public static double getDouble(Attributes atts, String qName, double defaultValue)
    {
        String val = atts.getValue(qName);
        return val != null ? Double.parseDouble(val) : defaultValue;
    }


    public static int getInt(Attributes atts, String qName, int defaultValue)
    {
        String val = atts.getValue(qName);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }


    public static String getString(Attributes atts, String qName, String defaultValue)
    {
        String val = atts.getValue(qName);
        return val != null ? val : defaultValue;
    }
}
