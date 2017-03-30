package playground.clruch.gheat;

public class MercatorProjection implements Projections {
    static final double MinLatitude = -85.05112878;
    static final double MaxLatitude = 85.05112878;
    static final double MinLongitude = -180;
    static final double MaxLongitude = 180;

    private static double clip(double n, double minValue, double maxValue) {
        return Math.min(Math.max(n, minValue), maxValue);
    }

    public Size getTileMatrixSizePixel(int zoom) {
        Size s = getTileMatrixSizeXY(zoom);
        return new Size(s.getWidth() * HeatMap.SIZE, s.getHeight() * HeatMap.SIZE);
    }

    public Size getTileMatrixSizeXY(int zoom) {
        Size sMin = getTileMatrixMinXY(zoom);
        Size sMax = getTileMatrixMaxXY(zoom);
        return new Size(sMax.getWidth() - sMin.getWidth() + 1, sMax.getHeight() - sMin.getHeight() + 1);
    }

    public Size getTileMatrixMaxXY(int zoom) {
        long xy = (1 << zoom);
        return new Size(xy - 1, xy - 1);
    }

    public Size getTileMatrixMinXY(int zoom) {
        return new Size(0, 0);
    }

    public DataPoint fromLatLngToPixel(PointLatLng center, int zoom) {
        DataPoint ret = new DataPoint(0, 0);
        center.setLatitude(clip(center.getLatitude(), MinLatitude, MaxLatitude));
        center.setLongitude(clip(center.getLongitude(), MinLongitude, MaxLongitude));
        double x = (center.getLongitude() + 180) / 360;
        double sinLatitude = Math.sin(center.getLatitude() * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);
        Size s = getTileMatrixSizePixel(zoom);
        long mapSizeX = (long) s.getWidth();
        long mapSizeY = (long) s.getHeight();
        ret.setX((long) clip(x * mapSizeX + 0.5, 0, mapSizeX - 1));
        ret.setY((long) clip(y * mapSizeY + 0.5, 0, mapSizeY - 1));
        return ret;
    }

    public DataPoint fromLatLngToPixel(double latitude, double longitude, int zoom) {
        DataPoint ret = new DataPoint(0, 0);
        latitude = clip(latitude, MinLatitude, MaxLatitude);
        longitude = clip(longitude, MinLongitude, MaxLongitude);
        double x = (longitude + 180) / 360;
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);
        Size s = getTileMatrixSizePixel(zoom);
        long mapSizeX = (long) s.getWidth();
        long mapSizeY = (long) s.getHeight();
        ret.setX((long) clip(x * mapSizeX + 0.5, 0, mapSizeX - 1));
        ret.setY((long) clip(y * mapSizeY + 0.5, 0, mapSizeY - 1));
        return ret;
    }

    public PointLatLng fromPixelToLatLng(DataPoint tlb, int zoom) {
        PointLatLng ret = new PointLatLng(0, 0, 0);
        Size s = getTileMatrixSizePixel(zoom);
        double mapSizeX = s.getWidth();
        double mapSizeY = s.getHeight();
        double xx = (clip(tlb.getX(), 0, mapSizeX - 1) / mapSizeX) - 0.5;
        double yy = 0.5 - (clip(tlb.getY(), 0, mapSizeY - 1) / mapSizeY);
        ret.setLatitude(90 - 360 * Math.atan(Math.exp(-yy * 2 * Math.PI)) / Math.PI);
        ret.setLongitude(360 * xx);
        return ret;
    }

    public DataPoint fromPixelToTileXY(DataPoint pixelCoordinate) {
        return new DataPoint((long) (pixelCoordinate.getX() / HeatMap.SIZE), (long) (pixelCoordinate.getY() / HeatMap.SIZE));
    }

    public DataPoint fromTileXYToPixel(DataPoint dataPoint) {
        return new DataPoint((dataPoint.getX() * HeatMap.SIZE), (dataPoint.getY() * HeatMap.SIZE));
    }
}
