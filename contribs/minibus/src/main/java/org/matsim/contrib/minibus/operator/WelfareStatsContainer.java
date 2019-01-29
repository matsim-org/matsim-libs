//package org.matsim.contrib.minibus.operator;
//
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.util.LinkedList;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.contrib.minibus.PConfigGroup;
//import org.matsim.core.controler.events.ScoringEvent;
//import org.matsim.core.utils.io.IOUtils;
//
//public class WelfareStatsContainer {
//	
//	private double userBenefits;
//	private double operatorRevenues;
//	private double operatorCosts;
//	
//	private final boolean welfareMaximization;
//	private final double earningsPerBoardingPassenger;
//	
//	private final PerPassengerSubsidy welfareAnalyzer;
//	
//	private boolean firstTime = true;
//	
//	public WelfareStatsContainer(PConfigGroup pConfig, PerPassengerSubsidy welfareAnalyzer){
//		
//		this.welfareMaximization = pConfig.getWelfareMaximization();
//		this.earningsPerBoardingPassenger = pConfig.getEarningsPerBoardingPassenger();
//		this.welfareAnalyzer = welfareAnalyzer;
//		
//	}
//	
//	public void run(ScoringEvent event, LinkedList<Operator> operators){
//		
//		this.userBenefits = 0.;
//		this.operatorRevenues = 0.;
//		this.operatorCosts = 0.;
//		
//		this.collectWelfareInformation(operators, event.getServices().getScenario().getPopulation());
//		this.writeResults(event);
//		
//	}
//	
//	private void collectWelfareInformation(LinkedList<Operator> operators, Population population){
//		
//		for(Person person : population.getPersons().values()){
//			
//			double score = person.getSelectedPlan().getScore();
//			this.userBenefits += score;
//			
//		}
//		
//		for(Operator operator : operators){
//			
//			if(this.welfareMaximization){
//				
//				double welfareCorrection = 0.;
//				double score = 0.;
//				
//				for(PPlan pplan : operator.getAllPlans()){
//					
//					score += pplan.getScore();
//					Id<PPlan> pplanId = Id.create(pplan.getLine().getId().toString() + "-" + pplan.getId().toString(), PPlan.class);
//					welfareCorrection += this.welfareAnalyzer.getWelfareCorrection(pplanId);
//					
//				}
//				
//				this.operatorCosts += welfareCorrection - score;
//				
//			} else{
//				
//				double score = 0.;
//				double earnings = 0.;
//				
//				for(PPlan pplan : operator.getAllPlans()){
//					
//					earnings += pplan.getTripsServed() * this.earningsPerBoardingPassenger;
//					score += pplan.getScore();
//					
//				}
//				
//				this.operatorRevenues += earnings;
//				this.operatorCosts += earnings - score;
//				
//			}
//			
//		}
//		
//	}
//	
//	private void writeResults(ScoringEvent event){
//		
//		BufferedWriter writer;
//		
//		if(this.firstTime){
//			
//			 writer = IOUtils.getBufferedWriter(event.getServices().getControlerIO().getOutputPath() + "/welfareStats.txt");
//			
//		} else{
//			
//			writer = IOUtils.getAppendingBufferedWriter(event.getServices().getControlerIO().getOutputPath() + "/welfareStats.txt");
//			
//		}
//		
//		try {
//		
//			if(this.firstTime){
//				
//				writer.write("iteration\tuserBenefits\toperatorRevenues\toperatorCosts");
//				
//				this.firstTime = false;
//				
//			}
//			
//			writer.newLine();
//			writer.write(Integer.toString(event.getIteration()) + "\t" + Double.toString(this.userBenefits) + "\t" +
//						Double.toString(this.operatorRevenues) + "\t" + Double.toString(this.operatorCosts));
//			
//			writer.flush();
//			writer.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//	}
//
//}
