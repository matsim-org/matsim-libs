package playground.vsp.congestion;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DelayInfo {

	public static class Builder {
		private Id<Person> personId;
		private Double linkEnterTime;
		private Double freeSpeedLeaveTime ;
		private Double linkLeaveTime;
		public Builder(){} // yy this could probably go away. kai, sep'15
		public Builder( AgentOnLinkInfo agentInfo ) {
			this.personId = agentInfo.getPersonId() ;
			this.linkEnterTime = agentInfo.getEnterTime() ;
			this.freeSpeedLeaveTime = agentInfo.getFreeSpeedLeaveTime() ;
		}
		public DelayInfo build() {
			Assert.assertNotNull( personId );
			Assert.assertNotNull( linkEnterTime );
			Assert.assertNotNull( freeSpeedLeaveTime );
			Assert.assertNotNull( linkLeaveTime );
			return new DelayInfo( personId, linkEnterTime, freeSpeedLeaveTime, linkLeaveTime ) ;
		}
		public Builder setPersonId( Id<Person> personId ) {
			this.personId = personId ;
			return this ;
		}
		public Builder setLinkEnterTime( Double val ) {
			this.linkEnterTime = val ;
			return this ;
		}
		public final Builder setFreeSpeedLeaveTime(Double freeSpeedLeaveTime) {
			this.freeSpeedLeaveTime = freeSpeedLeaveTime; 
			return this ;
		}
		public Builder setLinkLeaveTime(Double time) {
			this.linkLeaveTime = time ; return this ;
		}
	}

	// let's see what we need ...
	public final Id<Person> personId ;
	public final Double linkEnterTime ;
	public final Double freeSpeedLeaveTime ;
	public final Double linkLeaveTime ;

	private DelayInfo( Id<Person> personId, Double linkEnterTime, Double freeSpeedLeaveTime, Double linkLeaveTime ) {
		this.personId = personId ;
		this.linkEnterTime = linkEnterTime ;
		this.freeSpeedLeaveTime = freeSpeedLeaveTime ;
		this.linkLeaveTime = linkLeaveTime ;
	}
}