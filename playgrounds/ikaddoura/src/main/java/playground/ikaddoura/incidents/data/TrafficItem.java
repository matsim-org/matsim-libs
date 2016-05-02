/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.data;

/**
* @author ikaddoura
*/

public class TrafficItem {
	
	private String id;
	private double downloadTime;
	private String originalId;
	private String startDateTime;
	private String endDateTime;
	private String status;

	private TMCLocation origin = new TMCLocation();
	private TMCLocation to = new TMCLocation();
	private TMCAlert alert = new TMCAlert();
	
	public TrafficItem(Long downloadTime) {
		this.downloadTime = downloadTime;
	}
	public TMCLocation getOrigin() {
		return origin;
	}
	public void setOrigin(TMCLocation origin) {
		this.origin = origin;
	}
	public TMCLocation getTo() {
		return to;
	}
	public void setTo(TMCLocation to) {
		this.to = to;
	}
	public TMCAlert getTMCAlert() {
		return alert;
	}
	public void setTMCAlert(TMCAlert alert) {
		this.alert = alert;
	}
	public String getStartDateTime() {
		return startDateTime;
	}
	public void setStartDateTime(String startDateTime) {
		this.startDateTime = startDateTime;
	}
	public String getEndDateTime() {
		return endDateTime;
	}
	public void setEndDateTime(String endDateTime) {
		this.endDateTime = endDateTime;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getOriginalId() {
		return originalId;
	}
	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public double getDownloadTime() {
		return downloadTime;
	}
	@Override
	public String toString() {
		return "TrafficItem [id=" + id + ", originalId=" + originalId
				+ ", startDateTime=" + startDateTime + ", endDateTime=" + endDateTime + ", status=" + status + ", origin=" + origin
				+ ", to=" + to + ", alert=" + alert + "]";
	}
	public String toStringWithDownloadTime() {
		return "TrafficItem [id=" + id + ", downloadTime=" + downloadTime + ", originalId=" + originalId
				+ ", startTime=" + startDateTime + ", endTime=" + endDateTime + ", status=" + status + ", origin=" + origin
				+ ", to=" + to + ", alert=" + alert + "]";
	}
}

