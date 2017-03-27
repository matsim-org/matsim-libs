package playground.clruch.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimulationObject implements Serializable {

    public int iteration = 0;
    public String infoLine = "";
    public long now;
    public int total_matchedRequests;
    public List<RequestContainer> requests = new ArrayList<>();
    public List<VehicleContainer> vehicles;
    public Serializable serializable;

}
