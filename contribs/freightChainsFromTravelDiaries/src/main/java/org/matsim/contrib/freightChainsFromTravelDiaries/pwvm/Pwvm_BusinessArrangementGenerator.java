package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Pwvm_BusinessArrangementGenerator{

	// Defining the four types of land use as contained in DLM.
	static final int RESIDENTIAL = 0;
	static final int INDUSTRIAL = 1;
	static final int MIXED = 2;
	static final int SPECIAL = 3;

	private boolean verbosemode = false;

	public void setVerbosemode(boolean verbosemode) {
		this.verbosemode = verbosemode;
	}

	/**
	 * Returns a code which represents the landuse type in DLM.
	 * @param landuse_mode The mode [1..4]
	 * @return a String representing the 4-digit number
	 * @throws Exception if code was wrong
	 */
	private String getLanduseCode(int landuse_mode) throws Exception{
		String landuse_code = "";
		switch (landuse_mode) {
		case RESIDENTIAL:
			landuse_code = "2111";
			break;
		case INDUSTRIAL:
			landuse_code = "2112";
			break;
		case MIXED:
			landuse_code = "2113";
			break;
		case SPECIAL:
			landuse_code = "2114";
			break;
		default:
			Exception e = new Exception("landuse_mode must be between 0..3");
		throw e;
		}
		return landuse_code;
	}

	/**
	 * Checks if a land use area of specific type exists in the KGS provided.
	 * @param landuse_code The land use code as used in DLM (4-digit code)
	 * @param current_kgs The KGS to which the search will be limited.
	 * @return True if one or more such areas exist, else false.
	 * @throws Exception 
	 */
	private boolean isAvailableInKgs(int landuse_mode, String current_kgs) throws Exception {
		Connection con = Pwvm_DatabaseConnection.dbconnect();
		Statement st = con.createStatement();
		String stmt = "SELECT 1 FROM gis_housecoordinate JOIN" +
		"(SELECT gis_dlm_fot.the_geom FROM gis_dlm_fot JOIN kgs ON ST_Covers(kgs.the_geom, gis_dlm_fot.the_geom)" +
		"WHERE objart = '"+getLanduseCode(landuse_mode)+"' " +
		"AND kgs = '"+current_kgs+"' " +
		") AS landuse " +
		"ON ST_Covers(landuse.the_geom, gis_housecoordinate.the_geom) " +
		"LIMIT 1";
		ResultSet rs = st.executeQuery(stmt);
		boolean found = rs.next();
		return found;
	}

	/**
	 * Arranges businesses in space. Requires a table pwvm_business as input,
	 * which must be located in the database specified in the connection
	 * string. The table should contain all businesses by KGS. Also required
	 * is a table named house_coordinates containing all buildings. A third
	 * requirement is the fot table of the DLM, which also needs to be located
	 * in the database.
	 * @param arrange_distribution percentages defining where to place businesses by landuse
	 */
	public void arrange(double[] arrange_distribution) {
		try {
			Connection con1 = Pwvm_DatabaseConnection.dbconnect();
			Connection con2 = Pwvm_DatabaseConnection.dbconnect();
			Connection con3 = Pwvm_DatabaseConnection.dbconnect();
			String stmt = "SELECT DISTINCT kgs FROM pwvm_business"; //  WHERE kgs = '11000000'
			if (verbosemode)
				System.out.println(stmt);
			Statement st = con1.createStatement();
			ResultSet kgs = st.executeQuery(stmt);

			while (kgs.next()) {
				String current_kgs = kgs.getString(1);
				stmt = "SELECT count(*) FROM pwvm_business WHERE kgs = '"+current_kgs+"'";
				if (verbosemode)
					System.out.println(stmt);
				Statement st2 = con2.createStatement();
				ResultSet rs = st2.executeQuery(stmt);
				rs.next();
				int total_number_of_businesses_in_kgs = rs.getInt(1);
				if (verbosemode)
					System.out.println(" -> "+total_number_of_businesses_in_kgs);
				int[] businesses_per_landusetype = new int[4]; 
				businesses_per_landusetype[RESIDENTIAL] = (int) (total_number_of_businesses_in_kgs * arrange_distribution[RESIDENTIAL]);
				businesses_per_landusetype[INDUSTRIAL] = (int) (total_number_of_businesses_in_kgs * arrange_distribution[INDUSTRIAL]);
				businesses_per_landusetype[MIXED] = (int) (total_number_of_businesses_in_kgs * arrange_distribution[MIXED]);
				businesses_per_landusetype[SPECIAL] = (int) (total_number_of_businesses_in_kgs * arrange_distribution[SPECIAL]);

				// Rundungsfehler aufsammeln und nachtraeglich vergeben
				int sum = 0;
				for (int i : businesses_per_landusetype) {
					sum+= i;
				}
				// errechnete differenz:
				int diff = total_number_of_businesses_in_kgs - sum;
				// erst versuchen dem INDUSTRIAL zuzuweisen
				if (businesses_per_landusetype[INDUSTRIAL] > 0) {
					businesses_per_landusetype[INDUSTRIAL] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else if (businesses_per_landusetype[MIXED] > 0) {
					businesses_per_landusetype[MIXED] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else if (businesses_per_landusetype[SPECIAL] > 0) {
					businesses_per_landusetype[SPECIAL] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else {
					businesses_per_landusetype[RESIDENTIAL] += diff;
				}

				if (verbosemode) {
					System.out.println("  -> RESIDENTIAL: "+businesses_per_landusetype[RESIDENTIAL]);
					System.out.println("  -> INDUSTRIAL: "+businesses_per_landusetype[INDUSTRIAL]);
					System.out.println("  -> MIXED: "+businesses_per_landusetype[MIXED]);
					System.out.println("  -> SPECIAL: "+businesses_per_landusetype[SPECIAL]);
				}

				int business_count_todo = businesses_per_landusetype[RESIDENTIAL];
				int landuse_mode = RESIDENTIAL;

				stmt = "SELECT id FROM pwvm_business " +
				"WHERE kgs = '"+current_kgs+"' " +
				"ORDER BY random()";
				if (verbosemode)
					System.out.println(stmt);
				ResultSet businesses = st2.executeQuery(stmt);

				while (businesses.next()) {
					String current_business = businesses.getString(1);

					if (business_count_todo == 0) {

						switch (landuse_mode) {
						case RESIDENTIAL:
							landuse_mode = INDUSTRIAL;
							business_count_todo = businesses_per_landusetype[INDUSTRIAL];
							break;
						case INDUSTRIAL:
							landuse_mode = MIXED;
							business_count_todo = businesses_per_landusetype[MIXED];
							break;
						case MIXED:
							landuse_mode = SPECIAL;
							business_count_todo = businesses_per_landusetype[SPECIAL];
							break;
						}

					}

					business_count_todo--;

					stmt = "UPDATE pwvm_business " +
					"SET house_coordinate=COALESCE((" +
					"SELECT s2 FROM gis_housecoordinate JOIN" +
					"(SELECT gis_dlm_fot.the_geom FROM gis_dlm_fot JOIN kgs ON ST_Covers(kgs.the_geom, gis_dlm_fot.the_geom)" +
					"WHERE objart = '"+getLanduseCode(landuse_mode)+"' " +
					"AND kgs = '"+current_kgs+"' " +
					") AS landuse " +
					"ON ST_Covers(landuse.the_geom, gis_housecoordinate.the_geom) " +
					"ORDER BY random() LIMIT 1" +
					"), " +
					"(SELECT s2 FROM gis_housecoordinate JOIN kgs " +
						"ON ST_Covers(kgs.the_geom, gis_housecoordinate.the_geom) " +
						"WHERE kgs = '"+current_kgs+"' ORDER BY random() LIMIT 1)" +
								") WHERE id='"+current_business+"'";
					if (verbosemode)
						System.out.println(stmt);
					Statement st3 = con3.createStatement();
					st3.executeUpdate(stmt);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}