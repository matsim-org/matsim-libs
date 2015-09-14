package playground.vsp.congestion;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public final class AgentOnLinkInfo {
	public static class Builder {
		private Id<Person> personId;
		private Id<Link> linkId;
		private Double enterTime;
		private Double freeSpeedLeaveTime;
		public Builder setAgentId(Id<Person> personId) {
			this.personId = personId ; return this ;
		}
		public Builder setLinkId(Id<Link> linkId) {
			this.linkId = linkId ; return this ;
		}
		public Builder setEnterTime(double time) {
			this.enterTime = time ; return this ;
		}
		public Builder setFreeSpeedLeaveTime(double d) {
			this.freeSpeedLeaveTime = d ; return this ;
		}
		public final AgentOnLinkInfo build() {
			Assert.assertNotNull( personId );
			Assert.assertNotNull( linkId );
			Assert.assertNotNull( enterTime );
			Assert.assertNotNull( freeSpeedLeaveTime );
			return new AgentOnLinkInfo( personId, linkId, enterTime, freeSpeedLeaveTime ) ;
		}

	}

	private final  Id<Person> personId;
	private final Id<Link> linkId;
	private final Double enterTime;
	private final Double freeSpeedLeaveTime;


	public AgentOnLinkInfo(Id<Person> personId2, Id<Link> linkId, Double enterTime2, Double freeSpeedLeaveTime) {
		this.personId = personId2 ;
		this.linkId = linkId ;
		this.enterTime = enterTime2 ;
		this.freeSpeedLeaveTime = freeSpeedLeaveTime ;
	}

	public final Id<Person> getPersonId() {
		return personId;
	}

	public final Id<Link> getSetLinkId() {
		return linkId;
	}

	public final Double getEnterTime() {
		return enterTime;
	}

	public final Double getFreeSpeedLeaveTime() {
		return freeSpeedLeaveTime;
	}



}