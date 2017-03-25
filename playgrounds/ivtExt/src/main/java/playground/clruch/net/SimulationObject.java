package playground.clruch.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import playground.clruch.gfx.util.RequestContainer;
import playground.clruch.gfx.util.VehicleContainer;

public class SimulationObject implements Serializable {

    public String infoLine = "";

    public long now;
    
    public List<RequestContainer> requests = new ArrayList<>();
    
    public List<VehicleContainer> vehicles;

}
