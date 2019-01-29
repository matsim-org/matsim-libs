/**
 * This is probably a parking choice implementation of an approach where<ul>
 * <li> parking spaces on a per link basis are limited (somehow taken from external data)
 * <li> cars that attempt to park are <i>always</i> accepted but the drivers obtain a negative score for this
 * <li> over the iterations, parking supply is balanced with parking demand
 * <li> Walk from/to the parked vehicle is not executed, not even as teleportation, that is, it consumes zero time.  What <i> is </i> done
 * is that the walk leg is negatively scored.
 * </ul>
 * As said, this is our speculation.  
 * 
 * 
 * @author (of documentation) nagel
 */

package org.matsim.contrib.parking.parkingchoice;
