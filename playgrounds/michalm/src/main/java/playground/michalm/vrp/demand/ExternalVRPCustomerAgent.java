package playground.michalm.vrp.demand;

import java.util.*;

import org.apache.commons.lang.builder.*;

import pl.poznan.put.vrp.dynamic.customer.*;
import pl.poznan.put.vrp.dynamic.customer.CustomerAction.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.*;


public class ExternalVRPCustomerAgent
    implements VRPCustomerAgent
{
    private Customer customer;

    private Queue<CustomerAction> caQueue;//customerActionsQueue


    public ExternalVRPCustomerAgent(Customer customer)
    {
        this.customer = customer;
    }


    @Override
    public Customer getCustomer()
    {
        return customer;
    }
    
    public void doSimStep(int time)
    {
//        while (!caQueue.isEmpty() && caQueue.peek().time == time) {
//            CustomerAction action = caQueue.poll();
//            CAType actionType = action.type;
//            Request req = action.request;
//
//            if (actionType == CAType.REQ_SUBMIT) {
//                req.status = ReqStatus.RECEIVED;
//                req.submissionTime = time;
//            }
//            else {// MODIFY or CANCEL
//                if (req.status != ReqStatus.RECEIVED && req.status != ReqStatus.PLANNED) {
//                    // TODO
//                    System.out.println("Problem: cannot cancel/modify request with status: "
//                            + req.status);
//                    System.out.println("Simulated action: "
//                            + ToStringBuilder.reflectionToString(action));
//                    System.out.println("Req: " + ToStringBuilder.reflectionToString(req));
//                    System.out.println("===================\n\n");
//                    continue;
//                }
//
//                if (actionType == CAType.REQ_CANCEL) {
//                    throw new RuntimeException("Unsupported value: " + actionType);
//                }
//                else if (actionType == CAType.REQ_MODIFY) {
//                    throw new RuntimeException("Unsupported value: " + actionType);
//                }
//                else {
//                    throw new RuntimeException("Unknown action: " + actionType);
//                }
//
//            }
//
//            notifyListeners(actionType, time, req);
//            changed = true;
//        }
//
//        return changed;
    }
}
