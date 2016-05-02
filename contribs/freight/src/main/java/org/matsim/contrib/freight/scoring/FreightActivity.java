package org.matsim.contrib.freight.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.facilities.ActivityFacility;

public class FreightActivity implements Activity {

	private Activity act;
	
	private TimeWindow timeWindow;
	
	public FreightActivity(Activity act, TimeWindow timeWindow) {
		super();
		this.act = act;
		this.timeWindow = timeWindow;
	}
	
	public TimeWindow getTimeWindow(){
		return timeWindow;
	}

	@Override
	public double getEndTime() {
		return act.getEndTime();
	}

	@Override
	public void setEndTime(double seconds) {
		act.setEndTime(seconds);
	}

	@Override
	public String getType() {
		return act.getType();
	}

	@Override
	public void setType(String type) {
		act.setType(type);
	}

	@Override
	public Coord getCoord() {
		return act.getCoord();
	}

	@Override
	public double getStartTime() {
		return act.getStartTime();
	}

	@Override
	public void setStartTime(double seconds) {
		act.setStartTime(seconds);
	}

	@Override
	public double getMaximumDuration() {
		return act.getMaximumDuration();
	}

	@Override
	public void setMaximumDuration(double seconds) {
		act.setMaximumDuration(seconds);
	}

	@Override
	public Id getLinkId() {
		return act.getLinkId();
	}

	@Override
	public Id getFacilityId() {
		return act.getFacilityId();
	}

	@Override
	public void setLinkId(Id<Link> id) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setFacilityId(Id<ActivityFacility> id) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
