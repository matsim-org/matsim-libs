/**
 * While integrating parking into existing simulations, some agent may have plans like
 * home - car - work - pt - home - car - shopping - car - home
 * i.e, the person's car is at a considerably different location than the person using the car. 
 * In reality, a spouse might have driven the car back from work to home. With the standard MATSim logic (no vehicle ownership models),
 * this does not work and the vehicle would be teleported back home.
 * However, with parking models, the vehicles' last position is explicit. 
 * With this interface we offer several ways to cope with this:
 * <ul>
 * <li>The agent always walks back to his car. Useful for scenarios where above mentioned behavior does not happen</li>
 * <li>The vehicle is teleported to a free parking spot somewhere near the agent's location</li>
 * </ul> 
 * 
 * 
 */
package org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic;