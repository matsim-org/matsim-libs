/* *********************************************************************** *
 * project: org.matsim.*
 * TextFileFilter.java
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

package playground.jjoubert.Utilities.FileSampler;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This class just implements the FilenameFilter interface. A file is accepted if it has 
 * the same file extension as specified in the constructor. 
 * @author johanwjoubert
 */
public class MyFileFilter implements FilenameFilter {
	private final String extension;
	
	/**
	 * Constructs an instance of the <code>FilenameFilter</code> interface.
	 * @param extension the file extension of the created files. <b><i>Note:</i></b> the 
	 * 		extension must include the '.' (full stop) character. For example, the 
	 * 		extansion should be specified as <code>".txt"</code> and not just 
	 * 		<code>"txt"</code>.
	 */
	public MyFileFilter(String extension) {
		super();
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

	public boolean accept(File directory, String filename) {
		int filepathLength = filename.length();
		int pos = filename.indexOf(".");
		String extention = filename.substring(pos, filepathLength);
		if(extention.equalsIgnoreCase(this.extension)){
			return true;
		} else{
			return false;
		}
	}

}
