package playground.clruch.gheat.datasources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import playground.clruch.gheat.DataPoint;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.Projections;

public class PostGisDataSource implements HeatMapDataSource {
    static String query = null;

    public PostGisDataSource(String query) {
        this.query = query;
    }

    public PointLatLng[] GetList(DataPoint tlb, DataPoint lrb, int zoom, Projections _projection) {
        List<PointLatLng> llList = null;
        PointLatLng ptlb;
        PointLatLng plrb;
        ptlb = _projection.fromPixelToLatLng(tlb, zoom);
        plrb = _projection.fromPixelToLatLng(lrb, zoom);
        llList = getData(plrb.getLongitude(), plrb.getLatitude(), ptlb.getLongitude(), ptlb.getLatitude());
        PointLatLng[] result = new PointLatLng[llList.size()];
        for (int i = 0; i < llList.size(); i++) {
            result[i] = llList.get(i);
        }
        return result;
    }

    private List<PointLatLng> getData(double llx, double lly, double ulx, double uly) {
        List<PointLatLng> llList = new ArrayList<PointLatLng>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            // con = DBPool.getConnection();
            // String stm = query;
            // pst = con.prepareStatement(stm);
            // pst.setDouble(1, llx);
            // pst.setDouble(2, lly);
            // pst.setDouble(3, ulx);
            // pst.setDouble(4, uly);
            // System.out.println(pst);
            // rs = pst.executeQuery();
            // while (rs.next()) {
            // double weight = rs.getDouble("weight");
            // double longitude = rs.getDouble("longitude");// x
            // double latitude = rs.getDouble("latitude"); // y
            // PointLatLng pt = new PointLatLng(longitude, latitude, weight);
            // llList.add(pt);
            // }
        } catch (Exception ex) {
            Logger lgr = Logger.getLogger(PostGisDataSource.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            try {
                rs.close();
                pst.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return llList;
        }
    }
}
