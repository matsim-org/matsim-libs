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

package playground.thibautd.socnetsim.usage;

import org.matsim.core.controler.AbstractModule;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.usage.replanning.PlanLinkIdentifierUtils;

/**
 * @author thibautd
 */
public class ConfigConfiguredPlanLinkIdentifierModule extends AbstractModule {
    @Override
    public void install() {
        bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Strong.class ).toProvider( PlanLinkIdentifierUtils.LinkIdentifierProvider.class );
        bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Weak.class ).toProvider( PlanLinkIdentifierUtils.WeakLinkIdentifierProvider.class );
    }
}
