/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

final class InnerOuterCityScenarioCreator {


	public List<Id<Link>> getInnerCityLinks(){
		List<Id<Link>> innerCityLinks = new ArrayList<>();
		/*
		 * j(6,7)R, j(5,7), ..., j(3,7)[R|]
		 * .
		 * .
		 * .
		 * j(6,3), j(5,3), ..., j(3,3)
		 *
		 * i(3,6),i(4,6), ..., i(7,6)
		 * .
		 * .
		 * .
		 * i(3,3),i(4,3),...., i(7,3)
		 */

		for(int i=3;i<8;i++){
			for(int j=3;j<8;j++){
				innerCityLinks.add(Id.create("j("+i+","+j+")", Link.class));
				innerCityLinks.add(Id.create("j("+i+","+j+")R", Link.class));
				innerCityLinks.add(Id.create("i("+i+","+j+")R", Link.class));
				innerCityLinks.add(Id.create("i("+i+","+j+")", Link.class));
			}
		}
		return innerCityLinks;
	}

	public List<Id<Link>> getAccessLinksToInnerCity(){
		List<Id<Link>> accessLinks = new ArrayList<>();
		List<String> linkStrings = Arrays.asList("j(4,7)R","j(6,7)R","j(5,3)","j(3,3)",
				"i(3,4)","i(3,6)","i(7,3)R","i(7,5)R");
		for(String idString : linkStrings) accessLinks.add(Id.create(idString, Link.class));
		return accessLinks;
	}

}
