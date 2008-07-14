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

package playground.gregor.withinday_evac.information;

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
	
	public InformationEntity(double time, MSG_TYPE msgType, Message msg){
		this(DEFAULT_TTL,time,msgType, msg);
	}
	
	public InformationEntity(double ttl, double time, MSG_TYPE msgType, Message msg) {
		this.ttl = ttl;
		this.initTime = time;
		this.endTime = time + ttl;
		this.msgType = msgType;
		this.msg = msg;
	}
	
	
	public boolean stillLiving(double now) {
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

	
	
	public int compareTo(InformationEntity o) {
		if (this.endTime > o.getEndTime()) {
			return 1;
		} else if (this.endTime < o.getEndTime()) {
			return -1;
		}
		return 0;
	}


}
