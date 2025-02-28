package org.matsim.contrib.profiling.aop;

import jdk.jfr.Event;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.Vehicle;

public aspect PersonVehiclesAspect {

    pointcut personVehicles(PersonVehicles p, String mode):
            target(p) && args(mode) &&
                    call(Id<Vehicle> PersonVehicles.getVehicle(..));

    Id<Vehicle> around(PersonVehicles p, String mode): personVehicles(p,mode) {
        Event jfrEvent = JFRMatsimEvent.create("scoring AOP: " + p.getClass().getName());

        System.out.println("AOP profiling: " + p.getClass().getSimpleName());

        Id<Vehicle> result;
        System.out.println("mode: test");
        jfrEvent.begin();
        if ("test".equals(mode)) {
            result = Id.createVehicleId("aspect");
        } else {
            result = proceed(p, mode);
        }
        jfrEvent.commit();
        return result;
    }

}
