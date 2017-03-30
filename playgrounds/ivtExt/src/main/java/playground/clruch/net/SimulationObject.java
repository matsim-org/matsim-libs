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

    public int iteration = 0;
    public String infoLine = "";
    public long now;
    public int total_matchedRequests;
    public List<RequestContainer> requests = new ArrayList<>();
    public List<VehicleContainer> vehicles;
    public Serializable serializable;

}
