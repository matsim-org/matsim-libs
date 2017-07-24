package playground.joel.html;

import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Joel on 28.06.2017.
 */
public class ScenarioParameters implements Serializable {
    public int populationSize;
    public int iterations;
    public int redispatchPeriod;
    public int rebalancingPeriod;
    public int virtualNodes;

    public String dispatcher;
    public String networkName;
    public String user;
    public String date;

    public Tensor EMDks;
    public Tensor minFleet;
    public double minimumFleet;

    public Tensor availabilities;

    public ScenarioParameters() {
        user = System.getProperty("user.name");
        date = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss").format(new Date());
    }

}
