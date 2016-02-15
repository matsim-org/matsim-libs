/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.analysis;

import java.io.File;

/**
 * @author jillenberger
 */
public class FileIOContext {

    private final String root;

    private String fullPath;

    public FileIOContext(String root) {
        this.root = root;
        this.fullPath = root;
        new File(fullPath).mkdirs();
    }

    public String getRoot() {
        return root;
    }

    public String getPath() {
        return fullPath;
    }

    public void append(String path) {
        this.fullPath = String.format("%s/%s", root, path);
        new File(fullPath).mkdirs();
    }


}
