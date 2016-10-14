package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;

import java.util.Collection;

public interface AVOperator {
    Id<AVOperator> getId();
    AVDispatcher getDispatcher();
}
