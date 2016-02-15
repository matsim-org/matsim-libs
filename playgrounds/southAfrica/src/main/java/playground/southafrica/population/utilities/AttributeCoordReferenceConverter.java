/* *********************************************************************** *
 * project: org.matsim.*
 * AttributeCoordReferenceConverter.java                                                                        *
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
/**
 * 
 */
package playground.southafrica.population.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

/**
 * Earlier when the populations were generated for South Africa's National
 * Treasury, the reference to MATSim's {@link Coord} was
 * <code>org.matsim.core.utils.geometry.CoordImpl</code> and this was used in 
 * the household attributes. Since then, however, it has changed to the current
 * <code>org.matsim.api.core.v01.Coord</code>. The original reference no longer
 * exists, and therefore the {@link ObjectAttributesXmlReader} cannot convert a
 * household's home location. A find-and-replace typically only works on small 
 * cities, and the larger metros we use this class.
 * 
 * @author jwjoubert
 */
public class AttributeCoordReferenceConverter {
	final private static Logger LOG = Logger.getLogger(AttributeCoordReferenceConverter.class);
	final private static String OLD = "core.utils.geometry.CoordImpl";
	final private static String NEW = "api.core.v01.Coord";

	/**
	 * Executes the reading of an XML-file line by line, and replacing the old
	 * reference to the new. Replaced lines are written to file as they're 
	 * completed.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AttributeCoordReferenceConverter.class.toString(), args);
		String input = args[0];
		String output = args[1];
		
		LOG.info("Converting household attributes to reference correct Coord class...");
		BufferedReader br = IOUtils.getBufferedReader(input);
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		
		Counter counter = new Counter("  lines # ");
		try{
			String line = null;
			while((line=br.readLine()) != null){
				String newLine = line.replaceFirst(OLD, NEW);
				bw.write(newLine);
				counter.incCounter();
			}			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read/write when converting household attributes.");
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close reader/writer.");
			}
		}
		counter.printCounter();
		LOG.info("Done converting household attributes.");
		
		Header.printFooter();
	}
}
