/* *********************************************************************** *
 * project: org.matsim.*
 * ObjectFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.energy.validation;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * @author dgrether
 *
 */
@XmlRegistry
public class ObjectFactory {

	public ValidationInformation createValidationInformationList(){
		return new ValidationInformation();
	}
	
	public PoiInfo createPoiInfo(){
		return new PoiInfo();
	}
	
}
