package playground.clruch.gheat.datasources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import playground.clruch.gheat.DataPoint;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.Projections;

public class FileDataSource implements HeatMapDataSource {
//    static 
    List<PointLatLng> _pointList = null;

    public FileDataSource(String filePath, int longitudeIndex, int latitudeIndex, int weightIndex) {
        _pointList = new ArrayList<PointLatLng>();
        LoadPointsFromFile(filePath, longitudeIndex, latitudeIndex, weightIndex);
    }

    private void LoadPointsFromFile(String source, int longitudeIndex, int latitudeIndex, int weightIndex) {
        String[] item;
        String[] lines = readAllTextFileLines(source);
        for (String line : lines) {
            item = line.split(",");
            _pointList.add(new PointLatLng( //
                    Double.parseDouble(item[longitudeIndex]), //
                    Double.parseDouble(item[latitudeIndex]), //
                    Double.parseDouble(item[weightIndex])));
        }
    }

    private static String[] readAllTextFileLines(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            String textLine;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((textLine = br.readLine()) != null) {
                sb.append(textLine);
                sb.append('\n');
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            if (sb.length() == 0)
                sb.append("\n");
        }
        return sb.toString().split("\n");
    }

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
}
