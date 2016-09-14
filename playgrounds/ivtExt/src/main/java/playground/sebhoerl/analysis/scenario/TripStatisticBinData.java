package playground.sebhoerl.analysis.scenario;

import playground.sebhoerl.analysis.bins.BinDataFactory;

public class TripStatisticBinData {
    public double accumulatedTravelDistance = 0.0;
    public double accumulatedTravelTime = 0.0;
    
    public long numberOfTrips = 0;
    public long numberOfDepartures = 0;
    public long numberOfArrivals = 0;
    
    static public class Factory implements BinDataFactory<TripStatisticBinData> {
        @Override
        public TripStatisticBinData createData() {
            return new TripStatisticBinData();
        }
    }
}
