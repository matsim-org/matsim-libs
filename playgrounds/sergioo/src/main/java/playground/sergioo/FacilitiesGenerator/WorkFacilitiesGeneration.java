package playground.sergioo.FacilitiesGenerator;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.algebra.PointND;
import util.algebra.PointNDImpl;
import util.clustering.Cluster;
import util.clustering.KMeans;
import util.dataBase.DataBaseAdmin;
import util.dataBase.NoConnectionException;

public class WorkFacilitiesGeneration {

	//Constants
	private static final int SIZE = 10;

	//Attributes

	//Methods
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		ResultSet workEndTimesResult = dataBaseHits.executeQuery("SELECT id,t3_starttime FROM hits.hitsshort WHERE t6_purpose='home'");
		Set<PointND<Double>> points = new HashSet<PointND<Double>>();
		while(workEndTimesResult.next()) {
			ResultSet workStartTimesResult = dataBaseHits.executeQuery("SELECT t4_endtime FROM hits.hitsshort WHERE t6_purpose='work' AND id="+workEndTimesResult.getInt(1));
			if(workStartTimesResult.next())
				points.add(new PointNDImpl.Double(new Double[]{workStartTimesResult.getDouble(1), workEndTimesResult.getDouble(2)}));
		}
		Map<Integer, Cluster<Double>> clusters = new KMeans<Double>().getClusters(SIZE, points);
		for(Entry<Integer, Cluster<Double>> clusterE:clusters.entrySet()) {
			System.out.println(clusterE.getKey());
			for(PointND<Double> point:clusterE.getValue().getPoints())
				System.out.println("    ("+point.getElement(0)+","+point.getElement(1)+")");
			System.out.println();
		}
		for(Entry<Integer, Cluster<Double>> clusterE:clusters.entrySet()) {
			System.out.println(clusterE.getKey());
			System.out.println("    ("+clusterE.getValue().getMean().getElement(0)+","+clusterE.getValue().getMean().getElement(1)+")");
			System.out.println();
		}
	}
	
}
