package playground.vsp.congestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public final class AgentOnLinkInfo {
	public static class Builder {
		private Id<Person> personId;
		private Id<Link> linkId;
		private Double enterTime;
		private Double freeSpeedLeaveTime;
		public void setAgentId(Id<Person> personId) {
			this.personId = personId ;
		}
		public void setLinkId(Id<Link> linkId) {
			this.linkId = linkId ;
		}
		public void setEnterTime(double time) {
			this.enterTime = time ;
		}
		public void setFreeSpeedLeaveTime(double d) {
			this.freeSpeedLeaveTime = d ;
		}
		public final AgentOnLinkInfo build() {
			return new AgentOnLinkInfo( personId, linkId, enterTime, freeSpeedLeaveTime ) ;
		}

	}

	private Id<Person> personId;
	private Id<Link> linkId;
	private Double enterTime;
	private Double freeSpeedLeaveTime;


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

	public final Double getSetFreeSpeedLeaveTime() {
		return freeSpeedLeaveTime;
	}



}