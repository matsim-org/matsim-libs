package playground.clruch.gheat.datasources;

import playground.clruch.gheat.DataPoint;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.Projections;

public interface HeatMapDataSource {
    PointLatLng[] GetList(DataPoint tlb, DataPoint lrb, int zoom, Projections _projection);
}
