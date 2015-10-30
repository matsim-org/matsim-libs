/* *********************************************************************** *
 * project: org.matsim.*
 * GainsAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

public class GainsAnalysis implements LinkLeaveEventHandler{
	
	public Map<Id,AgentInfo> infos = new HashMap<Id,AgentInfo>();
	private Strategy strategy;
	
	private final Id<Link> link = Id.create("car141205", Link.class);
	
	private enum Strategy {sp,ne,so};
	
	public static void main(String [] args) {
		String sp = "/Users/laemmel/devel/hhw_hybrid/output_NE/ITERS/it.0/0.events.xml.gz";
		String ne = "/Users/laemmel/devel/hhw_hybrid/output_NE/ITERS/it.100/100.events.xml.gz";
		String so = "/Users/laemmel/devel/hhw_hybrid/output_SO/ITERS/it.100/100.events.xml.gz";
		
		GainsAnalysis gains = new GainsAnalysis();
		EventsManagerImpl e = new EventsManagerImpl();
		e.addHandler(gains);
		gains.setStrategy(Strategy.sp);
		new EventsReaderXMLv1(e).parse(sp);
		gains.setStrategy(Strategy.ne);
		new EventsReaderXMLv1(e).parse(ne);
		gains.setStrategy(Strategy.so);
		new EventsReaderXMLv1(e).parse(so);
		
		AgentInfo ai = gains.getMax();
		System.out.println(ai);
	}
	
	
	private AgentInfo getMax() {
		
		
		Comp c = new Comp();
		List<AgentInfo> ais = new ArrayList<AgentInfo>(this.infos.values());
		Collections.sort(ais, c);
		return (ais.get(ais.size()-1));
	}


	private void setStrategy(Strategy sp) {
		this.strategy = sp;
		
	}


	public static class AgentInfo {
		double soTime = -1;
		double neTime = -1;
		double spTime = -1;
		public Id id;
		
		@Override
		public String toString() {
			return this.id + " sp:" + this.spTime + " ne:" + this.neTime + " so:" + this.soTime;
		}
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(this.link)) {
			if (this.strategy == Strategy.sp) {
				AgentInfo ai = new AgentInfo();
				ai.spTime = event.getTime();
				ai.id = event.getDriverId();
				this.infos.put(ai.id, ai);
			} else {
				AgentInfo ai = this.infos.get(event.getDriverId());
				if (ai == null) {
					return;
				}
				if (this.strategy == Strategy.ne) {
					ai.neTime = event.getTime();
				} else if (this.strategy == Strategy.so) {
					if (ai.neTime == -1){// || ai.neTime < event.getTime()) {
						this.infos.remove(event.getDriverId());
						return;
					}
					ai.soTime = event.getTime();
				}
			}
		}
		
	}

	
	private final class Comp implements Comparator<AgentInfo> {

		@Override
		public int compare(AgentInfo a0, AgentInfo a1) {
			
			if (a0.soTime == -1) {
				return -1;
			}
			if (a1.soTime == -1) {
				return 1;
			}
			
			double d01 = Math.abs(a0.neTime-a0.soTime);
			double d02 = Math.abs(a0.spTime-a0.soTime);
			double d03 = Math.abs(a0.neTime-a0.spTime);
			double d0 = Math.min(d01, Math.min(d02, d03));
			double d11 = Math.abs(a1.neTime-a1.soTime);
			double d12 = Math.abs(a1.spTime-a1.soTime);
			double d13 = Math.abs(a1.neTime-a1.spTime);
			double d1 = Math.min(d11, Math.min(d12, d13));
			
			if (d0 < d1) {
				return -1;
			} if (d0 > d1) {
				return 1;
			}
			return 0;
		}
		
	}
}
