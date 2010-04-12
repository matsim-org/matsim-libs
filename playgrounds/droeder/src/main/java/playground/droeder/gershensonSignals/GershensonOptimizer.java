/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.gershensonSignals;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DaPaths;
import playground.droeder.ValueComparator;

/**
 * @author droeder
 *
 */
public class GershensonOptimizer {
	
	private Solution init(){
		Solution s = new Solution();
		double temp =0;
		
		do{
			temp = Math.random();
		}while(temp<0.5);
		s.setCap(temp);
		s.setN((int)((Math.random()*490) + 10.0));
		s.setD((int)((Math.random()*55) + 45.0));
		do {
			s.setU((int)((Math.random()*75) + 5.0));
			s.setMaxRed((int)((Math.random()*170) + 30.0));
		}while (s.getU() > s.getMaxRed());
		return s;
	}
	
	private Map<Integer, Solution> getBest(Map<Integer, Solution> solutions, Integer firstBest){
		Map<Integer, Double> temp = new HashMap<Integer, Double>();
		ValueComparator vc = new ValueComparator(temp);
		TreeMap<Integer, Double> tempBest = new TreeMap<Integer, Double>(vc);
		Map<Integer, Solution> bests = new HashMap<Integer, Solution>();
		
		for(Entry<Integer, Solution> e:solutions.entrySet()){
			temp.put(e.getKey(), e.getValue().getTime());
		}
		tempBest.putAll(temp);
		for(int i = 0 ; i<firstBest; i++){
			bests.put(i, solutions.get(tempBest.lastKey()));
			tempBest.pollLastEntry();
		}
		return bests;
	}
	
	private Map<Integer, Solution> recombination(Map<Integer, Solution> bests, double randSize){
		Map<Integer, Solution> recombinated = new HashMap<Integer, Solution>();
		Solution s;
		Integer i = 0;
		Integer j = 0;
		do{
			System.out.println("new do-while");
			for (Solution s1: bests.values()){
				for (Solution s2: bests.values()){
					for (Solution s3: bests.values()){
						for (Solution s4: bests.values()){
							for (Solution s5: bests.values()){
								double rnd = Math.random();
								if(rnd < 0.2){
									s = new Solution(s1.getU(), s2.getN(), s3.getMaxRed(), s4.getCap(), s5.getD());
								}else if (rnd >0.2 && rnd <0.4){
									s = new Solution(s5.getU(), s1.getN(), s2.getMaxRed(), s3.getCap(), s4.getD());
								}else if (rnd >0.4 && rnd <0.6){
									s = new Solution(s4.getU(), s5.getN(), s1.getMaxRed(), s2.getCap(), s3.getD());
								}else if (rnd >0.6 && rnd <0.8){
									s = new Solution(s3.getU(), s4.getN(), s5.getMaxRed(), s1.getCap(), s2.getD());
								}else{
									s = new Solution(s2.getU(), s3.getN(), s4.getMaxRed(), s5.getCap(), s1.getD());
								}
								if (s.getU() < s.getMaxRed() && !(recombinated.containsValue(s)) && !(bests.containsValue(s)) ){
									recombinated.put(i, s);
									i = i+1;
								}
								j++;
								if(recombinated.size()==(randSize)){
									return recombinated;
								}else if(j>Math.pow(bests.size(),5)){
									for (int ii = recombinated.size(); ii >randSize; i++){
										recombinated.put(ii, new Solution());
									}
									return recombinated;
								}
							}
						}
					}
				}
			}
		}while(recombinated.size()<(randSize));
		return recombinated;
	}
	
	public void writeToTxt (Map <Integer, Solution> data, String fileName){
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			writer.write("time" +"\t" + "d" +"\t" + "u" +"\t" + "cap" +"\t" + "n" +"\t" + "maxRed");
			writer.newLine();
			for(Solution s : data.values()){
				writer.write(String.valueOf(s.getTime()));
				writer.write("\t");
				writer.write(String.valueOf(s.getD()));
				writer.write("\t");
				writer.write(String.valueOf(s.getU()));
				writer.write("\t");
				writer.write(String.valueOf(s.getCap()));
				writer.write("\t");
				writer.write(String.valueOf(s.getN()));
				writer.write("\t");
				writer.write(String.valueOf(s.getMaxRed()));
				writer.write("\t");
				writer.newLine();
			}
			writer.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	public static void main(String[] args) {
		GershensonOptimizer g = new GershensonOptimizer();
		Map<Integer, Solution> temp = new HashMap<Integer, Solution>();
		Map<Integer, Solution> best = new HashMap<Integer, Solution>();
		GershensonRunner runner;
		String folder = DaPaths.OUTPUT + "cottbus\\optimization2\\";
		final String scenario = "cottbus";
		int i;
		int b = 200;
		
		for (int ii = 0; ii<b; ii++){
			temp.put(ii, g.init());
		}
		for (Entry<Integer, Solution> e  : temp.entrySet()){
			runner = new GershensonRunner(e.getValue().getU(), e.getValue().getN(), e.getValue().getCap(), e.getValue().getD(), e.getValue().getMaxRed(), false, false);
			Gbl.reset();
			
			runner.runScenario(scenario);
			if(runner.getAvTT()>0){
				e.getValue().setTime(runner.getAvTT());
			}else{
				e.getValue().setTime(9999);
			}
		}
		g.writeToTxt(temp,  folder + "randomSeed.txt");
		best = g.getBest(temp,b/5);
		g.writeToTxt(best, folder + "bestRandom.txt");
		
		for (int n = 1 ; n < 1000; n++){
			temp.clear();
			temp = g.recombination(best, b/10);
			i = temp.size();
			for (int ii = i  ; ii < b-1; ii++){
				temp.put(ii, g.init());
			}
			System.out.println(temp.size());
			for (Entry<Integer, Solution> e  : temp.entrySet()){
				runner = new GershensonRunner(e.getValue().getU(), e.getValue().getN(), e.getValue().getCap(), e.getValue().getD(), e.getValue().getMaxRed(), false, false);
				Gbl.reset();
				runner.runScenario(scenario);
				if(runner.getAvTT()>0){
					e.getValue().setTime(runner.getAvTT());
				}else{
					e.getValue().setTime(9999);
				}
			}
			i=temp.size();
			for (Solution s : best.values()){
				temp.put(i, s);
				if (i==b)break;
				i++;
			}
			g.writeToTxt(temp, folder + "it" + String.valueOf(n) +".txt");
			best.clear();
			best = g.getBest(temp, b/5);
			g.writeToTxt(best, folder + "bestIt" + String.valueOf(n) +".txt");
		}
	}
}
class Solution{
	private int u;
	private int n;
	private int maxRed;
	private double cap;
	private double d;
	private double time;
	
	public Solution(){
		
	}
	public Solution(int u, int n, int maxRed, double cap, double d){
		this.u = u;
		this.n = n;
		this.maxRed = maxRed;
		this.cap = ((int)(cap*100.00))/100.00;
		this.d = d;
	}

	public int getU() {
		return u;
	}

	public void setU(int u) {
		this.u = u;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getMaxRed() {
		return maxRed;
	}

	public void setMaxRed(int maxRed) {
		this.maxRed = maxRed;
	}

	public double getCap() {
		return cap;
	}

	public void setCap(double cap) {
		this.cap = ((int)(cap*100.00))/100.00;
	}

	public double getD() {
		return d;
	}

	public void setD(double d) {
		this.d = d;
	}
	
	public void setTime(double time){
		this.time =  ((int)(time*100.00))/100.00;
	}
	
	public double getTime(){
		return this.time;
	}
	@Override
	public String toString(){
		String temp;
		temp = "time=" + time + " d=" + d + " cap=" + cap + " u=" + u + " n=" + n;
		return temp;
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof Solution)) return false;
		Solution s = (Solution) o;
		
		if (this.cap == s.getCap() && this.d == s.getD() && this.maxRed == s.getMaxRed() 
				&& this.n == s.getN() && this.u == s.getU()){
			return true;
		} else{
			return false;
		}
	}
	
}