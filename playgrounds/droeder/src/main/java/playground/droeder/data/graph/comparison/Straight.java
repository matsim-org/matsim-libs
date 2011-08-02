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
package playground.droeder.data.graph.comparison;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author droeder
 *
 */
public class Straight{
	
	private Coord start;
	private Coord end;

	public Straight(Coord one, Coord two){
		this.start = one;
		this.end = two;
	}
	
	public Straight(Tuple<Coord, Coord> t){
		this.start = t.getFirst();
		this.end = t.getSecond();
	}
	
	public Coord getStart(){
		return this.start;
	}
	
	public Coord getEnd(){
		return this.end;
	}
	public String toString(){
		return start.getX() + "\t" + start.getY() + "\t" + end.getX() + "\t" + end.getY();
	}
}

