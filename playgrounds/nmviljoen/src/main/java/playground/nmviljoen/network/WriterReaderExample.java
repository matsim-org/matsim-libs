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

/**
 * 
 */
package playground.nmviljoen.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author nmviljoen
 *
 */
public class WriterReaderExample {
	final private static Logger LOG = Logger.getLogger(WriterReaderExample.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		BufferedWriter bw = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Desktop/test.csv");
		try{
			bw.write("x,y,z,weight");
			
			bw.newLine();
			bw.write(String.format("%.3f,%03.3f,%d,%.4f\n", 1.23456, 12.1, 4, 456.123456));
			
			double d = 123.4;
			bw.write(String.valueOf(d));
			bw.write(",");
			Double dd = 1234.56789;
			bw.write(String.valueOf(dd));
			
			bw.write(",1,2");
			
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				LOG.error("Oops, couldn't close");
			}
		}

		LOG.info("Done writing...");
		
		LOG.info("Now reading...");
		BufferedReader br = IOUtils.getBufferedReader("/Users/nadiaviljoen/Desktop/test.csv");
		try{
			String line = br.readLine(); // When there is a header.
//			String line = null; // When there is no header.
			
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				LOG.info("  ==> " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannor read from file.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close reader.");
			}
		}

		LOG.info("DONE!!");
	}

}
