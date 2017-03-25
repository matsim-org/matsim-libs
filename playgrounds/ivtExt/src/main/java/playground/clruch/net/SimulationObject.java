package playground.clruch.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimulationObject implements Serializable {

    public String infoLine = "";

    public long now;

    public List<RequestContainer> requests = new ArrayList<>();

    public List<VehicleContainer> vehicles;

    public Serializable serializable;

}
