/* *********************************************************************** *
 * project: org.matsim.*
 * RecyclingModule1.java
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

package playground.mfeil;


import org.matsim.population.Act;
import org.matsim.gbl.MatsimRandom;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.replanning.modules.StrategyModule;
import java.util.ArrayList;


/**
 * @author Matthias Feil
 * Includes also the optimization of the distance coefficients
 */


public class RecyclingModule1 extends RecyclingModule implements StrategyModule{
		
	private final int iterations, noOfAgents, noOfCoefficients;
	private final DistanceCoefficients coefficients;
	private final MultithreadedModuleA assignmentModule;
	private final String primActsDistance, homeLocationDistance, sex, age, license, car_avail, employed; 
	private final ArrayList<String> coef; 
	                      
	public RecyclingModule1 (ControlerMFeil controler) {
		super(controler);
		this.iterations 		= 20;
		this.noOfAgents			= 10;
		this.primActsDistance = "yes";
		this.homeLocationDistance = "yes";
		this.sex = "no";
		this.age = "no";
		this.license = "no";
		this.car_avail = "no";
		this.employed = "no";
		this.coef = this.detectNoOfCoefficients();
		this.noOfCoefficients=this.coef.size();
		this.coefficients 		= new DistanceCoefficients (new double []{1,1}, this.coef);	
		this.assignmentModule	= new AgentsAssignmentInitialiser1 (controler, this.preProcessRoutingData, 
				this.locator, this.scorer, this.cleaner, this, 
				this.minimumTime, this.coefficients, this.nonassignedAgents);
		
	}
	
	

	public void finish(){
		
		for (int i=0;i<this.testAgentsNumber;i++) {
			int pos = (int)MatsimRandom.random.nextDouble()*this.list[1].size();
			list[0].add(list[1].get(pos));
			schedulingModule.handlePlan(list[1].get(pos));
			list[1].remove(pos);
		}
		schedulingModule.finish();
		
		for (int i=0;i<list[0].size();i++){
			assignment.print(list[0].get(i).getPerson().getId()+"\t\t"+list[0].get(i).getScore()+"\t");
			for (int j=0;j<list[0].get(i).getActsLegs().size();j+=2){
				assignment.print(((Act)(list[0].get(i).getActsLegs().get(j))).getType()+"\t");
			}
			assignment.println();
		}
		assignment.println();
		
		agents = new OptimizedAgents (list[0]);
		
		Statistics.prt=false;
		this.detectCoefficients();
		Statistics.prt=true;
		
		assignmentModule.init();
		
		for (int i=0;i<list[1].size();i++){
			assignmentModule.handlePlan(list[1].get(i));
		}
		assignmentModule.finish();
		for (int i=0;i<Statistics.list.size();i++){
			for (int j=0;j<Statistics.list.get(i).size();j++){
				assignment.print(Statistics.list.get(i).get(j)+"\t");
			}
			assignment.println();
		}
		Statistics.list.clear();
	}
	
	private void detectCoefficients (){
		
		/* Initialization */
		double offset = 0.5;
		double coefMatrix [][] = new double [this.iterations+1][this.noOfCoefficients];	// first solution is base solution, then 'this.iterations' iterations
		for (int i=0;i<coefMatrix[0].length;i++){
			double aux = this.coefficients.getSingleCoef(i);
			coefMatrix[0][i] = aux;
		}
		double score [] = new double [this.iterations+1];
		double scoreAux, coefAux;
		int [] modified = new int [this.noOfCoefficients-1]; // last coefficient fixed
		int modifiedAux;
		
		/* Iteration 0 */
		score [0]= this.calculate();
		for (int i=1;i<this.iterations+1;i++) score[i]=-100000;
	
		/* Further iterations */
		for (int i=1;i<this.iterations+1;i++){
			coefMatrix[i][coefMatrix[i].length-1]=coefMatrix[i-1][coefMatrix[i].length-1];
			for (int j=0;j<this.noOfCoefficients-1;j++){
				modifiedAux = modified[j];
				
				if (modifiedAux!=-1){
					coefMatrix [i][j] = coefMatrix [i-1][j]+offset;
					coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);					
					this.coefficients.setSingleCoef(coefMatrix[i][j], j);
					scoreAux = this.calculate();
					if (scoreAux>score[i]) {
						score[i] = scoreAux;
						modified [j] = 1; // 1 means "increased by offset";
						for (int x=0;x<j;x++){
							modified [x]=0;	// set back the former index;
							coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
						}
					}
					else {
						coefMatrix [i][j] = coefMatrix [i-1][j];
					}
				}
				
				if (modifiedAux!=1){
					coefAux = coefMatrix [i][j];	// keep coef solution;
					
					if (coefMatrix [i-1][j]-offset>=0){
						coefMatrix [i][j] = coefMatrix [i-1][j]-offset;
						coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);	
						this.coefficients.setSingleCoef(coefMatrix[i][j], j);
						scoreAux = this.calculate();
						if (scoreAux>score[i]) {
							score[i] = scoreAux;
							if (modified [j] == 1) modified [j] = -1; // -1 means "decreased by offset";
							else {
								for (int x=0;x<j;x++){
									modified [x]=0;	// set back to former index;
									coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
								}
								modified [j] = -1;
							}
						}
						else {
							coefMatrix [i][j] = coefAux; // set back to former value;
						}
					}
					else {
						coefMatrix [i][j] = coefMatrix [i-1][j]+2*offset;
						coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);	
						this.coefficients.setSingleCoef(coefMatrix[i][j], j);
						scoreAux = this.calculate();
						if (scoreAux>score[i]) {
							score[i] = scoreAux;
							if (modified [j] != 1) {
								for (int x=0;x<j;x++){
									modified [x]=0;	// set back to former index;
									coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
								}
								modified [j] = 1;
							}
						}
						else {
							coefMatrix [i][j] = coefAux; // set back to former value;
						}
					}
				}
			}
		}
		
		
		/*
		double offset = 0.5;
		double basis [] = new double [this.noOfCoefficients];
		for (int i=0;i<basis.length;i++){
			double aux = this.coefficients.getSingleCoef(i);
			basis[i] = aux;
		}
		double [][] score = new double [(this.noOfCoefficients-1)][4];	// last coefficient remains fixed; {coef+;coef-;score+;score-}
		double [][] tmp = new double [this.iterations][3]; //{value of coefficient, position of coefficient, score}
		ArrayList<double[]> tabuList = new ArrayList<double[]>();
		int best = -1;
		
		for (int i=0;i<this.iterations;i++){
			for (int j=0;j<this.noOfCoefficients-1;j++){
				if (best!=j*2+1){
					score [j][0]= basis[j]+offset;
					this.coefficients.setSingleCoef(score[j][0], j);
					if (this.checkTabuList(tabuList)) {
						score [j][2] = -100000;
						System.out.println("Tabu!");
					}
					else {
						score [j][2]= this.calculate();
						System.out.println(score [j][2]);
					}
				}
				if (best!=j*2){
					if (basis[j]-offset>=0) score [j][1]= basis[j]-offset;
					else score [j][1]= basis[j]+2*offset;
					this.coefficients.setSingleCoef(score[j][1], j);
					if (this.checkTabuList(tabuList)) {
						score [j][3] = -100000;
						System.out.println("Tabu!");
					}
					else {
						score [j][3]= this.calculate();
						System.out.println(score [j][3]);
					}
				}
			}
				
			tmp[i][2] = -100000;
			for (int j=0;j<score.length;j++){
				if (score[j][2]>tmp[i][2]){
					tmp[i][2] = score[j][2];
					tmp[i][0] = score[j][0];
					System.out.println("tabu: "+tmp[i][0]+", "+tmp[i][2] );
					tmp[i][1] = j;
					best=j*2;
				}
				if (score[j][3]>tmp[i][2]){
					tmp[i][2] = score[j][3];
					tmp[i][0] = score[j][1];
					tmp[i][1] = j;
					best=j*2+1;
				}
			}
			double [] tmpSol = new double [(this.noOfCoefficients-1)];
			for (int x=0;x<(this.noOfCoefficients-1);x++){
				double aux = this.coefficients.getSingleCoef(x);
				tmpSol[x]=aux;
			}
			tabuList.add(tmpSol);
			double aux = tmp[i][0];
			basis[(int)tmp[i][1]] = aux;
			System.out.println(aux);
		}
		*/
			/*
			score[0][1]=basis+(i+1)*offset;
			this.coefficients.setPrimActsDistance(score[0][1]);
			score[0][0] = this.calculate();
			
			if (basis-(i+1)*offset>=0) score[1][1]=basis-(i+1)*offset;
			else {
				score [1][1] = basis+((this.iterations)+inc)*offset;
				inc += 1;
			}
			this.coefficients.setPrimActsDistance(score[1][1]);
			score[1][0] = this.calculate();
			double tmpScore = -100000;
			double x=0;
			for (int j=0;j<score.length;j++){
				if (score[j][0]>tmpScore){
					tmpScore = score[j][0];
					x = score[j][1];
				}
			}
			tmp[i][0]=tmpScore;
			tmp[i][1]= x;
			*/
		for (int i=0;i<score.length;i++){
			for (int j=0;j<coefMatrix[0].length-1;j++){
				System.out.print(coefMatrix[i][j]+" ");
			}
			System.out.println(this.coefficients.getSingleCoef(coefMatrix[0].length-1)+" "+score[i]);
		}
		
		
		double tmpScoreFinal = -100000;
		for (int i=0;i<this.iterations+1;i++){
			if (score[i]>tmpScoreFinal){
				tmpScoreFinal = score[i];
				this.coefficients.setCoef(coefMatrix[i]);
				for (int j=0;j<this.noOfCoefficients;j++) System.out.println(coef.get(j)+" = "+this.coefficients.getCoef()[j]);
				System.out.println("Score = "+tmpScoreFinal);
				System.out.println();
			}
		}
	}
	
	private double calculate (){
		double score = 0;
		this.assignmentModule.init();
		for (int j=0;j<java.lang.Math.min(this.noOfAgents, list[1].size());j++){
			assignmentModule.handlePlan(list[1].get(j));
		}
		assignmentModule.finish();
		for (int j=0;j<java.lang.Math.min(this.noOfAgents, list[1].size());j++){
			score += this.list[1].get(j).getScore(); 
		}
		return score;
	}
	
	private ArrayList<String> detectNoOfCoefficients (){
		ArrayList<String> coef = new ArrayList<String>();
		if (this.primActsDistance=="yes") coef.add("primActsDistance");
		if (this.homeLocationDistance=="yes") coef.add("homeLocationDistance");
		if (this.sex=="yes") coef.add("sex");
		if (this.age=="yes") coef.add("age");
		if (this.license=="yes") coef.add("license");
		if (this.car_avail=="yes") coef.add("car_avail");
		if (this.employed=="yes") coef.add("employed");
		
		return coef;
	}
	
	private double checkTabuList (double [][] coefMatrix, int i, int j, double offset){
		OuterLoop:
		for (int x=0;x<i;x++){
			for (int y=0;y<coefMatrix[i].length;y++){
				System.out.println(coefMatrix[x][y]+" "+coefMatrix[i][y]);
				if (coefMatrix[x][y]!=coefMatrix[i][y]) continue OuterLoop;
			}
			System.out.println("Anschlag tabu!");
			double coefAux = -1;
			for (int z=0;z<i;z++){
				if (coefMatrix[z][j]>coefAux)coefAux=coefMatrix[z][j];
			}
			return coefAux+offset;
		}
		return coefMatrix[i][j];
	}

}
