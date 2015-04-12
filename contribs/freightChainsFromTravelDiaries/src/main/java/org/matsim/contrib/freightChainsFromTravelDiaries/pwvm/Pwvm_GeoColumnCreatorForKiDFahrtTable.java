package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Pwvm_GeoColumnCreatorForKiDFahrtTable {

	public static void main(String[] args) {

		Connection conn = Pwvm_DatabaseConnection.dbconnect();

		try {
			
			String stmt = "SELECT DropGeometryColumn('kid_fahrt','f08_geom')";
			Statement st1 = conn.createStatement();
			System.out.println(stmt);
			ResultSet rs1 = st1.executeQuery(stmt);
			
			stmt = "SELECT AddGeometryColumn ('kid_fahrt','f08_geom',31467,'POINT',2)";
			

			System.out.println(stmt);
			rs1 = st1.executeQuery(stmt);

			stmt = "COMMENT ON COLUMN kid_fahrt.f08_geom IS 'Koordinate des Ziels (errechnet aus f08d und f08e)'";
//			stmt = "COMMENT ON COLUMN kid_fahrt.f02_geom IS 'Koordinate des Starts (errechnet aus f02d und f02e)'";
			
			st1.executeUpdate(stmt);

			stmt = "SELECT fahrzeugid, fahrtid FROM kid_fahrt ORDER BY fahrzeugid, fahrtid";
			System.out.println(stmt);
			rs1 = st1.executeQuery(stmt);			

			while (rs1.next()) {
				String current_fahrzeug_id = rs1.getString(1);
				String current_fahrt_id = rs1.getString(2);
				stmt = "UPDATE kid_fahrt " +
						"SET f08_geom=" +
						"ST_Transform(ST_PointFromText(" +
						"(SELECT 'POINT('||f08d||' '||f08e||')' FROM kid_fahrt " +
						"WHERE fahrzeugid = "+current_fahrzeug_id+"" +
								" AND fahrtid="+current_fahrt_id+"" +
//								" AND f08d::double precision != -1" +
//								" AND f08e::double precision != -1" +
//								" AND f08d::double precision != -9" +
//								" AND f08e::double precision != -9), 4326), " +
								// keine Koordinate erzeugen, wenn diese als fehlend (-1 oder -9) kodiert
								// oder ungueltig ist (wenige Faelle haben negative, ungueltige Koordinaten
								" AND f08d::double precision >= 0" +
								" AND f08e::double precision >= 0), 4326), " +
						"31467) " +
						"WHERE fahrzeugid = "+current_fahrzeug_id+" AND fahrtid="+current_fahrt_id;
				System.out.println(stmt);
				Statement st2 = conn.createStatement();
				st2.executeUpdate(stmt);

				stmt = "SELECT 1 FROM kid_fahrt WHERE f08_geom IS NULL AND fahrzeugid = "+current_fahrzeug_id+" AND fahrtid="+current_fahrt_id;
				System.out.println(stmt);
				ResultSet rs2 = st2.executeQuery(stmt);
				if (rs2.next()) {
					stmt = "UPDATE kid_fahrt SET f08_geom=(SELECT ST_Centroid(the_geom) FROM plz WHERE plz=kid_fahrt.f08c) " +
					"WHERE kid_fahrt.fahrzeugid = "+current_fahrzeug_id+" AND kid_fahrt.fahrtid="+current_fahrt_id +
					" AND kid_fahrt.f08c IN (SELECT plz FROM plz)";
					st2.executeUpdate(stmt);
				}
			}

			stmt = "CREATE INDEX kid_fahrt_f08_geom_gist ON kid_fahrt USING gist (f08_geom) TABLESPACE \"index\"";

			System.out.println(stmt);
//			st1.executeUpdate(stmt);

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

