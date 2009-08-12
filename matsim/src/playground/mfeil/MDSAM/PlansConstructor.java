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
import playground.mfeil.analysis.AnalysisSelectedPlansActivityChains;
import playground.mfeil.analysis.AnalysisSelectedPlansActivityChainsModes;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.gbl.Gbl;
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
import org.matsim.population.algorithms.XY2Links;
import org.matsim.api.basic.v01.Id;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategyModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.geometry.CoordUtils;



/**
 * @author Matthias Feil
 * Class that reads a file of plans and either varies them or assigns to an agent as alternatives the x most frequent other activity chains.
 */


public class PlansConstructor implements PlanStrategyModule{
		
	protected Controler controler;
	protected final String inputFile, outputFile, outputFileBiogeme, outputFileMod;
	protected PopulationImpl population;
	protected ArrayList<List<PlanElement>> actChains;
	protected NetworkLayer network;
	protected PlansCalcRoute router;
	protected XY2Links linker;
	protected List<List<Double>> sims;
	protected static final Logger log = Logger.getLogger(PlansConstructor.class);
	
	                      
	public PlansConstructor (Controler controler) {
		this.controler = controler;
	/*	this.inputFile = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";	
		this.outputFile = "/home/baug/mfeil/data/mz/output_plans.xml.gz";	
		this.outputFileBiogeme = "/home/baug/mfeil/data/mz/output_plans.dat";
		this.outputFileMod = "/home/baug/mfeil/data/mz/model.mod";
	*/	this.inputFile = "./plans/input_plans2.xml";	
		this.outputFile = "./plans/output_plans.xml.gz";	
		this.outputFileBiogeme = "./plans/output_plans.dat";
		this.outputFileMod = "./plans/model.mod";
		this.population = new PopulationImpl();
		this.network = controler.getNetwork();
		this.init(network);	
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.linker = new XY2Links (this.controler.getNetwork());
	}
	
	public PlansConstructor (PopulationImpl population, List<List<Double>> sims) {
	/*	this.inputFile = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";	
		this.outputFile = "/home/baug/mfeil/data/mz/output_plans.xml.gz";	
		this.outputFileBiogeme = "/home/baug/mfeil/data/mz/output_plans.dat";
		this.outputFileMod = "/home/baug/mfeil/data/mz/model.mod";
	*/	this.inputFile = "./plans/input_plans2.xml";	
		this.outputFile = "./plans/output_plans.xml.gz";	
		this.outputFileBiogeme = "./plans/output_plans.dat";
		this.outputFileMod = "./plans/model.mod";
		this.population = population;
		this.sims = sims;
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
		this.sims = new MDSAM(this.population).runPopulation();
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
		AnalysisSelectedPlansActivityChains analyzer = new AnalysisSelectedPlansActivityChainsModes(this.population);
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
			//this.router.run(person);
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
	/*
	private void getSimilarityOfPlans () {
		UniSAM sim = new UniSAM ();
		this.sims = new ArrayList<List<Double>>();
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			this.sims.add(new ArrayList<Double>());
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				if (plan.equals(person.getSelectedPlan())) {
					this.sims.get(this.sims.size()-1).add(0.0);
					continue;
				}
		/*		System.out.println("origPlan");
				for (int i=0;i<person.getSelectedPlan().getPlanElements().size();i+=2){
					System.out.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(i))).getType()+" ");
				}
				System.out.println();
				System.out.println("comparePlan");
				for (int i=0;i<plan.getPlanElements().size();i+=2){
					System.out.print(((ActivityImpl)(plan.getPlanElements().get(i))).getType()+" ");
				}
				System.out.println();
				this.sims.get(this.sims.size()-1).add(sim.run(person.getSelectedPlan(), plan));
			}
		}
	}*/
	
	private void writePlans(String outputFile){
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
		
}
