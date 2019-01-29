/**
 * The "Simulation Listeners/Events" are in addition to "matsim events".  "matsim events" concern traffic-related events, 
 * "simulation events" concern events related to simulation structure (such as "initialization is finished").
 * 
 * <p></p>
 * 
 * There is some debate if these things are so easy to separate; for example "time step finished" (Simulation Event) is the same as 
 * "clock advances by one second" (event related to the traffic world).  Nevertheless, there are strong opinions to leave these things
 * separate, and so they are separate.
 * 
 * <p></p>
 * 
 * For an example of how to have them in the same channel, see the dissertation of Christian Gloor.
 */
package org.matsim.core.mobsim.framework.listeners;