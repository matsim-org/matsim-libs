/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.qPositionPlots;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit after <code>AgentOnLinkInfo</code>
 */

public final class EnteringPersonInfo {
	
	private final  Id<Person> personId;
	private final 	Link link;
	private final Double enterTime;
	private final String legMode;
	
	public static class Builder {
		private Id<Person> personId;
		private Link link;
		private Double enterTime;
		private String legMode;
		
		public Builder setAgentId(final Id<Person> personId) {
			this.personId = personId ; return this ;
		}
		public Builder setLink(final Link link) {
			this.link = link ; return this ;
		}
		public Builder setEnterTime(final double time) {
			this.enterTime = time ; return this ;
		}
		public Builder setLegMode(final String legMode) {
			this.legMode = legMode ; return this ;
		}
		public final EnteringPersonInfo build() {
			Assert.assertNotNull( personId );
			Assert.assertNotNull( link );
			Assert.assertNotNull( enterTime );
			Assert.assertNotNull( legMode );
			return new EnteringPersonInfo( personId, link, enterTime, legMode ) ;
		}
	}

	public EnteringPersonInfo(final Id<Person> personId2, final Link link, final double enterTime2, final String legMode) {
		this.personId = personId2 ;
		this.link = link ;
		this.enterTime = enterTime2 ;
		this.legMode = legMode;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Link getLink() {
		return this.link;
	}

	public Double getLinkEnterTime() {
		return this.enterTime;
	}
	public String getLegMode() {
		return this.legMode ;
	}
}