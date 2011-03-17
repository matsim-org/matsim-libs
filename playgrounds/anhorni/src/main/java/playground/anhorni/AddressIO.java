/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class AddressIO {	
	private String inFile = "src/main/java/playground/anhorni/input/addresses.csv";
	private String outFile = "src/main/java/playground/anhorni/input/insert_query.txt";
		
	private String prefix = "INSERT INTO login (un, pwd) VALUES ('";
	private String middlefix = "','";
	private String postfix = "');";
	
	public static void main (String argv []){
		AddressIO io = new AddressIO();	
		io.execute();
		System.out.println("finished");
	}
	       
    public void execute() { 	
        try {
          final BufferedReader in = new BufferedReader(new FileReader(inFile));
          final BufferedWriter out = IOUtils.getBufferedWriter(outFile);
          
          int cnt = 0;
          String curr_line = in.readLine(); // Skip header
          while ((curr_line = in.readLine()) != null) {	
	          String parts[] = curr_line.split(";");
	          String un = parts[2] + parts[0];  
	          String pw = parts[3];
	          
	          out.write(prefix + un + middlefix + pw + postfix + "\n");
	          out.flush();
	          cnt++;
          }
          out.close();
          in.close();
          
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
    }
}
