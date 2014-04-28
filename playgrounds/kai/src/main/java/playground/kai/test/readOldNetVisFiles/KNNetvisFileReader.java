/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.test.readOldNetVisFiles;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author nagel
 *
 */
class KNNetvisFileReader {

	private KNNetvisFileReader() {
	}

	public static void main(String[] args) {

			String filename = "/Users/nagel/bigfiles/demos/coopers/no-control/output/testWrite08-05-00.vis" ;
			
			File file = new File(filename);

			FileInputStream fis = null ;
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			
			DataInputStream reader = null ;
			reader = new DataInputStream(fis) ;
			
			try {
				for ( ;; ) {
					byte[] buffer = new byte[999] ;
					int len = reader.read(buffer) ;
					System.out.println( buffer );
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		
	}

}
