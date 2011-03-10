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

package playground.anhorni.scenarios;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ConfigReader {
	
	private String configFile = "src/main/java/playground/anhorni/input/PLOC/3towns/config.txt";
	
	private int populationSize = -1;
	private int numberOfCityShoppingLocs = -1;
	private double shopShare = -1;	
	private int numberOfPersonStrata = -1;	// excluding destination choice
	//expenditure for home towns
	private double [] mu;
	private double [] sigma;
	private int numberOfRandomRuns = -1;
	private String path = "";
	private int numberOfAnalyses = -1;
	private String runId ="";
	private boolean temporalVar;
	       
    public void read() {
    	
        try {
          BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
          
          String line = bufferedReader.readLine();
          String parts[] = line.split("\t");
          this.runId = parts[1];
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.populationSize = Integer.parseInt(parts[1]);
                             
          mu = new double[2];
          sigma = new double[2];
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          for (int i = 0; i < 2; i++) {
        	  this.mu[i] = Double.parseDouble(parts[i + 1]);
          }
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          for (int i = 0; i < 2; i++) {
        	  this.sigma[i] = Double.parseDouble(parts[i + 1]);
          } 
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.numberOfRandomRuns = Integer.parseInt(parts[1]);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.path = parts[1];
         
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.numberOfAnalyses = Integer.parseInt(parts[1]);
          
          line = bufferedReader.readLine();
          parts = line.split("\t");
          this.temporalVar = Boolean.parseBoolean(parts[1]);
          
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
    }

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}

	public int getNumberOfCityShoppingLocs() {
		return numberOfCityShoppingLocs;
	}

	public void setNumberOfCityShoppingLocs(int numberOfCityShoppingLocs) {
		this.numberOfCityShoppingLocs = numberOfCityShoppingLocs;
	}

	public double getShopShare() {
		return shopShare;
	}

	public void setShopShare(double shopShare) {
		this.shopShare = shopShare;
	}

	public int getNumberOfPersonStrata() {
		return numberOfPersonStrata;
	}

	public void setNumberOfPersonStrata(int numberOfPersonStrata) {
		this.numberOfPersonStrata = numberOfPersonStrata;
	}

	public double[] getMu() {
		return mu;
	}

	public void setMu(double[] mu) {
		this.mu = mu;
	}

	public double[] getSigma() {
		return sigma;
	}

	public void setSigma(double[] sigma) {
		this.sigma = sigma;
	}

	public int getNumberOfRandomRuns() {
		return numberOfRandomRuns;
	}

	public void setNumberOfRandomRuns(int numberOfRandomRuns) {
		this.numberOfRandomRuns = numberOfRandomRuns;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getNumberOfAnalyses() {
		return numberOfAnalyses;
	}

	public void setNumberOfAnalyses(int numberOfAnalyses) {
		this.numberOfAnalyses = numberOfAnalyses;
	}

	public boolean isTemporalVar() {
		return temporalVar;
	}

	public void setTemporalVar(boolean temporalVar) {
		this.temporalVar = temporalVar;
	}
}
