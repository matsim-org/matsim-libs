/* *********************************************************************** *
 * project: org.matsim.*
 * AllrouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;

//import com.sleepycat.je.log.FileReader;

import playground.mmoyo.ptRouterAdapted.AdaptedLauncher;

import java.io.FileReader;

/**
 * routes a plan applying many different travel parameter values. Includes method to simulate all routed plans in a directory
 */
public class AllrouteTest {
	private final static Logger log = Logger.getLogger(AllrouteTest.class);
	
	final String configFile;
	final String outDir;
	
	//String constants inside the for loop
	final String STR_SEP = "_";
	final String STR_DASH = "/";
	final String STR_PNG = ".png";
	final String STR_ERRGRAPHFILE = "errorGraphErrorBiasOccupancy.png";
	final String STR_COUNTPATH = "ITERS/it.10/10.ptcountscompare.kmz";
	final String STR_CONTROLER ="controler";
	final String STR_OUTDIR ="outputDirectory";
	final String STR_PLANS ="plans";
	final String STR_INPLANS ="inputPlansFile";
	final String STR_READING ="\n\n  reading: ";
	final String STR_NOPT = "\n No pt-legs in plan";
	final String STR_CONFIG = "configFile.xml";
	final String strGraphDir; 
	private ScenarioImpl scn;
	
	public AllrouteTest(final String configFile){
		//load "template scenario" from config
		this.configFile = configFile;
		this.scn = new DataLoader().loadScenario(configFile);
		this.outDir = this.scn.getConfig().controler().getOutputDirectory();
	
		//create graphs dir where graphs will be stored
		this.strGraphDir = outDir + "graphs/";
		File graphDir = new File(this.strGraphDir);
		//graphDir.mkdir();
	} 
	
	private void routeAndSimulate()throws IOException{
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(this.configFile);

		double betaWalk;
		double betaDistance;
		double betaTransfer;
		
		for (betaWalk=1.0; betaWalk<= 10.0; betaWalk++){  
			for (betaDistance=0.0; betaDistance<= 1.5; betaDistance+=0.1){  //dist=0.0; dist<= 1.5; dist+=0.1
				for (betaTransfer=0.0; betaTransfer<= 1200.0; betaTransfer+=60.0){   //double transfer=0.0; transfer<= 1200.0; transfer+=60.0
					adaptedLauncher.set_betaWalk(betaWalk);
					adaptedLauncher.set_betaDistance(betaDistance);
					adaptedLauncher.set_betaTransfer(betaTransfer);
					String routedPlan = adaptedLauncher.route();
				
					String strCombination = (int)adaptedLauncher.get_betaWalk() + STR_SEP + Math.round(adaptedLauncher.get_betaDistance() *1)/1.0 + STR_SEP + (int)adaptedLauncher.get_betaTransfer(); 
					String tempOutDir = outDir + STR_DASH+ strCombination + STR_DASH;
					
					//set routed plan as input for config and write the new config
					this.scn.getConfig().setParam(STR_CONTROLER, STR_OUTDIR, tempOutDir);
					this.scn.getConfig().setParam(STR_PLANS, STR_INPLANS, routedPlan);

					//simulate
					log.info(STR_READING + strCombination);
					Controler controler = new Controler( this.scn ) ;
					controler.setCreateGraphs(false);
					controler.setOverwriteFiles(true);
					controler.run();
					
					//get error graph
					String kmzFile = tempOutDir + STR_COUNTPATH ;
					KMZ_Extractor kmzExtractor = new KMZ_Extractor(kmzFile, strGraphDir);
					kmzExtractor.extractFile(STR_ERRGRAPHFILE);
				
					//rename it with the combination name
					File file = new File(strGraphDir + STR_ERRGRAPHFILE);
					File file2 = new File(strGraphDir + strCombination + STR_PNG);
					if (!file.renameTo(file2)) {
						
					}
					
					//erase complete run folder
					File runFolder = new File(tempOutDir);
					IOUtils.deleteDirectory(runFolder);
				}
			}
		}
		
	}
	
	private boolean eraseDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean erased = eraseDir(new File(dir, children[i]));
				if (!erased) {
					return false;
				}
			}
		}
	    return dir.delete();
	}
	
	/**
	 * make simulations of population files that are in the same directory as the input plan. Only error graphs are kept at the end.
	 */
	private void simulatePlans(final int ini, final int end) throws IOException{
		//find out directory with all the routed plans
		File iniInPop = new File (this.scn.getConfig().plans().getInputFile());
		final String strPopsDir = iniInPop.getParent() + iniInPop.separator; 	
		File popsDir = new File(strPopsDir);

		DataLoader dataLoader = new DataLoader();
		final String STR_PTINTERACTION = "pt interaction";
		final String STR_ROUTEDPLAN = "routedPlan";
		final String STR_NOPOP = "This is not a routed population file: ";
		
		if (popsDir.isDirectory()) {
			String[] children = popsDir.list();	
			
			for (int i=ini; i<=end; i++) {
				String popFileName= children[i];
				String popFilePath= strPopsDir + popFileName;
				
				/////validate that this file starts with "routedPlan" and that it is a population file
				if (!popFileName.startsWith(STR_ROUTEDPLAN)){
					log.error(STR_NOPOP + popFilePath);
					continue;
				}
				MatsimFileTypeGuesser guesser = new MatsimFileTypeGuesser(popFilePath);
				if (guesser.getGuessedFileType()==null || !guesser.getGuessedFileType().equals(MatsimFileTypeGuesser.FileType.Population)){
					log.error(STR_NOPOP + popFilePath); 
					continue;
				}
				
				String strCombination = popFileName.substring(11, popFileName.length()-7);
				log.info(STR_READING + strCombination);
				Population pop = dataLoader.readPopulation(popFilePath);

				///validate that population file contains at least 1 pt leg! otherwise it is senseless to simulate it
				boolean hasPtInterac = false;
				Iterator <? extends Person> PersonIter = pop.getPersons().values().iterator();
				while (PersonIter.hasNext() && hasPtInterac==false) {
					Person person = PersonIter.next();
					for (PlanElement pe: person.getSelectedPlan().getPlanElements()){
						if (pe instanceof Activity) {
							Activity act = (Activity)pe;
							hasPtInterac = hasPtInterac || act.getType().equals(STR_PTINTERACTION);
						}
					}					
				}
				PersonIter = null;

				if(hasPtInterac){
					//modify config
					String tempOutDir = outDir + strCombination + STR_DASH;
					this.scn.getConfig().setParam(STR_CONTROLER, STR_OUTDIR, tempOutDir);
					this.scn.getConfig().setParam(STR_PLANS, STR_INPLANS, popFilePath);				
					
					//write temporary config file
					String tmpconfigFile = this.outDir + strCombination + STR_CONFIG ;
					ConfigWriter configWriter = new ConfigWriter(this.scn.getConfig());
					configWriter.write(tmpconfigFile); //maybe it would be a good idea to keep the config files?
					
					
					
					//simulate
					Controler controler = new Controler( tmpconfigFile ) ;
					controler.setCreateGraphs(false);
					controler.setOverwriteFiles(true);
					controler.run();
					
					/*
					//get error graph
					String kmzFile = tempOutDir + STR_COUNTPATH ;
					KMZ_Extractor kmzExtractor = new KMZ_Extractor(kmzFile, strGraphDir);
					kmzExtractor.extractFile(STR_ERRGRAPHFILE);
					//rename it with the combination name
					IOUtils.renameFile(strGraphDir + STR_ERRGRAPHFILE, strGraphDir + strCombination + STR_PNG);
					
					//write popFile
					//System.out.println("writing output plan file..." );
					//PopulationWriter popwriter = new PopulationWriter(controler.getPopulation(), this.scn.getNetwork());
					//popwriter.write(strGraphDir + strCombination + ".xml.gz") ;
					//System.out.println("done");
					
					//erase complete run folder
					File runFolder = new File(tempOutDir);
					eraseDir(runFolder);
					*/
				}else{
					log.warn(STR_NOPT);
				}
			}		
		}
	}
	
	private void simulateConfigs(final int ini, final int end) throws IOException{
		this.scn = null;
		
		File configsDir = new File(this.outDir + "configs");
		final String STR_CONF= "configFile.xml";
		
		if (configsDir.isDirectory()) {
			String[] children = configsDir.list();	
			
			for (int i=ini; i<=end; i++) {
				String configFileName= children[i];
				
				/////validate that this file ends with "configFile.xml" 
				if (!configFileName.endsWith(STR_CONF)){
					continue;
				}
				
				//simulate
				Controler controler = new Controler( this.outDir +  configFileName ) ;
				controler.setCreateGraphs(false);
				controler.setOverwriteFiles(true);
				controler.run();
			}		
		}
	}
	
	

	/**
	 * routes only a segment of all possible travel parameter combinations, to partition the complete work
	 */
	private void routeRange(int numPlans, int segment) {
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(this.configFile);

		double betaWalk;
		double betaDistance;
		double betaTransfer;
		
		int i=1;
		int ii=0;
		for (betaWalk=1.0; betaWalk<= 10.0; betaWalk++){  
			for (betaDistance=0.0; betaDistance<= 1.5; betaDistance+=0.1){  //dist=0.0; dist<= 1.5; dist+=0.1
				for (betaTransfer=0.0; betaTransfer<= 1200.0; betaTransfer+=60.0){   //double transfer=0.0; transfer<= 1200.0; transfer+=60.0
					if (i==1 || i%numPlans==0){
						ii++;
					}
					if (segment==ii){
						adaptedLauncher.set_betaWalk(betaWalk);
						adaptedLauncher.set_betaDistance(betaDistance);
						adaptedLauncher.set_betaTransfer(betaTransfer);
						log.info(adaptedLauncher.get_betaWalk() + " " + adaptedLauncher.get_betaDistance() + " " + adaptedLauncher.get_betaTransfer());
						String routedPlan = adaptedLauncher.route();						
					}
					i++;
				}
			}
			
		}
	}
	
	
	private void simConfigsInFile (final String configDirPath, final String missingRunsFile, int ini, int end){
		List<String> missRuns = new ArrayList<String>();
		try {
		    BufferedReader in = new BufferedReader(new FileReader(missingRunsFile));
		    String str;
		    while ((str = in.readLine()) != null) {
		    	missRuns.add(str);
		    }
		    in.close();
		} catch (IOException e) {
		}
		
		final String STR_CONF = "configFile.xml";
		int i=1;
		for (String s: missRuns){
			if (i>=ini && i<=end ){
				String configFile = configDirPath + s + STR_CONF;
				Controler controler = new Controler( configFile) ;
				controler.setCreateGraphs(false);
				controler.setOverwriteFiles(true);
				controler.run();
			}
			i++;
		}
		
	}
	
	
	public static void main(String[] args) throws IOException {
		String configFile; 

		if (args.length==1){
			configFile = args[0];
		}else if (args.length==3){
			configFile = args[0];
			AllrouteTest allrouteTest= new AllrouteTest(configFile);

			/*  route
			int numPlans = Integer.valueOf (args[1]);
			int segment = Integer.valueOf (args[2]);
			allrouteTest.routeRange(numPlans, segment);
			*/

			/*
			final int ini = Integer.valueOf (args[1]);
			final int end = Integer.valueOf (args[2]);
			allrouteTest.simulateConfigs(ini, end);
			*/

		}else if (args.length==5){
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			
			configFile = args[0];
			AllrouteTest allrouteTest= new AllrouteTest(configFile);
			
			final String configsDir = args[1];
			final String missingRuns = args[2];
			final int ini = Integer.valueOf (args[3]);
			final int end = Integer.valueOf (args[4]);
			
			allrouteTest.simConfigsInFile(configsDir, missingRuns, ini, end);
			
			/*
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			final int ini = 0;
			final int end = 3;
			AllrouteTest allrouteTest= new AllrouteTest(configFile);
			allrouteTest.simulatePlans(ini, end);
			*/

			/*
			int numPlans = 80;
			int segment = 40;
			AllrouteTest allrouteTest= new AllrouteTest(configFile);
			allrouteTest.routeRange(numPlans, segment);
			 */
			
			
		}

		/*
		if (!new File(configFile).exists()) {
			throw new FileNotFoundException(configFile);
		}
		*/
		
		//AllrouteTest allrouteTest= new AllrouteTest(configFile);
		//allrouteTest.routeAndSimulate();
		//allrouteTest.simulatePlans();
		
	}

}
