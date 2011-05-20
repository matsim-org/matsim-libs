package playground.michalm.vrp.events;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.events.*;


public class VRPCustomerEventImpl
    extends EventImpl
    implements VRPCustomerEvent
{
    public static final String EVENT_TYPE = "VRP_customer";

    public static final String ATTRIBUTE_FROM_LINK = "fromLink";
    public static final String ATTRIBUTE_TO_LINK = "toLink";
    public static final String ATTRIBUTE_CUSTOMER_ID = "customerId";
    public static final String ATTRIBUTE_REQUEST_ID = "requestId";

    private final Id fromLinkId;
    private final Id toLinkId;
    private final Id customerId;


    public VRPCustomerEventImpl(double time, Id fromLinkId, Id toLinkId, Id customerId)
    {
        super(time);

        this.fromLinkId = fromLinkId;
        this.toLinkId = toLinkId;
        this.customerId = customerId;

        // MISSING (currently)
        // public int quantity;
        // public double priority;
        //
        // public int duration;
        // public int t0;// earliest start time
        // public int t1;// latest start time

    }


    @Override
    public Id getFromLinkId()
    {
        return fromLinkId;
    }


    @Override
    public Id getToLinkId()
    {
        return toLinkId;
    }


    @Override
    public String getEventType()
    {
        return EVENT_TYPE;
    }


    @Override
    public Map<String, String> getAttributes()
    {
        Map<String, String> attr = super.getAttributes();

        attr.put(ATTRIBUTE_FROM_LINK, fromLinkId.toString());
        attr.put(ATTRIBUTE_TO_LINK, toLinkId.toString());
        attr.put(ATTRIBUTE_CUSTOMER_ID, customerId.toString());

        return attr;
    }
}
