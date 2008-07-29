/* *********************************************************************** *
 * project: org.matsim.*
 * VertexIntervalls.java
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


package playground.dressler.Intervall.src.Intervalls;

import java.util.ArrayList;



/**
 * class representing a node in a Time Expanded Network
 * @author manuelschneider
 *
 */
public class VertexIntervalls {

//------------------------FIELDS----------------------------------//
	
	private ArrayList<VertexIntervall> _intervalls = null;
	//TODO more Efficient generic type

//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one VertexIntervall (0,Integer.MAX_VALUE) with Predecessor set to null
	 * and min dist to Integer.MAX_VALUE
	 */
	public VertexIntervalls(){
		VertexIntervall intervall = new VertexIntervall(0,Integer.MAX_VALUE);
		_intervalls = new ArrayList<VertexIntervall>();
		_intervalls.add(intervall);
	}

//------------------------SPLITTING--------------------------------//	
	
	/**
	 * Finds the VertexIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new VertexInterval after the original
	 * @param t time point to split at
	 * @return the new VertexIntervall for further modification
 	 */
	public VertexIntervall splitAt(int t){
		//TODO make use of getIndexAT(t)
		boolean found = false;
		VertexIntervall j =null;
		int index=-1;
		for (VertexIntervall i: _intervalls){
			if (i.contains(t)){
				found = true;
				j= i.splitAt(t);
				index = _intervalls.indexOf(i);
				break;
			}
		}
		if (found){
			_intervalls.add(index+1, j);
			return j;
		}
		else throw new IllegalArgumentException("there is no Intervall that can be split at "+t);
	}

//------------------------------PREDESESSORS-------------------------//	
	
	/**
	 * Gives the Predecessor of the node at time t in a shortest path
	 * @param t time
	 * @return prdecessor at t 
	 */
	public String GetPredAt(int t){
		return _intervalls.get(this.getIndexAt(t)).getPredecessor();
	}
	
	/**
	 * Gives the minimal distance of node at time t 
	 * @param t time 
	 * @return min dist at t
	 */
	public int GetMinDistAt(int t){
		return _intervalls.get(this.getIndexAt(t)).getDistAt(t);
	}
	
	

//------------------------------INTERVALL GETTER-----------------------//
	
	/**
	 * Finds the Position of the VertexIntervall containing t in the collection
	 * @param t time
	 * @return index of the VertexIntervall 
	 */
	public int getIndexAt(int t){
		for (VertexIntervall i: this._intervalls){
			if (i.contains(t)) return this._intervalls.indexOf(i);
		}
		throw new IllegalArgumentException("there is no Intervall containing"+t);
	}
	
	/**
	 * Geter for an VertexIntervall when the index is known
	 * @param index index ;-)
	 * @return VertexIntervall at the specified index
	 */
	public VertexIntervall getVertexIntervallWithIndex(int index){
		return this._intervalls.get(index);
	}
	
	/**
	 * Finds the VertexIntervall containing t in the collection
	 * @param t time
	 * @return  VertexIntervall  containing t
	 */
	public VertexIntervall getIntervallAt(int t){
		for (VertexIntervall i: this._intervalls){
			if (i.contains(t)) return i;
		}
		throw new IllegalArgumentException("there is no Intervall containing"+t);
	}
	
	public String toString(){
		String l,r;
		switch (Intervall.getMode()){
		case 1: 
			l="[";
			r="]";
			break;
		case 2:
			l="(";
			r="]";
			break;
		case 3:
			l="[";
			r=")";
			break;
		case 4:
			l="(";
			r=")";
			break;
		default: 
			l="|";
			r="|";
			break;
		}
		StringBuilder str = new StringBuilder();
		for (VertexIntervall i: this._intervalls){
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" d:"+i.getDist() +" p:"+i.getPredecessor()+"\n");
		}
		return str.toString();
	}
	
	public VertexIntervall getLast(){
		return VertexIntervall.maxRight(this._intervalls); 
	}
	
//------------------------MAIN METHOD--------------------------------//
	
	/**
	 * main method for testing
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
