// code by jph
package playground.clruch.gfx;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.idsc.queuey.view.gheat.DataPoint;
import ch.ethz.idsc.queuey.view.gheat.HeatMapDataSource;
import ch.ethz.idsc.queuey.view.gheat.PointLatLng;
import ch.ethz.idsc.queuey.view.gheat.Projections;

/* package */ class MatsimDataSource implements HeatMapDataSource {
    private List<PointLatLng> _pointList = null;

    public MatsimDataSource() {
        _pointList = new ArrayList<PointLatLng>();
    }

    public void addPoint(PointLatLng pointLatLng) {
        _pointList.add(pointLatLng);
    }

    @Override
    public PointLatLng[] GetList(DataPoint tlb, DataPoint lrb, int zoom, Projections _projection) {
        List<PointLatLng> llList = null;
        PointLatLng ptlb;
        PointLatLng plrb;
        ptlb = _projection.fromPixelToLatLng(tlb, zoom);
        plrb = _projection.fromPixelToLatLng(lrb, zoom);
        // System.out.println(ptlb + ", " + plrb); // jan commented this out
        // Find all of the points that belong in the expanded tile
        // Some points may appear in more than one tile depending where they appear
        llList = new ArrayList<PointLatLng>();
        for (PointLatLng point : _pointList) {
            if (point.getLatitude() <= ptlb.getLatitude() && point.getLongitude() >= ptlb.getLongitude() && point.getLatitude() >= plrb.getLatitude()
                    && point.getLongitude() <= plrb.getLongitude()) {
                llList.add(point);
            }
        }
        return llList.toArray(new PointLatLng[llList.size()]);
    }

    public void clear() {
        _pointList.clear();
    }
}
