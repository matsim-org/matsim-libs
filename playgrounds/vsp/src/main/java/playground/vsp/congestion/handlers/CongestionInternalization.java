package playground.vsp.congestion.handlers;

import org.matsim.api.core.v01.events.LinkLeaveEvent;

public interface CongestionInternalization {
	
	// This is the core method which can be implemented in different ways
	// in order to change the logic how to internalize delays.
	public void calculateCongestion(LinkLeaveEvent event);

	// the total delay calculated as 'link leave time minus freespeed leave time'
	public double getTotalDelay();
	
	// the total delay which is internalized, i.e. allocated to causing agents
	public double getTotalInternalizedDelay();
	
	// writes the basic information to a file
	public void writeCongestionStats(String fileName);
	
}
