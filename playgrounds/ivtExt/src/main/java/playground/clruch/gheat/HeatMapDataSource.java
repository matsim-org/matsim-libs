package playground.clruch.gheat;

public interface HeatMapDataSource {
    PointLatLng[] GetList(DataPoint tlb, DataPoint lrb, int zoom, Projections _projection);
}
