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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class SocialNetworkSamplingConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "socialNetworkSampler";


	public enum TreeType {KDTree, VPTree}
	private TreeType spatialTreeType = TreeType.KDTree;
	private boolean rebalanceKdTree = false;

	public SocialNetworkSamplingConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("rebalanceKdTree")
	public boolean doRebalanceKdTree() {
		return rebalanceKdTree;
	}

	@StringSetter("rebalanceKdTree")
	public void setRebalanceKdTree( final boolean rebalanceKdTree ) {
		this.rebalanceKdTree = rebalanceKdTree;
	}

	@StringGetter("spatialTreeType")
	public TreeType getSpatialTreeType() {
		return spatialTreeType;
	}

	@StringSetter("spatialTreeType")
	public void setSpatialTreeType( final TreeType spatialTreeType ) {
		this.spatialTreeType = spatialTreeType;
	}
}
