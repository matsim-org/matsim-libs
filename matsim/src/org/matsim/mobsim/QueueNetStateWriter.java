/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNetStateWriter.java
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

package org.matsim.mobsim;

import java.util.Collection;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.mobsim.QueueLink.AgentOnLink;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;
import org.matsim.utils.vis.netvis.VisConfig;

/**
 * Writes snapshots in the format used by {@link org.matsim.utils.vis.netvis.NetVis}
 *
 * @author david
 */
public class QueueNetStateWriter extends DisplayNetStateWriter {

	private static final boolean writeSpaceCap = true;

    @Override
    protected Collection<AgentOnLink> getAgentsOnLink(final BasicLinkI link) {
        QueueLink qlink = (QueueLink) link;
        return qlink.getDrawableCollection();
    }

    public QueueNetStateWriter(final BasicNetI network, final String networkFileName,
    		final VisConfig visConfig, final String filePrefix, final int timeStepLength_s, final int bufferSize) {
        super(network, networkFileName, visConfig, filePrefix, timeStepLength_s, bufferSize);
    }

    @Override
    protected double getLinkDisplValue(final BasicLinkI link, final int index) {
        QueueLink mylink = (QueueLink) link;
        double value = writeSpaceCap ? mylink.getDisplayableSpaceCapValue() : mylink.getDisplayableTimeCapValue();
        return value;
    }

    @Override
    protected String getLinkDisplLabel(final BasicLinkI link) {
        QueueLink mylink = (QueueLink) link;
        return mylink.getId().toString();
    }

}