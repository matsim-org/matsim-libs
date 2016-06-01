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

package org.matsim.contrib.util;

import java.util.*;

import com.google.common.collect.Iterables;


public class CSVLineBuilder
{
    private final List<String> line = new ArrayList<>();


    public CSVLineBuilder add(String cell)
    {
        line.add(cell);
        return this;
    }


    public CSVLineBuilder addf(String format, Object cell)
    {
        line.add(String.format(format, cell));
        return this;
    }


    public CSVLineBuilder addEmpty()
    {
        line.add(null);
        return this;
    }


    public CSVLineBuilder addAll(Iterable<String> cells)
    {
        Iterables.addAll(line, cells);
        return this;
    }


    public CSVLineBuilder addAll(String... cells)
    {
        Collections.addAll(line, cells);
        return this;
    }


    public String[] build()
    {
        return line.toArray(new String[line.size()]);
    }
}
