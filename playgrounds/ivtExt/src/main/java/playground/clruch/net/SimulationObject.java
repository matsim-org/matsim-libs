// code by jph
package playground.clruch.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * this class deliberately does not have any helper/member functions.
 * 
 * {@link SimulationObject} is only used for communication and storage of the
 * status of the AV simulation
 * 
 * instead, implement utility functions in {@link SimulationObjects}
 */
public class SimulationObject implements Serializable {

    /**
     * WARNING:
     * 
     * ANY MODIFICATION IN THIS CLASS EXCEPT COMMENTS
     * WILL INVALIDATE PREVIOUS SIMULATION RECORDINGS
     * 
     * DO NOT MODIFY THIS CLASS UNLESS
     * THERE IS A VERY GOOD REASON
     */

    /**
     * iteration number the object was recorded
     */
    public int iteration = 0;

    /**
     * infoline that is shown in the console
     */
    public String infoLine = "";

    /**
     * time
     */
    public long now;

    /**
     * total of matched request until now
     */
    public int total_matchedRequests;

    /**
     * list of request
     */
    public List<RequestContainer> requests = new ArrayList<>();

    /**
     * vehicles shall not be an empty list
     * but always contain the complete collection of vehicles
     */
    public List<VehicleContainer> vehicles;

    /**
     * use field serializable to attach information that is specific to dispatcher
     * the value is null by default
     */
    public Serializable serializable;

    /**
     * DO NOT PUT MEMBER FUNCTIONS OR STATIC FUNCTION INSIDE CLASS !!!
     */

}
