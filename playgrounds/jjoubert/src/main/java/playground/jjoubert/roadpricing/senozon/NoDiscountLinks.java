/* *********************************************************************** *
 * project: org.matsim.*
 * NoDiscountLinks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.senozon;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public abstract class NoDiscountLinks {
	public static List<Id> getList(){
		List<Id> list = new ArrayList<Id>();
		list.add(new IdImpl(120271));
		list.add(new IdImpl(230322));
		list.add(new IdImpl(232752));
		list.add(new IdImpl(20619));
		list.add(new IdImpl(81177));
		list.add(new IdImpl(71194));
		list.add(new IdImpl(28021));
		list.add(new IdImpl(183490));
		list.add(new IdImpl(229189));
		list.add(new IdImpl(229176));
		list.add(new IdImpl(85167));
		list.add(new IdImpl(85152));
		list.add(new IdImpl(141531));
		list.add(new IdImpl(128220));
		list.add(new IdImpl(193949));
		list.add(new IdImpl(8297));
		list.add(new IdImpl(208518));
		list.add(new IdImpl(34557));
		list.add(new IdImpl(61053));
		list.add(new IdImpl(74466));
		list.add(new IdImpl(133161));
		list.add(new IdImpl(136037));
		list.add(new IdImpl(2139));
		list.add(new IdImpl(217635));
		list.add(new IdImpl(155113));
		list.add(new IdImpl(216065));
		list.add(new IdImpl(196380));
		list.add(new IdImpl(143278));
		
		return list;
	}

}

