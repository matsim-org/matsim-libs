/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.replanning;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @author thibautd
 */
public class BlackListedTimeAllocationMutatorConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "blackListedTimeAllocationMutator";

	private Set<String> blackList = Collections.EMPTY_SET;

	public BlackListedTimeAllocationMutatorConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter( "blackList" )
	private String getBlackListString() {
		return CollectionUtils.setToString( getBlackList() );
	}

	@StringSetter( "blackList" )
	private void setBlackListString( String blackList ) {
		setBlackList( CollectionUtils.stringToSet( blackList ) );
	}

	public Set<String> getBlackList() {
		return blackList;
	}

	public void setBlackList( Set<String> blackList ) {
		this.blackList = blackList;
	}
}
