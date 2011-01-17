/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
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

package playground.anhorni.PLOC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PLOCConfigReader {
	
	private String configFile = "src/main/java/playground/anhorni/input/PLOC/config.txt";
	
	private int numberOfRandomRuns = -1;
	private int numberOfAnalyses = -1;
	       
    public void read() {
    	
        try {
          BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
                  
          String line = bufferedReader.readLine();
          String parts[] = line.split("\t");
          this.numberOfRandomRuns = Integer.parseInt(parts[1]);
                   
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.numberOfAnalyses = Integer.parseInt(parts[1]);
          
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
    }

	public int getNumberOfRandomRuns() {
		return numberOfRandomRuns;
	}

	public void setNumberOfRandomRuns(int numberOfRandomRuns) {
		this.numberOfRandomRuns = numberOfRandomRuns;
	}

	public int getNumberOfAnalyses() {
		return numberOfAnalyses;
	}

	public void setNumberOfAnalyses(int numberOfAnalyses) {
		this.numberOfAnalyses = numberOfAnalyses;
	}
}
