/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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

package org.matsim.interfaces.basic.v01.population;

import java.util.List;


/**
* @author dgrether
*/
public interface BasicPlan {

	public List<? extends BasicPlanElement> getPlanElements();

	public void addLeg(final BasicLeg leg);

	public void addAct(final BasicActivity act);

	public boolean isSelected();
	
	public void setSelected(boolean selected);

	public void setScore(Double score);
	
	public Double getScore();

}
