/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Class to write a {@link PathDependentNetwork} in different formats to file.
 * Current formats supported:
 * <ul>
 * 		<li> own node/arc format;
 * 		<li> node coordinates with in-, out- and total degree.
 * </ul>
 * TODO Formats that must still be completed:
 * <ul>
 * 		<li> GraphML;
 * 		<li> GXL.
 * </ul>
 * 
 * @author jwjoubert
 */
public class PathDependentNetworkWriter {
	private final Logger log = Logger.getLogger(PathDependentNetworkWriter.class);
	private final PathDependentNetwork network;

	
	public PathDependentNetworkWriter(PathDependentNetwork network) {
		this.network = network;
	}
	
	
	public void writeNetworkXml(String filename, boolean overwrite) throws IOException{
		if(!overwrite){
			File f = new File(filename);
			if(f.exists()){
				log.warn("File " + filename + " exists and may not be overwritten!!");
				throw new IOException("Cannot overwrite " + filename);
			}			
		}
		log.info("Writing network to " + filename);

		DigicorePathDependentNetworkWriter nw = new DigicorePathDependentNetworkWriter(this.network);
		nw.write(filename);
	}
	
	
	public void writeNetworkXml(String filename) throws IOException{
		writeNetworkXml(filename, false);
	}
	
}
