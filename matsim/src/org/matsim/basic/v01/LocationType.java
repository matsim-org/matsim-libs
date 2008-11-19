package org.matsim.basic.v01;

/**
 * LocationType is a first step to reorganize the framework
 * towards the OGC specifications which foresee for each
 * feature (a matsim location is some special kind of 
 * feature) that it may have one to many feature types.
 * In matsim however this relation has to be 1:1 at 
 * the moment.
 * @author dgrether
 *
 */
public enum LocationType {LINK, FACILITY, ZONE}