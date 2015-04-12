package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Pwvm_GeoColumnCreator {

	public static void main(String[] args) {

		Connection conn = Pwvm_DatabaseConnection.dbconnect();

		try {
			
			String stmt = "SELECT DropGeometryColumn('kid_fahrzeug','k24_geom')";
			Statement st1 = conn.createStatement();
			System.out.println(stmt);
			ResultSet rs1 = st1.executeQuery(stmt);
			
			stmt = "SELECT AddGeometryColumn ('kid_fahrzeug','k24_geom',31467,'POINT',2)";
			System.out.println(stmt);
			rs1 = st1.executeQuery(stmt);

			stmt = "COMMENT ON COLUMN kid_fahrzeug.k24_geom IS 'Tatsaechlicher Fahrzeugstandort (errechnet aus k24d und k24e)'";
			st1.executeUpdate(stmt);

			stmt = "SELECT fahrzeugid FROM kid_fahrzeug";
			System.out.println(stmt);
			rs1 = st1.executeQuery(stmt);			

			while (rs1.next()) {
				String current_id = rs1.getString(1);
				stmt = "UPDATE kid_fahrzeug " +
						"SET k24_geom=" +
						"ST_Transform(ST_PointFromText(" +
						"(SELECT 'POINT('||k24d||' '||k24e||')'" +
						" FROM kid_fahrzeug" +
						" WHERE fahrzeugid="+current_id+"" +
						// keine Koordinate erzeugen, wenn diese als fehlend (-1 oder -9) kodiert
						// oder ungueltig ist (wenige Faelle haben negative, ungueltige Koordinaten
						" AND k24d::double precision >= 0" +
						" AND k24e::double precision >= 0), 4326), " +						
//								" AND k24d::double precision != -1" +
//								" AND k24e::double precision != -1" +
//								" AND k24d::double precision != -9" +
//								" AND k24e::double precision != -9), " +
//						" 4326)," +
						" 31467) " +
						"WHERE fahrzeugid="+current_id;
				System.out.println(stmt);
				Statement st2 = conn.createStatement();
				st2.executeUpdate(stmt);

				stmt = "SELECT 1 FROM kid_fahrzeug WHERE k24_geom IS NULL AND fahrzeugid = "+current_id;
				ResultSet rs2 = st2.executeQuery(stmt);
				if (rs2.next()) {

					stmt = "UPDATE kid_fahrzeug SET k24_geom=(SELECT ST_Centroid(the_geom) FROM plz WHERE plz=kid_fahrzeug.k24c) " +
					"WHERE kid_fahrzeug.fahrzeugid = "+current_id+
					" AND kid_fahrzeug.k24c IN (SELECT plz FROM plz)";
					st2.executeUpdate(stmt);
				}
			}

			stmt = "CREATE INDEX kid_fahrzeug_the_geom_gist ON kid_fahrzeug USING gist (k24_geom) TABLESPACE \"index\"";

			System.out.println(stmt);
			//				st1.executeUpdate(stmt);

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

