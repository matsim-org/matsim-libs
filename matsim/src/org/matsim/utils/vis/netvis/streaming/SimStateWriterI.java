/* *********************************************************************** *
 * project: org.matsim.*
 * SimStateWriterI.java
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

package org.matsim.utils.vis.netvis.streaming;

import java.io.IOException;

public interface SimStateWriterI {

    /**
     * Prepares the writer for subsequent calls to <code>dump(int)</code>.
     */
    public void open();

    /**
     * Asks the writer to dump the current state to an appropriate file.
     * 
     * @return <code>true</code> if the writer actually wrote something and
     *         <code>false</code> otherwise
     */
    public boolean dump(int time_s) throws IOException;

    /**
     * Closes the underlying output stream and generates a configuration file.
     * Has to be called explicitly by the client after all data has been
     * written.
     */
    public void close() throws IOException;

}