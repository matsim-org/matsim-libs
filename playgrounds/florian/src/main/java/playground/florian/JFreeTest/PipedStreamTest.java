/* *********************************************************************** *
 * project: org.matsim.*
 * TestClass
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
package playground.florian.JFreeTest;


import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class PipedStreamTest {

	
	public PipedStreamTest() throws IOException{

		//eventuell auch mit StringOutput/InputStream
		
		PipedInputStream inputStream = new PipedInputStream();
		
		PipedOutputStream outputstream = new PipedOutputStream(inputStream);

		//????
		inputStream.connect(outputstream);

		
		
//    XMLWriter writer2 = new XMLWriter(outputStream , format );
//
//    writer2.write(document);
//    
//    dataset = DatasetReader.readCategoryDatasetFromXML(inputStream);
//		
//		
		
		
		
		
		
		
		
		
	}
	
}
