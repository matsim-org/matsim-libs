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
package playground.agarwalamit.mixedTraffic.plots;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */
public class LeavingPersonInfo {
	
	public static class Builder {
		private Id<Person> personId;
		private Id<Link> linkId;
		private Double leaveTime;
		private String legMode;
		
		public Builder setAgentId(Id<Person> personId) {
			this.personId = personId ; return this ;
		}
		public Builder setLinkId(Id<Link> linkId) {
			this.linkId = linkId ; return this ;
		}
		public Builder setLeaveTime(double time) {
			this.leaveTime = time ; return this ;
		}
		public Builder setLegMode(String legMode) {
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

	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final Double leaveTime;
	private final String legMode;
	
	public LeavingPersonInfo(Id<Person> personId2, Id<Link> linkId, Double leaveTime2, String legMode) {
		this.personId = personId2 ;
		this.linkId = linkId ;
		this.leaveTime = leaveTime2 ;
		this.legMode = legMode;
	}

	public final Id<Person> getPersonId() {
		return this.personId;
	}

	public final Id<Link> getLinkId() {
		return this.linkId;
	}

	public final Double getLinkLeaveTime() {
		return this.leaveTime;
	}
	public String getLegMode() {
		return this.legMode ;
	}
}