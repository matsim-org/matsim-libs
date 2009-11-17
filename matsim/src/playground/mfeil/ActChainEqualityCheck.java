/* *********************************************************************** *
 * project: org.matsim.*
 * ActChainEqualityCheck.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;


/**
 * Simple class that check whether two activity chains are identical. Returns true if so, and false otherwise. *
 * @author mfeil *
 */

public class ActChainEqualityCheck {

	public boolean checkEqualActChains (List<PlanElement> ac1, List<PlanElement> ac2){
		
		if (ac1.size()!=ac2.size()){
		
			return false;
		}
		else{
			ArrayList<String> acts1 = new ArrayList<String> ();
			ArrayList<String> acts2 = new ArrayList<String> ();
			for (int i = 0;i<ac1.size();i=i+2){
				acts1.add(((ActivityImpl)(ac1.get(i))).getType().toString());				
			}
			for (int i = 0;i<ac2.size();i=i+2){
				acts2.add(((ActivityImpl)(ac2.get(i))).getType().toString());				
			}		
			return (acts1.equals(acts2));
		}
	}	
	
	public boolean checkEqualActChainsModes (List<PlanElement> ac1, List<PlanElement> ac2){
		
		if (ac1.size()!=ac2.size()){
			
			return false;
		}
		else{
			ArrayList<String> actsmodes1 = new ArrayList<String> ();
			ArrayList<String> actsmodes2 = new ArrayList<String> ();
			for (int i = 0;i<ac1.size();i++){
				if (i%2==0)	actsmodes1.add(((ActivityImpl)(ac1.get(i))).getType().toString());		
				else actsmodes1.add(((LegImpl)(ac1.get(i))).getMode().toString());
			}
			for (int i = 0;i<ac2.size();i++){
				if (i%2==0) actsmodes2.add(((ActivityImpl)(ac2.get(i))).getType().toString());	
				else actsmodes2.add(((LegImpl)(ac2.get(i))).getMode().toString());
			}		
			return (actsmodes1.equals(actsmodes2));
		}
	}		
	
		public boolean checkEqualActChainsModesAccumulated (List<PlanElement> ac1, List<PlanElement> ac2){
		
		if (ac1.size()!=ac2.size()){
			
			return false;
		}
		else{
			ArrayList<String> actsmodes1 = new ArrayList<String> ();
			ArrayList<String> actsmodes2 = new ArrayList<String> ();
			for (int i = 0;i<ac1.size();i++){
				if (i%2==0)	actsmodes1.add(((ActivityImpl)(ac1.get(i))).getType().toString());		
				else actsmodes1.add(((LegImpl)(ac1.get(i))).getMode().toString());
			}
			java.util.Collections.sort(actsmodes1);
			for (int i = 0;i<ac2.size();i++){
				if (i%2==0) actsmodes2.add(((ActivityImpl)(ac2.get(i))).getType().toString());	
				else actsmodes2.add(((LegImpl)(ac2.get(i))).getMode().toString());
			}		
			java.util.Collections.sort(actsmodes2);
			return (actsmodes1.equals(actsmodes2));
		}
	}		
}	