package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;


/**
 * @author michalm
 */
public class RequestImpl
    implements Request
{
    // TODO ideas for future
    // perhaps
    // 1.
    // introduce fake-Request that represents return to the depot (makes sense if one wants to
    // change vehicle's depots from day to day, etc.). Any vehicle relocations during the day could
    // also be performed as a request of a special kind
    //
    // 2.
    // departure: instead of: "arrival-start-finish-departure" use:
    // "departure-arrival-start-finish"
    // this improves optVeh->veh mapping
    // its easier to switch between EarliestArrival/LatestArrival strategies

    private final int id;

    private final Customer customer;

    private final Vertex fromVertex;
    private final Vertex toVertex;

    private final int quantity;
    private final double priority;

    private final int duration;
    private final int t0;// earliest start time
    private final int t1;// latest start time

    private final boolean fixedVehicle;

    private ServeTask serveTask = null;
    private ReqStatus status = ReqStatus.UNPLANNED;// based on: serveTask.getStatus();
    private int submissionTime = -1;


    // public Type type;

    public RequestImpl(int id, Customer customer, Vertex fromVertex, Vertex toVertex, int quantity,
            double priority, int duration, int t0, int t1, boolean fixedVehicle)
    {
        this.id = id;
        this.customer = customer;
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
        this.quantity = quantity;
        this.priority = priority;
        this.duration = duration;
        this.t0 = t0;
        this.t1 = t1;
        this.fixedVehicle = fixedVehicle;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public Customer getCustomer()
    {
        return customer;
    }


    @Override
    public Vertex getFromVertex()
    {
        return fromVertex;
    }


    @Override
    public Vertex getToVertex()
    {
        return toVertex;
    }


    @Override
    public int getQuantity()
    {
        return quantity;
    }


    @Override
    public double getPriority()
    {
        return priority;
    }


    @Override
    public int getDuration()
    {
        return duration;
    }


    @Override
    public int getT0()
    {
        return t0;
    }


    @Override
    public int getT1()
    {
        return t1;
    }


    @Override
    public int getSubmissionTime()
    {
        return submissionTime;
    }


    @Override
    public boolean getFixedVehicle()
    {
        return fixedVehicle;
    }


    @Override
    public Schedule getSchedule()
    {
        return serveTask.getSchedule();
    }


    @Override
    public ReqStatus getStatus()
    {
        if (serveTask != null) {
            switch (serveTask.getStatus()) {
                case PLANNED:
                    return ReqStatus.PLANNED;

                case STARTED:
                    return ReqStatus.STARTED;

                case PERFORMED:
                    return ReqStatus.PERFORMED;
            }
        }

        return status;
    }


    @Override
    public void deactivate(int submissionTime)
    {
        if (getStatus() != ReqStatus.UNPLANNED) {
            throw new RuntimeException("Only UNPLANNED request can be deactivated.");
        }

        status = ReqStatus.INACTIVE;
        this.submissionTime = submissionTime;
    }


    @Override
    public void submit()
    {
        if (getStatus() != ReqStatus.INACTIVE) {
            throw new RuntimeException("Request has been already submitted.");
        }

        status = ReqStatus.UNPLANNED;
    }


    @Override
    public void reject()
    {
        switch (getStatus()) {
            case PLANNED:
                // TODO some code to handle ServeTask????????
                throw new UnsupportedOperationException("What about ServeTask???");

            case UNPLANNED:
                status = ReqStatus.REJECTED;
                return;

            default:
                throw new RuntimeException("ReqStatus must be UNPLANNED or PLANNED");
        }
    }


    @Override
    public void cancel()
    {
        switch (getStatus()) {
            case PLANNED:
            case STARTED:
                // TODO some code to handle ServeTask????????
                throw new UnsupportedOperationException("What about ServeTask???");

            case UNPLANNED:
                status = ReqStatus.CANCELLED;
                return;

            default:
                throw new RuntimeException("ReqStatus must be UNPLANNED, PLANNED or STARTED");
        }
    }


    @Override
    public void reset()
    {
        serveTask = null;
        status = (submissionTime == -1) ? ReqStatus.INACTIVE : ReqStatus.UNPLANNED;
    }


    @Override
    public void notifyScheduled(ServeTask serveTask)
    {
        if (getStatus() != ReqStatus.UNPLANNED) {
            throw new RuntimeException("ReqStatus must be UNPLANNED");
        }

        this.serveTask = serveTask;
        this.status = null;// getStatus() will determine ReqStatus based on TaskStatus
    }


    @Override
    public void notifyUnscheduled()
    {
        switch (getStatus()) {
            case PLANNED: // typical
            case STARTED: // ?? ONLY in case of CANCELLATION !!!
                this.serveTask = null;
                this.status = ReqStatus.UNPLANNED;
                return;

            default:
                throw new RuntimeException("ReqStatus must be UNPLANNED, PLANNED or STARTED");
        }
    }


    @Override
    public ServeTask getServeTask()
    {
        return serveTask;
    }


    @Override
    public int getStartTime()
    {
        return serveTask.getBeginTime();
    }


    @Override
    public int getEndTime()
    {
        return serveTask.getEndTime();
    }


    // public Request()
    // {
    // status = Status.RECEIVED;
    // type = Type.NORMAL;
    // }

    @Override
    public String toString()
    {
        // return "Request_" + id;

        // return "Request_" + id + " [A=" + arrivalTime + ", S=(" + t0 + ", " + startTime + ", " +
        // t1
        // + "), F=" + finishTime + ", D=" + departureTime + "]";

        if (serveTask == null) {
            return "Request_" + id + " [S=(" + t0 + ", ???, " + t1 + "), F=???]";
        }

        return "Request_" + id + " [S=(" + t0 + ", " + serveTask.getBeginTime() + ", " + t1
                + "), F=" + serveTask.getEndTime() + "]";
    }

}
