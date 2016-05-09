/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.util.*;


public class CSVLineBuilder
{
    private List<String> line = new ArrayList<>();


    public CSVLineBuilder add(String e)
    {
        line.add(e);
        return this;
    }


    public CSVLineBuilder add(List<String> c)
    {
        line.addAll(c);
        return this;
    }


    public CSVLineBuilder add(String[] a)
    {
        line.addAll(Arrays.asList(a));
        return this;
    }
    
    
    public String[] build()
    {
        return line.toArray(new String[line.size()]);
    }
}
