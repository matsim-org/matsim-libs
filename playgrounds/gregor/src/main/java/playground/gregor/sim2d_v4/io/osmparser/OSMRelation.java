/* *********************************************************************** *
 * project: org.matsim.*
 * OSMRelation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.io.osmparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class OSMRelation implements OSMElement{

	private final Map<String,String> tags = new HashMap<String,String>();
	
	private final List<Member> members = new ArrayList<Member>();
	
	private final Id id;

	public OSMRelation(Id id) {
		this.id = id;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public void addTag(String key, String val) {
		this.tags.put(key, val);
		
	}

	@Override
	public Map<String, String> getTags() {
		return this.tags;
	}
	
	public void addMember(Member m) {
		this.members.add(m);
	}
	
	public List<Member> getMembers() {
		return this.members;
	}
	
	public static final class Member {
		
		private final String type;
		private final Id ref;
		private final String role;
		
		public Member(String type, Id ref, String role) {
			this.type = type;
			this.ref = ref;
			this.role = role;
		}
		
		public String getType() {
			return this.type;
		}
		
		public Id getRefId() {
			return this.ref;
		}
		
		public String getRole() {
			return this.role;
		}
	}
	
}
