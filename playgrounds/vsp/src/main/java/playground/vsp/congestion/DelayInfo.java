package playground.vsp.congestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DelayInfo {
	
	public static class Builder {
		private Id<Person> personId;
		private Double linkEnterTime;
		public Builder setPersonId( Id<Person> personId ) {
			this.personId = personId ;
			return this ;
		}
		public Builder setLinkEnterTime( Double val ) {
			this.linkEnterTime = val ;
			return this ;
		}
		public DelayInfo build() {
			return new DelayInfo( personId, linkEnterTime ) ;
		}
	}
	
	// let's see what we need ...
	public final Id<Person> personId ;
	public final double linkEnterTime ;

	private DelayInfo( Id<Person> personId, double linkEnterTime ) {
		this.personId = personId ;
		this.linkEnterTime = linkEnterTime ;
	}
}