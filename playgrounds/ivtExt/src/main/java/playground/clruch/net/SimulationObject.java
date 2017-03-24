package playground.clruch.net;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playground.clruch.gfx.util.VehicleContainer;

public class SimulationObject implements Serializable {

    public String infoLine = "";

    public long now;

    public Map<String, Integer> requestsPerLinkMap = new HashMap<>();
    
    public List<VehicleContainer> vehicles;

}
