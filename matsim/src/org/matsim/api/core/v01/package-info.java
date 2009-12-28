/**
 * The purpose of this package is to provide "external" programmers a stable way to access matsim.  The
 * idea is that matsim calls some external module as something like
 * 
 * ExternalModule extMod = new ExternalModule( Scenario sc ) ;
 * ...
 * extMod.run() ;
 * 
 * and then the external module can retrieve all relevant information from this without having to re-parse
 * the input files.
 * 
 * In consequence, the information provided by the basic interfaces is what is in the xml files (and potentially
 * even less).
 * 
 * If the external module needs any functionality beyond the "basic" functionality, it needs to build its own objects, e.g.
 * 
 * class RouteNode {
 *     private Node node ;
 *     public getNodeInfo() { return node ;}
 *     private double arrivalTimeArray ;
 *     ...
 * }
 * 
 * Our intention at this point is to keep the interfaces stable (after they are released, which as of now (feb09) is not
 * yet the case) <em> but </em> to potentially extend them.
 * 
 * (Which assumes that, for the time being, no <em> implementations </em> of the interfaces exist except inside matsim.)
 * 
 */
package org.matsim.api.core.v01;
