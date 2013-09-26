package pl.poznan.put.vrp.dynamic.data;

import java.util.List;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.VrpGraph;


/**
 * @author michalm
 */
public class VrpData
{
    private List<Depot> depots;
    private List<Customer> customers;
    private List<Vehicle> vehicles;

    private List<Request> requests;

    private int time;

    private VrpGraph vrpGraph;

    private VrpDataParameters parameters;// TODO or properties


    public List<Depot> getDepots()
    {
        return depots;
    }


    public List<Customer> getCustomers()
    {
        return customers;
    }


    public List<Vehicle> getVehicles()
    {
        return vehicles;
    }


    public List<Request> getRequests()
    {
        return requests;
    }


    public int getTime()
    {
        return time;
    }


    public VrpGraph getVrpGraph()
    {
        return vrpGraph;
    }


    public void setVrpGraph(VrpGraph vrpGraph)
    {
        this.vrpGraph = vrpGraph;
    }


    public VrpDataParameters getParameters()
    {
        return parameters;
    }


    // SETTERS

    public void setDepots(List<Depot> depots)
    {
        this.depots = depots;
    }


    public void setCustomers(List<Customer> customers)
    {
        this.customers = customers;
    }


    public void setVehicles(List<Vehicle> vehicles)
    {
        this.vehicles = vehicles;
    }


    public void setRequests(List<Request> requests)
    {
        this.requests = requests;
    }


    public void setTime(int time)
    {
        this.time = time;
    }


    public void setParameters(VrpDataParameters parameters)
    {
        this.parameters = parameters;
    }


    public void removeAllRequests()
    {
        // Reset schedules
        for (Vehicle v : vehicles) {
            v.resetSchedule();
        }

        // remove all existing requests
        requests.clear();
    }
}
