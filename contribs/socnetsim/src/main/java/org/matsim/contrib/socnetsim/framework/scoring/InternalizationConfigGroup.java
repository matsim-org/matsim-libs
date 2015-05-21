/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.scoring;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

import java.util.Map;

/**
 * @author thibautd
 */
public class InternalizationConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "scoreInternalization";

    private String internalizationSocialNetworkFile = null;
    private double internalizationRatio = 0;

    public InternalizationConfigGroup() {
        super( GROUP_NAME );
    }

    @Override
    public Map<String,String> getComments() {
        final Map<String,String> map = super.getComments();
        map.put( "internalizationSocialNetworkFile" ,
                "path to the file defining the score internalization social network, if any. If null, the social network of the simulation will be used.");
        map.put( "internalizationRatio" ,
                "ratio by which to multiply the scores of social contacts before suming them with the ego's score. 0 means egoism, 1 average group score maximisation." );
        return map;
    }

    @StringGetter( "internalizationSocialNetworkFile" )
    public String getInternalizationSocialNetworkFile() {
        return internalizationSocialNetworkFile;
    }

    @StringSetter( "internalizationSocialNetworkFile" )
    public void setInternalizationSocialNetworkFile(final String internalizationSocialNetwork) {
        this.internalizationSocialNetworkFile = internalizationSocialNetwork;
    }

    @StringGetter( "internalizationRatio" )
    public double getInternalizationRatio() {
        return internalizationRatio;
    }

    @StringSetter( "internalizationRatio" )
    public void setInternalizationRatio(final double internalizationRatio) {
        this.internalizationRatio = internalizationRatio;
    }
}
