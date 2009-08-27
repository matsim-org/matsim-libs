/* *********************************************************************** *
 * project: org.matsim.*
 * MyPermutation.java
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

package playground.jjoubert.Utilities;

import java.util.ArrayList;

import org.matsim.core.gbl.MatsimRandom;

public class MyPermutator {

	public MyPermutator() {
	}
	
	public ArrayList<Integer> permutate(int size){
		ArrayList<Integer> unused = new ArrayList<Integer>();
		for(int i = 0; i < size; i++){
			unused.add(Integer.valueOf(i+1));
		}
		ArrayList<Integer> solution = new ArrayList<Integer>();
		for(int j = 0; j < size-1; j++){
			int pos = MatsimRandom.getRandom().nextInt(unused.size());
			solution.add(unused.get(pos));
			unused.remove(pos);
		}
		solution.add(unused.get(0));		
		return solution;
	}

}