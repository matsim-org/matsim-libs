package playground.michalm.vrp.demand;

import org.matsim.core.mobsim.framework.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;


public class TaxiCustomer
    implements Customer
{
    private int id;
    private Vertex vertex;
    private MobsimAgent passanger;

    
    public TaxiCustomer(int id, Vertex vertex, MobsimAgent passanger)
    {
        this.id = id;
        this.vertex = vertex;
        this.passanger = passanger;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return passanger.getId().toString();
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }
    
    
    public MobsimAgent getPassanger()
    {
        return passanger;
    }
}
