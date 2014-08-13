/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CloneServiceImpl.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.clones;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

class CloneServiceImpl implements CloneService {

    @Override
    public Id resolveParentId(Id cloneId) {
        String id = cloneId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_") + 1);
        else
            originalId = id;
        return new IdImpl(originalId);
    }

}
