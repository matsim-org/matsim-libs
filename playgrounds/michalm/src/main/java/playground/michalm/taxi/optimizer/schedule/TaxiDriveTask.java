package playground.michalm.taxi.optimizer.schedule;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.DriveTaskImpl;


public class TaxiDriveTask
    extends DriveTaskImpl
{
    public enum TaxiDriveType
    {
        PICKUP, DELIVERY, CRUISE;
    }


    private final Request request;// may be null for non-PICKUP/DELIVERY tasks
    private final TaxiDriveType driveType;


    public TaxiDriveTask(int beginTime, int endTime, Arc arc, Request request)
    {
        super(beginTime, endTime, arc);
        this.request = request;

        Vertex reqFromVertex = request.getFromVertex();

        if (reqFromVertex == arc.getToVertex()) {
            driveType = TaxiDriveType.PICKUP;
        }
        else if (reqFromVertex == arc.getFromVertex()) {
            driveType = TaxiDriveType.DELIVERY;
        }
        else {
            throw new IllegalArgumentException();
        }
    }


    public TaxiDriveTask(int beginTime, int endTime, Arc arc)
    {
        super(beginTime, endTime, arc);

        request = null;
        driveType = TaxiDriveType.CRUISE;
    }


    public TaxiDriveType getDriveType()
    {
        return driveType;
    }


    public Request getRequest()
    {
        return request;// may be null for non-PICKUP/DELIVERY tasks
    }


    @Override
    protected String commonToString()
    {
        return "[" + driveType.name() + "]" + super.commonToString();
    }
}
