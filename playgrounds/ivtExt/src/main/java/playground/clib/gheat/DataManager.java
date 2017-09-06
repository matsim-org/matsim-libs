// code by varunpant
package playground.clib.gheat;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private Projections _projection = new MercatorProjection();
    private HeatMapDataSource dataSource;

    public DataManager(HeatMapDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataPoint[] GetPointsForTile(int x, int y, BufferedImage dot, int zoom) throws InterruptedException {
        List<DataPoint> points = new ArrayList<DataPoint>();
        Size maxTileSize = new Size(HeatMap.SIZE, HeatMap.SIZE);
        DataPoint adjustedDataPoint;
        DataPoint pixelCoordinate;
        // Top Left Bounds
        DataPoint tlb = _projection.fromTileXYToPixel(new DataPoint(x, y));
        // Lower right bounds
        DataPoint lrb = new DataPoint((tlb.getX() + maxTileSize.getWidth()) + dot.getWidth(), (tlb.getY() + maxTileSize.getHeight()) + dot.getWidth());
        // pad the Top left bounds
        tlb = new DataPoint(tlb.getX() - dot.getWidth(), tlb.getY() - dot.getHeight());
        PointLatLng[] TilePoints = dataSource.GetList(tlb, lrb, zoom, _projection);
        // Go throught the list and convert the points to pixel cooridents
        for (PointLatLng llDataPoint : TilePoints) {
            // Now go through the list and turn it into pixel points
            pixelCoordinate = _projection.fromLatLngToPixel(llDataPoint.getLatitude(), llDataPoint.getLongitude(), zoom);
            // Make sure the weight is still pointing after the conversion
            pixelCoordinate.setWeight(llDataPoint.getWeight());
            // Adjust the point to the specific tile
            adjustedDataPoint = AdjustMapPixelsToTilePixels(new DataPoint(x, y), pixelCoordinate);
            // Make sure the weight is still pointing after the conversion
            adjustedDataPoint.setWeight(pixelCoordinate.getWeight());
            // Add the point to the list
            points.add(adjustedDataPoint);
        }
        return points.toArray(new DataPoint[points.size()]);
    }

    private static DataPoint AdjustMapPixelsToTilePixels(DataPoint tileXYPoint, DataPoint mapPixelPoint) {
        return new DataPoint(mapPixelPoint.getX() - (tileXYPoint.getX() * HeatMap.SIZE), mapPixelPoint.getY() - (tileXYPoint.getY() * HeatMap.SIZE));
    }
}
