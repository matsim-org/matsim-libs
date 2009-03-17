/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparisonStringWriter.java
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

package playground.dgrether.analysis;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;


/**
 * This Class is able to write a PlanComparison Object to a String.
 * @author dgrether
 *
 */
public class PlanComparisonStringWriter implements PlanComparisonWriter {
	/**
	 * A StringBuffer to concat the result.
	 */
	private StringBuffer _buffer;

	/**
	 * @see playground.dgrether.analysis.PlanComparisonWriter#write(playground.dgrether.analysis.PlanComparison)
	 */
	public void write(PlanComparison pc) {
		double score1, score2;
		String linesep = System.getProperty("line.separator");
		Coord coordinates;
		_buffer = new StringBuffer();
		_buffer.append("Id");
		_buffer.append("\t");
		_buffer.append("X-Coordinate");
		_buffer.append("\t");
		_buffer.append("Y-Coordinate");
		_buffer.append("\t");
		_buffer.append("Score first plan");
		_buffer.append("\t");
		_buffer.append("Score second plan");
		_buffer.append("\t");
		_buffer.append("Improvement");
		_buffer.append("\t");
		_buffer.append("Delta") ;
		_buffer.append(linesep);

		for (Id i : pc.getPersonIds()) {
			score1 = pc.getFirstScore(i);
			score2 = pc.getSecondScore(i);
			if ( !Double.isNaN(score1) && !Double.isNaN(score2) ) {
				coordinates = pc.getHomeLocation(i).getCoord();
				_buffer.append(i.toString());
				_buffer.append("\t");
				_buffer.append(coordinates.getX());
				_buffer.append("\t");
				_buffer.append(coordinates.getY());
				_buffer.append("\t");
				_buffer.append(score1);
				_buffer.append("\t");
				_buffer.append(score2);
				_buffer.append("\t");
				if (score1 < score2)
					_buffer.append("1");
				else if (score1 == score2)
					_buffer.append("0");
				else if ( Double.isNaN(score1) || Double.isNaN(score2) ) // NaN return false to all comparisons
					_buffer.append("0") ;
				else
					_buffer.append("-1");
				_buffer.append("\t") ;
				if ( Double.isNaN(score1) || Double.isNaN(score2) ) 
					_buffer.append("0") ;
				else
					_buffer.append(score2-score1) ;
				_buffer.append(" ");
				_buffer.append(linesep);
			}
		}

	}
	/**
	 *
	 * @return the resulting String
	 */
	public String getResult() {
		return _buffer.toString();
	}


}
