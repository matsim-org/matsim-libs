package playground.clruch.gheat.datasources;

import java.io.BufferedReader;
import java.io.FileReader;

import playground.clruch.gheat.DataPoint;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.Projections;
import playground.clruch.gheat.datasources.QuadTree.QuadTree;

public class QuadTreeDataSource implements HeatMapDataSource {
    static QuadTree qt = new QuadTree(-180.000000, -90.000000, 180.000000, 90.000000);

    /* Constructor for QuadTreeDataSource takes indexes for longitude,latitude and weight colums from csv file. */
    public QuadTreeDataSource(String filePath, int longitudeIndex, int latitudeIndex, int weightIndex) {
        LoadPointsFromFile(filePath, longitudeIndex, latitudeIndex, weightIndex);
    }

    public PointLatLng[] GetList(DataPoint tlb, DataPoint lrb, int zoom, Projections _projection) {
        PointLatLng ptlb = _projection.fromPixelToLatLng(lrb, zoom);
        PointLatLng plrb = _projection.fromPixelToLatLng(tlb, zoom);
        PointLatLng[] list = qt.searchIntersect(plrb.getLongitude(), ptlb.getLatitude(), ptlb.getLongitude(), plrb.getLatitude());
        return list;
    }

    private void LoadPointsFromFile(String source, int longitudeIndex, int latitudeIndex, int weightIndex) {
        String[] item;
        String[] lines = readAllTextFileLines(source);
        for (String line : lines) {
            item = line.split(",");
            qt.set(Double.parseDouble(item[longitudeIndex]), Double.parseDouble(item[latitudeIndex]), Double.parseDouble(item[latitudeIndex]));
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
}
