package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Pwvm_HouseholdArrangementGenerator{

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
	 * Arranges households in space. Requires a table pwvm_households as input,
	 * which must be located in the database specified in the connection
	 * string. The table should contain all households by TVZ (Teilverkehrszelle).
	 * Also required is a table named house_coordinates containing all buildings.
	 * A third requirement is the fot table of the DLM, which also needs to be
	 * located in the database.
	 * @param arrange_distribution percentages defining where to place households
	 * by landuse
	 */
	public void arrange(double[] arrange_distribution) {
		try {
			Connection con1 = Pwvm_DatabaseConnection.dbconnect();
			Connection con2 = Pwvm_DatabaseConnection.dbconnect();
			Connection con3 = Pwvm_DatabaseConnection.dbconnect();
			String stmt = "SELECT DISTINCT tvz FROM pwvm_household";
			if (verbosemode)
				System.out.println(stmt);
			Statement st = con1.createStatement();
			ResultSet kgs = st.executeQuery(stmt);

			while (kgs.next()) {
				String current_tvz = kgs.getString(1);
				stmt = "SELECT count(*) FROM pwvm_household WHERE tvz = '"+current_tvz+"'";
				if (verbosemode)
					System.out.println(stmt);
				Statement st2 = con2.createStatement();
				ResultSet rs = st2.executeQuery(stmt);
				rs.next();
				int total_number_of_households_in_tvz = rs.getInt(1);
				if (verbosemode)
					System.out.println(" -> "+total_number_of_households_in_tvz);
				int[] households_per_landusetype = new int[4]; 
				households_per_landusetype[RESIDENTIAL] = (int) (total_number_of_households_in_tvz * arrange_distribution[RESIDENTIAL]);
				households_per_landusetype[INDUSTRIAL] = (int) (total_number_of_households_in_tvz * arrange_distribution[INDUSTRIAL]);
				households_per_landusetype[MIXED] = (int) (total_number_of_households_in_tvz * arrange_distribution[MIXED]);
				households_per_landusetype[SPECIAL] = (int) (total_number_of_households_in_tvz * arrange_distribution[SPECIAL]);

				// Rundungsfehler aufsammeln und nachtraeglich vergeben
				int sum = 0;
				for (int i : households_per_landusetype) {
					sum+= i;
				}
				// errechnete differenz:
				int diff = total_number_of_households_in_tvz - sum;
				// erst versuchen dem RESIDENTIAL zuzuweisen
				if (households_per_landusetype[RESIDENTIAL] > 0) {
					households_per_landusetype[RESIDENTIAL] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else if (households_per_landusetype[MIXED] > 0) {
					households_per_landusetype[MIXED] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else if (households_per_landusetype[SPECIAL] > 0) {
					households_per_landusetype[SPECIAL] += diff;
				} // falls dort 0 lieber woanders versuchen	
				else {
					households_per_landusetype[INDUSTRIAL] += diff;
				}

				if (verbosemode) {
					System.out.println("  -> RESIDENTIAL: "+households_per_landusetype[RESIDENTIAL]);
					System.out.println("  -> INDUSTRIAL: "+households_per_landusetype[INDUSTRIAL]);
					System.out.println("  -> MIXED: "+households_per_landusetype[MIXED]);
					System.out.println("  -> SPECIAL: "+households_per_landusetype[SPECIAL]);
				}
				
				int household_count_todo = households_per_landusetype[RESIDENTIAL];
				int landuse_mode = RESIDENTIAL;

				stmt = "SELECT idhh FROM pwvm_household " +
				"WHERE tvz = '"+current_tvz+"' " +
				"ORDER BY random()";
				if (verbosemode)
					System.out.println(stmt);
				ResultSet households = st2.executeQuery(stmt);

				while (households.next()) {
					String current_household = households.getString(1);

					if (household_count_todo == 0) {

						switch (landuse_mode) {
						case RESIDENTIAL:
							landuse_mode = INDUSTRIAL;
							household_count_todo = households_per_landusetype[INDUSTRIAL];
							break;
						case INDUSTRIAL:
							landuse_mode = MIXED;
							household_count_todo = households_per_landusetype[MIXED];
							break;
						case MIXED:
							landuse_mode = SPECIAL;
							household_count_todo = households_per_landusetype[SPECIAL];
							break;
						}

					}

					household_count_todo--;

					stmt = "UPDATE pwvm_household " +
					"SET house_coordinate=COALESCE((" +
					"SELECT s2 FROM gis_housecoordinate JOIN" +
					"(SELECT gis_dlm_fot.the_geom FROM gis_dlm_fot JOIN gis_teilverkehrszelle " +
					"ON ST_Covers(gis_teilverkehrszelle.the_geom, gis_dlm_fot.the_geom)" +
					"WHERE objart = '"+getLanduseCode(landuse_mode)+"' " +
					"AND gis_teilverkehrszelle.tvz = '"+current_tvz+"' " +
					") AS landuse " +
					"ON ST_Covers(landuse.the_geom, gis_housecoordinate.the_geom) " +
					"ORDER BY random() LIMIT 1" +
					"), " +
					"(SELECT s2 FROM gis_housecoordinate JOIN gis_teilverkehrszelle " +
						"ON ST_Covers(gis_teilverkehrszelle.the_geom, gis_housecoordinate.the_geom) " +
						"WHERE gis_teilverkehrszelle.tvz = '"+current_tvz+"' ORDER BY random() LIMIT 1)" +
								") WHERE idhh='"+current_household+"'";
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