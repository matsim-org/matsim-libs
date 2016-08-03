/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.multiModeCadyts;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;

/**
 * @author amit
 */

public class ModalLinkLookUp implements LookUpItemFromId<ModalLink> {

	@Override
	public ModalLink getItem( Id<ModalLink> id ) {
		String strs [] = id.toString().split(ModalLink.getModeLinkSplittor());
		return new ModalLink(strs[0], Id.createLinkId(strs[1]));
	}
}
