/* *********************************************************************** *
 * project: org.matsim.*
 * SNConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.jhackney.deprecated;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.population.Population;
import org.matsim.socialnetworks.interactions.NonSpatialInteractor;
import org.matsim.socialnetworks.interactions.SocializingOpportunity;
import org.matsim.socialnetworks.interactions.SpatialSocialOpportunityTracker;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;

public class SNConfig  {

//	---------------------- social network variables ---------------- //
//	-- these are here because they are set up in setup and needed again in iterations --//
	private static final String DIRECTORY_SN = "socialnets";
	public static String SOCNET_OUT_DIR = null;
	public static SocialNetwork snet;
	public static SocialNetworkStatistics snetstat;
	public static PajekWriter1 pjw;
	public static NonSpatialInteractor plansInteractorNS;//non-spatial (not observed, ICT)
	public static SpatialInteractor plansInteractorS;//spatial (face to face)
	static int max_sn_iter;
	static String [] infoToExchange;//type of info for non-spatial exchange is read in 
	public static String activityTypesForEncounters[]={"home","work","shop","education","leisure"};

	SpatialSocialOpportunityTracker gen2 = new SpatialSocialOpportunityTracker();
	Collection<SocializingOpportunity> socialEvents=null;

//	Variables for allocating the spatial meetings among different types of activities
	static double fractionS[];
	static HashMap<String,Double> rndEncounterProbs= new HashMap<String,Double>();
//	New variables for replanning
	static int replan_interval;
	
	private final static Logger log = Logger.getLogger(SNConfig.class);

//	-------------------- end social network variables --------------------//    
	
	public static void snsetup(Population plans){
		
		
	System.out.println("----------Initialization of social network -------------------------------------");

	Config config = Gbl.getConfig();

	max_sn_iter = Integer.parseInt(config.socnetmodule().getNumIterations());
	replan_interval = Integer.parseInt(config.socnetmodule().getRPInt());
	String rndEncounterProbString = config.socnetmodule().getFacWt();
	String interactorNSFacTypesString = config.socnetmodule().getXchange();
	infoToExchange = getFacTypes(interactorNSFacTypesString);
	fractionS = getActivityTypeAllocation(rndEncounterProbString);
	rndEncounterProbs = getActivityTypeAllocationMap(activityTypesForEncounters, rndEncounterProbString);

	// TODO Auto-generated method stub
	System.out.println(" Instantiating the Pajek writer ...");

	String outputPath =config.controler().getOutputDirectory(); 
	if (outputPath.endsWith("/")) {
		outputPath = outputPath.substring(0, outputPath.length()-1);
	}
	SOCNET_OUT_DIR = outputPath + "/"+DIRECTORY_SN;
	File snDir = new File(SOCNET_OUT_DIR);
	if (!snDir.mkdir() && !snDir.exists()) {
		Gbl.errorMsg("The iterations directory " + (outputPath + "/" + DIRECTORY_SN) + " could not be created.");
	}
	
	pjw = new PajekWriter1(SOCNET_OUT_DIR, null);
	System.out.println("... done");

	System.out.println(" Initializing the social network ...");
	snet = new SocialNetwork(plans);
	System.out.println("... done");

	System.out.println(" Calculating the statistics of the initial social network)...");
	snetstat=new SocialNetworkStatistics();
	snetstat.openFiles();
	snetstat.calculate(0, snet, plans);
	System.out.println(" ... done");

	System.out.println(" Writing out the initial social network ...");
	pjw.write(snet.getLinks(), plans, 0);
	System.out.println("... done");

	System.out.println(" Setting up the NonSpatial interactor ...");
	plansInteractorNS=new NonSpatialInteractor(snet);
	System.out.println("... done");

	System.out.println(" Setting up the Spatial interactor ...");
	plansInteractorS=new SpatialInteractor(snet);
	System.out.println("... done");
	}
	/**
	 * A method for decyphering the config codes. Part of configuration
	 * reader. Replace eventually with a routine that runs all of the
	 * facTypes but uses a probability for each one, summing to 1.0. Change
	 * the interactors accordingly.
	 * 
	 * @param longString
	 * @return
	 */
	private static String[] getFacTypes(String longString) {
		// TODO Auto-generated method stub
		String patternStr = ",";
		String[] s;
		log.info("!!add keyword\"any\" and a new interact method to exchange info of any factility types (compatible with probabilities)");
		if (longString.equals("all-p")) {
			s = new String[5];
			s[0] = "home";
			s[1] = "work";
			s[2] = "education";
			s[3] = "leisure";
			s[4] = "shop";
		} else if (longString.equals("all+p")) {
			s = new String[6];
			s[0] = "home";
			s[1] = "work";
			s[3] = "education";
			s[4] = "leisure";
			s[5] = "shop";
			s[6] = "person";
		} else {
			s = longString.split(patternStr);
		}
		for (int i = 0; i < s.length; i++) {
			// if(s[i]!="home"&&s[i]!="work"&&s[i]!="education"&&s[i]!="leisure"&&s[i]!="shop"&&s[i]!="person"&&s[i]!="none"){
			if (!s[i].equals("home") && !s[i].equals("work") && !s[i].equals("education") && !s[i].equals("leisure")
					&& !s[i].equals("shop") && !s[i].equals("person") && !s[i].equals("none")) {
				System.out.println(SNConfig.class + ":" + s[i]);
				Gbl.errorMsg("Error on type of info to exchange. Check config file. Use commas with no spaces");
			}
		}
		return s;
	}

	private static double[] getActivityTypeAllocation(String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if(w[i]<0.||w[i]>1.){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
	private static HashMap<String,Double> getActivityTypeAllocationMap(String[] types, String longString) {
		String patternStr = ",";
		String[] s;
		HashMap<String,Double> map = new HashMap<String,Double>();
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if(w[i]<0.||w[i]>1.){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
			map.put(types[i],w[i]);
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return map;
	}    
	
}
