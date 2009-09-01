/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConstructor.java
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

package playground.mfeil.MDSAM;



import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.population.MatsimPopulationReader;
import playground.mfeil.ActChainEqualityCheck;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import playground.mfeil.analysis.AnalysisSelectedPlansActivityChainsModes;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
//import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategyModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacility;



/**
 * @author Matthias Feil
 * Class that reads a file of plans and either varies them or assigns to an agent as alternatives the x most frequent other activity chains.
 */


public class PlansConstructor implements PlanStrategyModule{
		
	protected Controler controler;
	protected final String inputFile, outputFile, outputFileBiogeme, outputFileMod, outputFileSims, outputFileSimsActs, outputFileSimsModes, outputFileSimsLocations;
	protected PopulationImpl population;
	protected ArrayList<List<PlanElement>> actChains;
	protected NetworkLayer network;
	protected PlansCalcRoute router;
	protected LocationMutatorwChoiceSet locator;
	protected MDSAM mdsam;
	protected XY2Links linker;
	protected List<List<Double>> sims;
	protected List<Double> simsActs;
	protected List<Double> simsLocations;
	protected List<Double> simsModes;
	protected static final Logger log = Logger.getLogger(PlansConstructor.class);
	
	                      
	public PlansConstructor (Controler controler) {
		this.controler = controler;
		this.inputFile = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";	
		this.outputFile = "/home/baug/mfeil/data/largeSet/it0/output_plans_mz03.xml.gz";	
		this.outputFileBiogeme = "/home/baug/mfeil/data/largeSet/it0/output_plans03.dat";
		this.outputFileMod = "/home/baug/mfeil/data/largeSet/it0/model03.mod";
		this.outputFileSims = "/home/baug/mfeil/data/largeSet/it0/sims03.xls";
		this.outputFileSimsActs = "/home/baug/mfeil/data/largeSet/it0/sims03acts.xls";
		this.outputFileSimsModes = "/home/baug/mfeil/data/largeSet/it0/sims03modes.xls";
		this.outputFileSimsLocations = "/home/baug/mfeil/data/largeSet/it0/sims03locations.xls";
	/*	this.inputFile = "./plans/input_plans2.xml";	
		this.outputFile = "./plans/output_plans.xml.gz";	
		this.outputFileBiogeme = "./plans/output_plans.dat";
		this.outputFileMod = "./plans/model.mod";
	*/	this.population = new PopulationImpl();
		this.network = controler.getNetwork();
		this.init(network);	
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, ((ScenarioImpl)controler.getScenarioData()).getKnowledges());
		this.linker = new XY2Links (this.controler.getNetwork());
	}
	
	public PlansConstructor (PopulationImpl population, List<List<Double>> sims) {
		this.inputFile = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";	
		this.outputFile = "/home/baug/mfeil/data/mz/output_plans.xml.gz";	
		this.outputFileBiogeme = "/home/baug/mfeil/data/mz/output_plans.dat";
		this.outputFileMod = "/home/baug/mfeil/data/mz/model.mod";
		this.outputFileSims = "/home/baug/mfeil/data/largeSet/it0/sims03.xls";
		this.outputFileSimsActs = "/home/baug/mfeil/data/largeSet/it0/sims03acts.xls";
		this.outputFileSimsModes = "/home/baug/mfeil/data/largeSet/it0/sims03modes.xls";
		this.outputFileSimsLocations = "/home/baug/mfeil/data/largeSet/it0/sims03locations.xls";
	/*	this.inputFile = "./plans/input_plans2.xml";	
		this.outputFile = "./plans/output_plans.xml.gz";	
		this.outputFileBiogeme = "./plans/output_plans.dat";
		this.outputFileMod = "./plans/model.mod";
	*/	this.population = population;
		this.sims = sims;
		this.simsActs = this.mdsam.getSimsActs();
		this.simsModes = this.mdsam.getSimsModes();
		this.simsLocations = this.mdsam.getSimsLocations();
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}
	
	public void prepareReplanning() {
		// Read the external plans file.
		new MatsimPopulationReader(this.population, this.controler.getNetwork()).readFile(this.inputFile);		
		log.info("Reading population done.");
	}

	public void handlePlan(final PlanImpl plan) {			
		// Do nothing here. We work only on the external plans.
	}

	public void finishReplanning(){
	//	this.selectZurich10MZPlans();
		this.reducePersons();
		this.linkRouteOrigPlans();
		this.enlargePlansSet();
		this.writePlans(this.outputFile);
		this.mdsam = new MDSAM(this.population);
		this.sims = this.mdsam.runPopulation();
		this.simsActs = this.mdsam.getSimsActs();
		this.simsModes = this.mdsam.getSimsModes();
		this.simsLocations = this.mdsam.getSimsLocations();
		this.writeSims(this.outputFileSims, this.outputFileSimsActs, this.outputFileSimsModes, this.outputFileSimsLocations);
		this.writePlansForBiogeme(this.outputFileBiogeme);
		this.writeModFile(this.outputFileMod);
	}
	
	
	// Method that filters only Zurich10% plans
	private void selectZurich10MZPlans (){
		log.info("Creating Zurich10% population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			boolean isIn = false;
			for (int j=0;j<person.getSelectedPlan().getPlanElements().size();j+=2){
				//30km circle around Zurich city centre (Bellevue)
				if (CoordUtils.calcDistance(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(j))).getCoord(), new CoordImpl(683518.0,246836.0))<=30000){
					isIn = true;
					break;
				}
			}
			if (!isIn){
				this.population.getPersons().remove(person.getId());
			}
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}
	
	
	private void reducePersons (){
		// Drop those persons whose plans do not belong to x most frequent activity chains.
		log.info("Analyzing activitiy chains...");
		AnalysisSelectedPlansActivityChainsModes analyzer = new AnalysisSelectedPlansActivityChainsModes(this.population);
		analyzer.run();
		ArrayList<List<PlanElement>> ac = analyzer.getActivityChains();
		ArrayList<ArrayList<PlanImpl>> pl = analyzer.getPlans();
		log.info("done.");
		List<Integer> ranking = new ArrayList<Integer>();
		for (int i=0;i<pl.size();i++){
			ranking.add(pl.get(i).size());
		}
		java.util.Collections.sort(ranking);
		this.actChains = new ArrayList<List<PlanElement>>();
		List<Id> agents = new LinkedList<Id>();
		for (int i=0;i<pl.size();i++){
			if (pl.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-51,0))){ //51
//			if (pl.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-2,0))){
				this.actChains.add(ac.get(i));
				for (Iterator<PlanImpl> iterator = pl.get(i).iterator(); iterator.hasNext();){
					PlanImpl plan = iterator.next();
					agents.add(plan.getPerson().getId());
				}
			}
		}
		log.info("Dropping persons from population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!agents.contains(person.getId())) this.population.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}
	
	private void linkRouteOrigPlans (){
		log.info("Adding links and routes to original plans...");
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			PlanImpl plan = person.getSelectedPlan();
			this.linker.run(plan);
			for (int j=1;j<plan.getPlanElements().size();j++){
				if (j%2==1){
					this.router.handleLeg((LegImpl)plan.getPlanElements().get(j), (ActivityImpl)plan.getPlanElements().get(j-1), (ActivityImpl)plan.getPlanElements().get(j+1), ((ActivityImpl)plan.getPlanElements().get(j-1)).getEndTime());
				}
				else {
					((ActivityImpl)(plan.getPlanElements().get(j))).setStartTime(((LegImpl)(plan.getPlanElements().get(j-1))).getArrivalTime());
					if (j!=plan.getPlanElements().size()-1){
						((ActivityImpl)(plan.getPlanElements().get(j))).setEndTime(java.lang.Math.max(((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime()+1, ((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()));
						((ActivityImpl)(plan.getPlanElements().get(j))).setDuration(((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime());
					}
				}
			}
		}
		log.info("done.");
	}
	
	private void enlargePlansSet (){
		log.info("Adding alternative plans...");
		int counter=0;
		ActChainEqualityCheck acCheck = new ActChainEqualityCheck();
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			counter++;
			if (counter%10==0) {
				log.info("Handled "+counter+" persons");
				Gbl.printMemoryUsage();
			}
			for (int i=0;i<this.actChains.size();i++){
				
				// Add all plans with activity chains different to the one of person's current plan
				if (!acCheck.checkEqualActChainsModes(person.getSelectedPlan().getPlanElements(), this.actChains.get(i))){
					PlanImpl plan = new PlanImpl (person);
			
					for (int j=0;j<this.actChains.get(i).size();j++){
						if (j%2==0) {
							ActivityImpl act = new ActivityImpl((ActivityImpl)this.actChains.get(i).get(j));
							if (/*j!=0 && */j!=this.actChains.get(i).size()-1) {
								act.setEndTime(MatsimRandom.getRandom().nextDouble()*act.getDuration()*2+act.getStartTime());
								if (j!=0 && !act.getType().equalsIgnoreCase("h")) {
									this.modifyLocationCoord(act);
								}
								else if (act.getType().equalsIgnoreCase("h")) {
									act.setCoord(person.getSelectedPlan().getFirstActivity().getCoord());
								}
							}
							plan.addActivity((BasicActivity)act);
						}
						else {
							LegImpl leg = new LegImpl((LegImpl)this.actChains.get(i).get(j));
							plan.addLeg((BasicLeg)leg);
						}
					}
					
					plan.getFirstActivity().setCoord(person.getSelectedPlan().getFirstActivity().getCoord());
					plan.getLastActivity().setCoord(person.getSelectedPlan().getLastActivity().getCoord());
					
					this.linker.run(plan);
					
					/* Analysis of subtours and random allocation of modes to subtours 
					PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
					planAnalyzeSubtours.run(plan);
					for (int j=0;j<planAnalyzeSubtours.getNumSubtours();j++){
						TransportMode[]	modes = TimeModeChoicerConfigGroup.getPossibleModes();
						TransportMode mode = modes[(int)(MatsimRandom.getRandom().nextDouble()*modes.length)];
						for (int k=1;k<plan.getPlanElements().size();k+=2){
							if (planAnalyzeSubtours.getSubtourIndexation()[(k-1)/2]==j){
								((LegImpl)plan.getPlanElements().get(k)).setMode(mode);
							}
						}
					}*/
	
					for (int j=1;j<plan.getPlanElements().size();j++){
						if (j%2==1){
							this.router.handleLeg((LegImpl)plan.getPlanElements().get(j), (ActivityImpl)plan.getPlanElements().get(j-1), (ActivityImpl)plan.getPlanElements().get(j+1), ((ActivityImpl)plan.getPlanElements().get(j-1)).getEndTime());
						}
						else {
							((ActivityImpl)(plan.getPlanElements().get(j))).setStartTime(((LegImpl)(plan.getPlanElements().get(j-1))).getArrivalTime());
							if (j!=plan.getPlanElements().size()-1){
								((ActivityImpl)(plan.getPlanElements().get(j))).setEndTime(java.lang.Math.max(((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime()+1, ((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()));
								((ActivityImpl)(plan.getPlanElements().get(j))).setDuration(((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime());
							}
						}
					}
					// if plan too long make it invalid (set score to -100000)
					if (plan.getLastActivity().getStartTime()-86400>plan.getFirstActivity().getEndTime()){
						plan.setScore(-100000.0);
					}
					person.getPlans().add(i, plan);
				}
			}
		}
		log.info("done.");
	}
	
	protected void modifyLocation (ActivityImpl act){
		log.info("Start modify.");
		ActivityFacilitiesImpl afImpl = (ActivityFacilitiesImpl) this.controler.getFacilities();
		
		String actType = null;
		if (act.getType().equalsIgnoreCase("w")) actType = "work_sector2";
		else if (act.getType().equalsIgnoreCase("e")) actType = "education_higher";
		else if (act.getType().equalsIgnoreCase("s")) actType = "shop";
		else if (act.getType().equalsIgnoreCase("l")) actType = "leisure";
		else log.warn("Unerkannter act type: "+act.getType());
		
		List <ActivityFacility> facs = new ArrayList<ActivityFacility>(afImpl.getFacilitiesForActivityType(actType).values());
		ActivityFacility fac;
		do {
			int position = (int) (MatsimRandom.getRandom().nextDouble()*facs.size());
			fac = facs.get(position);
		} while (CoordUtils.calcDistance(fac.getCoord(), new CoordImpl(683518.0,246836.0))>30000);
		act.setCoord(fac.getCoord());
	}
	
	protected void modifyLocationCoord (ActivityImpl act){
		// circle around Zurich centre
		double X = 683518.0 - 30000 + java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*60000);
		double Y = 246836.0 - Math.sqrt(Math.abs(30000*30000-(683518-X)*(683518-X))) + java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*Math.sqrt(Math.abs(30000*30000-(683518-X)*(683518-X)))*2);
		act.setCoord(new CoordImpl(X, Y));
	}
	
	
	public void writePlans(String outputFile){
		log.info("Writing plans...");
		new PopulationWriter(this.population, outputFile).write();
		log.info("done.");
	}
	
	
	public void writePlansForBiogeme(String outputFile){
		log.info("Writing plans for Biogeme...");
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// First row
		stream.print("Id\tChoice\t");
		PersonImpl p = this.population.getPersons().values().iterator().next();
		for (int i = 0;i<p.getPlans().size();i++){
			int j=0;
			for (j =0;j<java.lang.Math.max(p.getPlans().get(i).getPlanElements().size()-1,1);j++){
				stream.print("x"+(i+1)+""+(j+1)+"\t");
			}
			stream.print("x"+(i+1)+""+(j+1)+"\t");
		}
		for (int i = 0;i<p.getPlans().size();i++){
			stream.print("av"+(i+1)+"\t");
		}
		stream.println();
		
		// Filling plans
		int counterOut = -1;
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			counterOut++;
			stream.print(person.getId()+"\t");
			int position = -1;
			for (int i=0;i<person.getPlans().size();i++){
				if (person.getPlans().get(i).equals(person.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			int counterIn = -1;
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				counterIn++;
				if (plan.getPlanElements().size()==1) stream.print("24\t");
				else stream.print((((ActivityImpl)(plan.getFirstActivity())).getEndTime()+86400-((ActivityImpl)(plan.getLastActivity())).getStartTime())/3600+"\t");
				for (int i=1;i<plan.getPlanElements().size()-1;i++){
					if (i%2==0) stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).calculateDuration()/3600+"\t");
					else stream.print(((LegImpl)(plan.getPlanElements().get(i))).getTravelTime()/3600+"\t");
				}
				stream.print(this.sims.get(counterOut).get(counterIn)+"\t");
			}
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				if (plan.getScore()!=null && plan.getScore()==-100000.0)	stream.print(0+"\t");
				else stream.print(1+"\t");
			}
			stream.println();
		}
		stream.close();
		log.info("done.");
	}
	
	private void writeModFile(String outputFile){
		new ModFileMaker (this.population, this.sims).write(this.outputFileMod);
	}
	
	public void writeSims (String outputFile, String outputFile1, String outputFile2, String outputFile3){
		log.info("Writing sims file...");
		
		int [] stats = new int [21];
		int [] stats1 = new int [21];
		int [] stats2 = new int [21];
		int [] stats3 = new int [21];
		for (int i=0;i<stats.length;i++){
			stats[i]=0;
			stats1[i]=0;
			stats2[i]=0;
			stats3[i]=0;
		}
		
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		PrintStream stream1;
		try {
			stream1 = new PrintStream (new File(outputFile1));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		PrintStream stream2;
		try {
			stream2 = new PrintStream (new File(outputFile1));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		PrintStream stream3;
		try {
			stream3 = new PrintStream (new File(outputFile1));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		
		for (int i=0;i<this.sims.size();i++){
			stream.print("alt"+(i+1)+"\t");
			stream1.print("alt"+(i+1)+"\t");
			stream2.print("alt"+(i+1)+"\t");
			stream3.print("alt"+(i+1)+"\t");
		}		
		stream.println();
		
		int counter1 = 0;
		int counter2 = 0;
		int counter3 = 0;
		
		for (int i=0;i<this.sims.size();i++){
			for (int j=0;j<this.sims.get(i).size();j++){
				stream.print(this.sims.get(i).get(j)+"\t");
				stats [this.sims.get(i).get(j).intValue()]++;
				if (this.sims.get(i).get(j)!=0){
					stream1.print(this.simsActs.get(counter1)+"\t");
					stream2.print(this.simsModes.get(counter2)+"\t");
					stream3.print(this.simsLocations.get(counter3)+"\t");
					stats1 [this.simsActs.get(counter1).intValue()]++;
					stats2 [this.simsModes.get(counter2).intValue()]++;
					stats3 [this.simsLocations.get(counter3).intValue()]++;
					counter1++;
					counter2++;
					counter3++;
				}
				else {
					stream1.print("0\t");
					stream2.print("0\t");
					stream3.print("0\t");
					stats1 [0]++;
					stats2 [0]++;
					stats3 [0]++;
				}
			}
			stream.println();
			stream1.println();
			stream2.println();
			stream3.println();
		}
		stream.println();
		stream1.println();
		stream2.println();
		stream3.println();
		
		for (int i=0;i<stats.length;i++){
			stream.println(i+"\t"+stats[i]);
			stream1.println(i+"\t"+stats1[i]);
			stream2.println(i+"\t"+stats2[i]);
			stream3.println(i+"\t"+stats3[i]);
		} 
		
		stream.close();
		stream1.close();
		stream2.close();
		stream3.close();
		log.info("done.");
	}
		
}
