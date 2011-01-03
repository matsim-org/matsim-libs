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

package playground.anhorni.LEGO.miniscenario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

public class ConfigReader {
	
	private String configFile = "src/main/java/playground/anhorni/LEGO/miniscenario/input/config.txt";
	private String path = "";
	private double spacing = 0.0;
	private int sideLengt = 0;
	private int personsPerLocation = 0;
	private double linkCapacity = 0.0;
	
	private double varTastes = 0.0;
	private double scoreElementDistance = 0.0;	
	private double scoreElementTastes = 0.0;
	private double scoreElementFLoad = 0.0;
	private double scoreElementEpsilons = 0.0;
	private boolean gumbel = false;
	private double actScoreScale = 1.0;
	private double actScoreOffset = 0.0;
	private int analysisPopulationOffset = 0;
	private boolean linearDistanceUtility = true;	
 		
	private final static Logger log = Logger.getLogger(ConfigReader.class);
	       
    public void read() {
    	
        try {
          BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
          
          String line = bufferedReader.readLine();
          String parts[] = line.split("\t");
          this.path = parts[1];  
                   
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.spacing = Double.parseDouble(parts[1]);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.sideLengt = Integer.parseInt(parts[1]);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.personsPerLocation = Integer.parseInt(parts[1]);
          log.info("Persons per location: " + personsPerLocation);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.linkCapacity = Double.parseDouble(parts[1]);
          log.info("link capacity: " + linkCapacity);
                              
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.varTastes = Double.parseDouble(parts[1]);
          log.info("variance of distance tastes: " + varTastes);
                             
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.scoreElementDistance = Double.parseDouble(parts[1]);
          log.info("Score share distance: " + scoreElementDistance);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.scoreElementTastes = Double.parseDouble(parts[1]);
          log.info("Score share tastes: " + scoreElementTastes);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.scoreElementFLoad = Double.parseDouble(parts[1]);
          log.info("scoreElementFLoad: " + scoreElementFLoad);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.scoreElementEpsilons = Double.parseDouble(parts[1]);
          log.info("scoreElementEpsilons: " + scoreElementEpsilons);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.gumbel = Boolean.parseBoolean(parts[1]);
          log.info("Gumbel (or normal distribution): " + gumbel);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.actScoreScale = Double.parseDouble(parts[1]);
          log.info("actScoreScale: " + actScoreScale);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.actScoreOffset = Double.parseDouble(parts[1]);
          log.info("actScoreOffset: " + actScoreOffset);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.analysisPopulationOffset = Integer.parseInt(parts[1]);
          log.info("analysisPopulationOffset: " + analysisPopulationOffset);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.linearDistanceUtility = Boolean.parseBoolean(parts[1]);
          log.info("linearDistanceUtility: " + linearDistanceUtility);              
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
    }

	public String getPath() {
		return path;
	}

	public double getSpacing() {
		return spacing;
	}

	public int getSideLengt() {
		return sideLengt;
	}

	public int getPersonsPerLocation() {
		return personsPerLocation;
	}

	public double getLinkCapacity() {
		return linkCapacity;
	}

	public double getVarTastes() {
		return varTastes;
	}

	public double getScoreElementDistance() {
		return scoreElementDistance;
	}

	public double getScoreElementTastes() {
		return scoreElementTastes;
	}

	public double getScoreElementFLoad() {
		return scoreElementFLoad;
	}

	public double getScoreElementEpsilons() {
		return scoreElementEpsilons;
	}

	public boolean isGumbel() {
		return gumbel;
	}

	public double getActScoreScale() {
		return actScoreScale;
	}

	public double getActScoreOffset() {
		return actScoreOffset;
	}

	public int getAnalysisPopulationOffset() {
		return analysisPopulationOffset;
	}

	public boolean isLinearDistanceUtility() {
		return linearDistanceUtility;
	}
}
