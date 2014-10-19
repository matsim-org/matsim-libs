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

package playground.southafrica.gauteng.roadpricingscheme;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

final class NoDiscountLinks {
	private NoDiscountLinks(){} // do not instantiate
		
	static List<Id<Link>> getList(){
		List<Id<Link>> list = new ArrayList<>();
		list.add(Id.create(120271, Link.class));
		list.add(Id.create(230322, Link.class));
		list.add(Id.create(232752, Link.class));
		list.add(Id.create( 20619, Link.class));
		list.add(Id.create( 81177, Link.class));
		list.add(Id.create( 71194, Link.class));
		list.add(Id.create( 28021, Link.class));
		list.add(Id.create(183490, Link.class));
		list.add(Id.create(229189, Link.class));
		list.add(Id.create(229176, Link.class));
		list.add(Id.create( 85167, Link.class));
		list.add(Id.create( 85152, Link.class));
		list.add(Id.create(141531, Link.class));
		list.add(Id.create(128220, Link.class));
		list.add(Id.create(193949, Link.class));
		list.add(Id.create(  8297, Link.class));
		list.add(Id.create(208518, Link.class));
		list.add(Id.create( 34557, Link.class));
		list.add(Id.create( 61053, Link.class));
		list.add(Id.create( 74466, Link.class));
		list.add(Id.create(133161, Link.class));
		list.add(Id.create(136037, Link.class));
		list.add(Id.create(  2139, Link.class));
		list.add(Id.create(217635, Link.class));
		list.add(Id.create(155113, Link.class));
		list.add(Id.create(216065, Link.class));
		list.add(Id.create(196380, Link.class));
		list.add(Id.create(143278, Link.class));
		
		return list;
	}

}

