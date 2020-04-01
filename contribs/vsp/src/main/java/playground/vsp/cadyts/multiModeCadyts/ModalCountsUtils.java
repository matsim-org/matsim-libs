/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.multiModeCadyts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 24.02.18.
 */

public class ModalCountsUtils {

    private static final String ID_SEPERATOR = "_&_";

    public static Id<ModalCountsLinkIdentifier> getModalCountLinkId(String mode, Id<Link> linkId
    ){
        return Id.create( mode.concat(ID_SEPERATOR).concat( String.valueOf(linkId) ), ModalCountsLinkIdentifier.class);
    }

}
