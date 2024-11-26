package org.matsim.freight.receiver.multiday;

import org.matsim.core.utils.misc.OptionalTime;

public class MultidayUtils {
    private static final double ONE_DAY = 24.0*60.0*60.0;

    static OptionalTime getTimeOfDay(OptionalTime time){
        if(time.isUndefined()){
            return time;
        }

        double timeDouble = time.seconds();
        while(timeDouble > ONE_DAY){
            timeDouble -= ONE_DAY;
        }
        return OptionalTime.defined(timeDouble);
    }
}
