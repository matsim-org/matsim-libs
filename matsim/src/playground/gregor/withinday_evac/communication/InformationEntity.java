/* *********************************************************************** *
 * project: org.matsim.*
 * InformationEntity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withinday_evac.communication;

import org.matsim.gbl.Gbl;

public class InformationEntity implements Comparable<InformationEntity> {
	
	
	public enum MSG_TYPE {
		FOLLOW_ME,
		MY_NEXT_LINK,
		LINK_BLOCKED,
		UNDEFIND;
	}
	
	private static final double DEFAULT_TTL = 10;
	private final double ttl;
	private final double initTime;
	private final double endTime;
	private final MSG_TYPE msgType;
	private final Message msg;
	private boolean resend = false;
	
	public InformationEntity(final double time, final MSG_TYPE msgType, final Message msg){
		this(DEFAULT_TTL/Gbl.getConfig().simulation().getFlowCapFactor(),time,msgType, msg);
	}
	
	public InformationEntity(final double ttl, final double time, final MSG_TYPE msgType, final Message msg) {
		this.ttl = ttl;
		this.initTime = time;
		this.endTime = time + ttl;
		this.msgType = msgType;
		this.msg = msg;
	}
	
	
	public boolean stillLiving(final double now) {
		if ((now - this.initTime) > this.ttl) {
			return false;
		}
		return true;
	} 
	
//	public double getTtl(double now) {
//		return this.ttl - (now-this.initTime);
//	}

	public double getEndTime() {
		return this.endTime;
	}
	
	public MSG_TYPE getMsgType() {
		return this.msgType;
	}
	
	public Message getMsg() {
		return this.msg;
	}

	
	
	public int compareTo(final InformationEntity o) {
		if (this.endTime > o.getEndTime()) {
			return 1;
		} else if (this.endTime < o.getEndTime()) {
			return -1;
		}
		return 0;
	}

	public void setResend(final boolean resend) {
		this.resend = resend;
	}

	public boolean isResend() {
		return this.resend;
	}


}
