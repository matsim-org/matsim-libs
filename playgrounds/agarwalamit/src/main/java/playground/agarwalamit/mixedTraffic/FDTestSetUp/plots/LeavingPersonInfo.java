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
package playground.agarwalamit.mixedTraffic.FDTestSetUp.plots;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit after <code>AgentOnLinkInfo</code>
 */
public class LeavingPersonInfo {
	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final Double leaveTime;
	private final String legMode;
	
	public static class Builder {
		private Id<Person> personId;
		private Id<Link> linkId;
		private Double leaveTime;
		private String legMode;
		
		public Builder setAgentId(final Id<Person> personId) {
			this.personId = personId ; return this ;
		}
		public Builder setLinkId(final Id<Link> linkId) {
			this.linkId = linkId ; return this ;
		}
		public Builder setLeaveTime(final double time) {
			this.leaveTime = time ; return this ;
		}
		public Builder setLegMode(final String legMode) {
			this.legMode = legMode ; return this ;
		}
		public final LeavingPersonInfo build() {
			Assert.assertNotNull( personId );
			Assert.assertNotNull( linkId );
			Assert.assertNotNull( leaveTime );
			Assert.assertNotNull( legMode );
			return new LeavingPersonInfo( personId, linkId, leaveTime, legMode ) ;
		}
	}

	public LeavingPersonInfo(final Id<Person> personId2, final Id<Link> linkId, final double leaveTime2, final String legMode) {
		this.personId = personId2 ;
		this.linkId = linkId ;
		this.leaveTime = leaveTime2 ;
		this.legMode = legMode;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public Double getLinkLeaveTime() {
		return this.leaveTime;
	}
	public String getLegMode() {
		return this.legMode ;
	}
}