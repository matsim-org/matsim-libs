/**
 * Design thoughts:<ul>
 * <li> For large scale scenarios (e.g. all of Germany) simulating each person's parking search is not feasable because it would bump computation time from days to weeks.
 * However, one might want a mechanism that makes it less attractive to visit an area that's already overcrowded with many parking vehicles by car. The parking proxi is,
 * as the name suggests a proxy for actual parking simulation. Instead, the time loss induced by searching for a parking spot is estimated based on the number of cars already
 * parked within a certain area.
 * <li> The delay is introduced by changing the walk time of the egress walk of car legs. Since walk legs are teleported modes, this translates directly to the mobsim.
 * <li> The module is designed in such a way that this change only happens in the mobsim and is only visible in the experienced plans file. The "planned" plan does not
 * contain the delay. This circumvents the issue that the number of cars might change between iterations but the stored plan does not - the delay is calculated each iteration
 * directly in the mobsim.
 * </ul>
 * 
 * 
 * @author tkohl / Senozon
 *
 */
package org.matsim.contrib.parking.parkingproxy;