package playground.michalm.vrp.online;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.simulator.*;


public class MATSimMonitoring
    extends SimulatedMonitoring
{
    //private MATSimVRPData data;


    @Override
    public boolean updateData(VRPData vrpData)
    {
        boolean changed = super.updateData(vrpData);

        // for each new time slice (e.g. quarter) new short Paths are used

        return changed;
    }
}
