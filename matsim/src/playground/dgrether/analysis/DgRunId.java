/* *********************************************************************** *
 * project: org.matsim.*
 * DgRunId
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
package playground.dgrether.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


/**
 * @author dgrether
 *
 */
public class DgRunId {

	private String runNumber;

	public DgRunId(String n) {
		this.runNumber = n;
	}
	
  @Override
	public String toString(){
  	return this.runNumber;
  }

  public String toDotString(){
  	return this.runNumber + ".";
  }
  
  
  public Id toId(){
  	return new IdImpl(this.runNumber);
  }

}
