/* *********************************************************************** *
 * project: org.matsim.*
 * TabularFileHandler.java
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

package org.matsim.core.utils.io.tabularFileParser;

/**
 * An implementation of this interface is expected by the
 * <code>TabularFileParser</code> for row-by-row handling of parsed files.
 *
 * @author gunnar
 *
 */
public interface TabularFileHandler {

    /**
     * Is called by the <code>TabularFileParser</code> whenever a row has been
     * parsed
     *
     * @param row
     *            a <code>String[]</code> representation of the parsed row's
     *            columns
     */
    public void startRow(String[] row);

}
