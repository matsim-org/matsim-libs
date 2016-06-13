package org.matsim.contrib.taxi.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;


public class TaxiOptimizerContext
{
    public final TaxiData taxiData;
    public final Network network;
    public final MobsimTimer timer;
    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;
    public final TaxiScheduler scheduler;


    public TaxiOptimizerContext(TaxiData taxiData, Network network, MobsimTimer timer,
            TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler)
    {
        this.taxiData = taxiData;
        this.network = network;
        this.timer = timer;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
        this.scheduler = scheduler;
    }
}
