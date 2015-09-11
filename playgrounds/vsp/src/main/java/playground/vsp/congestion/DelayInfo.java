package playground.vsp.congestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DelayInfo {

	public static class Builder {
		private Id<Person> personId;
		private Double linkEnterTime;
		private Double freeSpeedLeaveTime ;
		public DelayInfo build() {
			return new DelayInfo( personId, linkEnterTime, freeSpeedLeaveTime ) ;
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
	}

	// let's see what we need ...
	public final Id<Person> personId ;
	public final Double linkEnterTime ;
	public final Double freeSpeedLeaveTime ;

	private DelayInfo( Id<Person> personId, Double linkEnterTime, Double freeSpeedLeaveTime ) {
		this.personId = personId ;
		this.linkEnterTime = linkEnterTime ;
		this.freeSpeedLeaveTime = freeSpeedLeaveTime ;
	}
}