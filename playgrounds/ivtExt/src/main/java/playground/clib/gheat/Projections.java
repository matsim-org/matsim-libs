// code by varunpant
package playground.clib.gheat;

public interface Projections {
    DataPoint fromLatLngToPixel(PointLatLng center, int zoom);

    Size getTileMatrixMinXY(int zoom);

    Size getTileMatrixMaxXY(int zoom);

    DataPoint fromLatLngToPixel(double latitude, double longitude, int zoom);

    PointLatLng fromPixelToLatLng(DataPoint tlb, int zoom);

    DataPoint fromPixelToTileXY(DataPoint pixelCoordinate);

    DataPoint fromTileXYToPixel(DataPoint dataPoint);
}
