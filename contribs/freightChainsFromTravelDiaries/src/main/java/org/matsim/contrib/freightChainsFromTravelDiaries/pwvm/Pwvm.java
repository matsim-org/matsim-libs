package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/* ==== OGC ====
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;

 import org.geotools.data.DataStore;
 import org.geotools.data.DataStoreFinder;
 import org.geotools.data.DefaultQuery;
 import org.geotools.data.FeatureSource;
 import org.geotools.data.DefaultQuery;
 import org.geotools.feature.AttributeType;
 import org.geotools.feature.Feature;
 import org.geotools.feature.FeatureCollection;
 import org.geotools.feature.FeatureIterator;
 import org.geotools.feature.FeatureType;
 import org.geotools.filter.FilterFactoryFinder;
 import org.opengis.filter.expression.Expression;
 import org.opengis.filter.Filter;
 import org.opengis.filter.FilterFactory;
 */

public class Pwvm {

	private static final String SCENARIO_IDENTIFIER = "-scen";
	private static final String LOGBOOKREPOSITORY = "-logbook";
	private static final String ARRANGE = "-arrange";
	private static final String SIMULATE = "-simulate";
	private static final String SAMPLE = "-sample";
	private static final String PRINTMATRIX = "-printmatrix";
	private static final String PRINTMATSIMXML = "-printMatsimXML";
	private static final String LBID = "-lb";
	private static final String FILENAME = "-filename";
	private static final String SERVICETRAFFIC = "-servicetraffic";
	private static final String BUSINESS = "b";
	private static final String HOUSEHOLD = "h";
	private static final String VERBOSE = "-v";
	private static final String MOREVERBOSE = "-vv";


	// Defining the four types of land use as contained in DLM.
	static final int RESIDENTIAL = 0;
	static final int INDUSTRIAL = 1;
	static final int MIXED = 2;
	static final int SPECIAL = 3;

	public static void main(String[] args) {

		String scenario_identifier       = "";
		double randomSampleRatio         = 1.0;
		boolean createLogBooksFromKiD	 = false;
		boolean arrangeBusinesses		 = false;
		boolean arrangeHouseholds		 = false;
		boolean printMatrix				 = false;
		boolean printMatsimXML		 = false;
		int logbookIdToExport = -1;
		String exportFilename			 = "";
		boolean exportServicetrafficOnly = false;
		boolean runSimulation 			 = false;
		boolean verbosemode    			 = false;
		boolean bemoreverbose  			 = false;	// not implemented yet
		double[] arrange_distribution    = new double[4];
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(SCENARIO_IDENTIFIER)) {
				i++;
				if (i >= args.length) {
					usage();
				}
				scenario_identifier = args[i];
			} else if (args[i].equals(ARRANGE)) {
				i++;
				if ((i < args.length) && (args[i].compareToIgnoreCase(HOUSEHOLD) == 0) | (args[i].compareToIgnoreCase(BUSINESS) == 0)) {
					if (args[i].compareToIgnoreCase(HOUSEHOLD) == 0)
						arrangeHouseholds = true;
					if (args[i].compareToIgnoreCase(BUSINESS) == 0)
						arrangeBusinesses = true;
				} else
					usage();
				i++;
				try {
					Double d = Double.parseDouble(args[i]);
					Double sum = d;
					arrange_distribution[RESIDENTIAL] = d;					
					i++;
					d = Double.parseDouble(args[i]);
					sum += d;
					arrange_distribution[INDUSTRIAL] = d;
					i++;
					d = Double.parseDouble(args[i]);
					sum += d;
					arrange_distribution[MIXED] = d;
					i++;
					d = Double.parseDouble(args[i]);
					sum += d;
					arrange_distribution[SPECIAL] = d;

					if (sum != 1) {
						System.out.println("The sum should be equal to 1.0");
						System.exit(-1);
					}				

				} catch (Exception e) {
					e.printStackTrace();
					usage();
				}
			} else if (args[i].equals(SIMULATE)) {
				runSimulation = true;
			} else if (args[i].equals(SAMPLE)) {
				i++;
				if (i >= args.length) {
					usage();
				}
				randomSampleRatio = Double.valueOf(args[i]);
			} else if (args[i].equals(LOGBOOKREPOSITORY)) {
				createLogBooksFromKiD = true;
			} else if (args[i].equals(PRINTMATSIMXML)) {
				printMatsimXML = true;
				i++;
				if (i < args.length) {
					if (args[i].equals(LBID)) {
						i++;
						if (i >= args.length) {
							usage();
						}
						logbookIdToExport = Integer.parseInt(args[i]);
					} else {
						i--;
					}
				} else
					i--;
				i++;
				if (i < args.length) {
					if (args[i].equals(FILENAME)) {
						i++;
						if (i >= args.length) {
							usage();
						}
						exportFilename = args[i];
					} else {
						i--;
					}
				} else
					i--;
			} else if (args[i].equals(PRINTMATRIX)) {
				printMatrix = true;
				i++;
				if (i < args.length) {
					if (args[i].equals(FILENAME)) {
						i++;
						if (i >= args.length) {
							usage();
						}
						exportFilename = args[i];
					} else {
						i--;
					}
				} else
					i--;
			} else if (args[i].equals(SERVICETRAFFIC)) {
			    exportServicetrafficOnly = true;
			} else if (args[i].equals(VERBOSE)) {
				verbosemode = true;
			} else if (args[i].equals(MOREVERBOSE)) {
				verbosemode = true;
				bemoreverbose = true;
			}
		}

//		if (scenario_identifier == "") {
//			System.out.println("Which scenario to work with?");
//			usage();
//			System.exit(-1);
//		}


		if (arrangeBusinesses) {
			System.out.println("Arranging businesses...");
			Pwvm_BusinessArrangementGenerator bg = new Pwvm_BusinessArrangementGenerator();
			bg.setVerbosemode(verbosemode);
			bg.arrange(arrange_distribution);
		} else if (arrangeHouseholds) {
			System.out.println("Arranging households...");
			Pwvm_HouseholdArrangementGenerator hg = new Pwvm_HouseholdArrangementGenerator();
			hg.setVerbosemode(verbosemode);
			hg.arrange(arrange_distribution);
		}
		
		if (createLogBooksFromKiD) {
			System.out.println("Generating logbook repository...");
			Pwvm_LogbookRepositoryGenerator lrg = new Pwvm_LogbookRepositoryGenerator();
			lrg.setVerbosemode(verbosemode);
			lrg.generate();
			System.out.println("Done.");
		}
		
		if (runSimulation) {
			System.out.println("Starting simulation...");
			Pwvm_Simulator sim = new Pwvm_Simulator();
			sim.setVerbosemode(verbosemode);
			sim.setRandomSampleRatio(randomSampleRatio);
			sim.start();
		}
		
		if (printMatsimXML) {
			printMatsimXMLFromDatabase(exportFilename, logbookIdToExport);
		}
		
		if (printMatrix) {
			printMatrixFromDatabase(exportFilename, exportServicetrafficOnly);
		}
		
		if (!(arrangeBusinesses | arrangeHouseholds | createLogBooksFromKiD | runSimulation | printMatsimXML | printMatrix))
			usage();

	}


  /**
	 * Exports as Visum file
	 * @param filename writes to the filename specified. "" for output to console.
	 * @param logbookIdToExport the id of the logbook, or -1 for all logbooks.
	 */

	private static void printMatrixFromDatabase(String filename, boolean exportServiceTrafficOnly) {

		String s = "";
		if (exportServiceTrafficOnly)
			s = " AND purpose IN (2, 3) ";
		
		String stmt = "SELECT von||' '||nach||' '||COALESCE(fahrten, 0) FROM "+
		"( SELECT c.no AS von, c2.no AS nach, (SELECT count(*) FROM pwvm_matrix WHERE ST_Within(source_geom, c.the_geom) AND ST_Within(dest_geom, c2.the_geom) "+s+") as fahrten "+
		" FROM visumbezirk c, visumbezirk c2 ) AS matrix "+
		"ORDER BY von ASC, nach ASC";
		
		Connection con = Pwvm_DatabaseConnection.dbconnect();
		Statement st;
		try {
			st = con.createStatement();

			ResultSet rs = st.executeQuery(stmt);
			
			if (!filename.equals("")) {
				
				
				
			  if (!filename.endsWith(".mtx"))
				  filename += ".mtx";
			  
			  System.out.println("Exporting matrix into file "+filename+"...");
			  
		      File f = new File(filename); 
		      FileWriter fw = new FileWriter(f); 
		      BufferedWriter bw = new BufferedWriter(fw); 
		      
		      bw.write("$O;Y5");
	    	  bw.newLine();
	    	  bw.write("*");
	    	  bw.newLine();
	    	  bw.write("*  Zeitintervall");
	    	  bw.newLine();
	    	  bw.write("   0.00   0.00");
	    	  bw.newLine();
	    	  bw.write("*  Faktor");
	    	  bw.newLine();
	    	  bw.write("   1.000000");
	    	  bw.newLine();
	    	  bw.write("*              Quell                Ziel         Anzahl");
	    	  bw.newLine();
	    	  bw.write("*               Bez                 Bez         Fahrten");
	    	  bw.newLine();
	    	  bw.newLine();
	    	  
		      while (rs.next()) {	  
		    	  bw.write(rs.getString(1));
		    	  bw.newLine();
			  }
		      bw.close();
		      System.out.println("DONE EXPORT.");
			} else {
				System.out.println("$O;Y5");
				System.out.println("*");
				System.out.println("*  Zeitintervall");
				System.out.println("   0.00   0.00");
				System.out.println("*  Faktor");
				System.out.println("   1.000000");
				System.out.println("*              Quell                Ziel         Anzahl");  
				System.out.println("*               Bez                 Bez         Fahrten");
				System.out.println("");
				while (rs.next()) {
					System.out.println(rs.getString(1));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	/**
	 * Generates Matsim XML
	 * @param filename writes to the filename specified. "" for output to console.
	 * @param logbookIdToExport the id of the logbook, or -1 for all logbooks.
	 */
	private static void printMatsimXMLFromDatabase(String filename, int logbookIdToExport) {

		String stmt = "SELECT "
		+ "business_id, " 
		+ "purpose, "
		+ "ST_X(ST_Transform(source_geom, 4326)) AS source_x, "
		+ "ST_Y(ST_Transform(source_geom, 4326)) AS source_y, "
		+ "ST_X(ST_Transform(dest_geom, 4326)) AS dest_x, "
		+ "ST_Y(ST_Transform(dest_geom, 4326)) AS dest_y, "
		+ "vehicletype, "
		+ "logbook_id, "
		+ "trip_id, "
		+ "destinationtype, "
		+ "start_time, "
		+ "stop_time "
  		+ "FROM pwvm_matrix";

  		if(logbookIdToExport != -1)
  			stmt += " WHERE logbook_id = "+logbookIdToExport;
  		
  		stmt += " ORDER BY logbook_id ASC, trip_id ASC";
  		
  		System.out.println(stmt);
  		
//		+ "LIMIT 100";
		
		String source_geom = "";
		String dest_geom = "";
		String logbook_id = "";
		String trip_id = "";
		String destinationtype = "";
		String start_time = "";
		String stop_time = "";
			
		Connection con = Pwvm_DatabaseConnection.dbconnect();
		Statement st;
		try {
			st = con.createStatement();

			ResultSet rs = st.executeQuery(stmt);
			
			if (!filename.equals("")) {
	
			  if (!filename.endsWith(".xml"))
				  filename += ".xml";
			  
			  System.out.println("Generating Matsim XML into file "+filename+"...");
			  
		      File f = new File(filename); 
		      FileWriter fw = new FileWriter(f); 
		      BufferedWriter bw = new BufferedWriter(fw); 
		      
		      bw.write("<plans name=\"plans file\" xml:lang=\"de-DE\">");
		      bw.newLine();
		
		      boolean theFirst = true;
	   
		      while (rs.next()) {
		    	  
		    	  source_geom = "x=\""+rs.getString("source_x")+"\" y=\""+rs.getString("source_y")+"\"";
		    	  dest_geom = "x=\""+rs.getString("dest_x")+"\" y=\""+rs.getString("dest_y")+"\"";
		    	  logbook_id = rs.getString("logbook_id");
		    	  trip_id = rs.getString("trip_id");
		    	  destinationtype = rs.getString("destinationtype");
		    	  start_time = rs.getString("start_time");
		    	  stop_time = rs.getString("stop_time");
		    	  
		    	  if (trip_id.equals("1")) {
		    		  // es handelt sich um den ersten Trip eines Logbooks
		    		  
		    		  if (!theFirst) {
		    			  // es beginnt ein neues Logbook, daher das vorherige zunaechst abschliessen
		    			  bw.write("</plan>");
					      bw.newLine();
					      bw.write("</person>");
					      bw.newLine();
		    		  }
		    		  
		    		  theFirst = false;
		    	  
		    		  // Nun das neue beginnen
		    		  bw.write("<person id=\""+logbook_id+"\">");
		    		  bw.newLine();
		    		  bw.write("<plan selected=\"yes\">");
		    		  bw.newLine();
		    		  bw.write("<act type=\"home\" "+source_geom+" start_time=\"00:00:00\" end_time=\""+start_time+"\" />");
		    		  bw.newLine();

		    	  }

		    	  //  fahrt ausgeben und danach das ziel als neuen wegpunkt ausgeben

		    	  bw.write("<leg mode=\"car\" dep_time=\""+start_time+"\" arr_time=\""+stop_time+"\" />");
		    	  bw.newLine();

		    	  bw.write("<act type=\"home\" "+dest_geom+" start_time=\""+stop_time+"\" />");
		    	  bw.newLine();
		      
			      //<act type="home" x="4593596.929982451" y="5822971.047469956" start_time="00:00:00" dur="15:06:05" end_time="15:06:05" />
			      
			      //bw.write("<leg mode="car" dep_time="15:06:05" trav_time="00:19:00" arr_time="15:25:05" />");
			      
			      //</leg>
			      //<act type="leisure" x="4595737.336871209" y="5823738.71255535" start_time="15:25:05" end_time="18:18:28" />
			      //<leg mode="car" dep_time="18:18:28" trav_time="00:19:00" arr_time="18:37:28">
			      //</leg>
			      //<act type="home" x="4593596.929982451" y="5822971.047469956" start_time="18:37:28" />
	    	  
			  }
		      
		      // abschliessen
		      bw.write("</plan>");
		      bw.newLine();
		      bw.write("</person>");
		      bw.newLine();
		      bw.write("</plans>");
		      bw.newLine();
		      
		      bw.close();
		      System.out.println("DONE EXPORT.");
		      
			} else {

				System.out.println("<plans name=\"plans file\" xml:lang=\"de-DE\">");

				boolean theFirst = true;

				while (rs.next()) {

					source_geom = "x=\""+rs.getString("source_x")+"\" y=\""+rs.getString("source_y")+"\"";
					dest_geom = "x=\""+rs.getString("dest_x")+"\" y=\""+rs.getString("dest_y")+"\"";
					logbook_id = rs.getString("logbook_id");
					trip_id = rs.getString("trip_id");
					destinationtype = rs.getString("destinationtype");
					start_time = rs.getString("start_time");
					stop_time = rs.getString("stop_time");

					if (trip_id.equals("1")) {
						// es handelt sich um den ersten Trip eines Logbooks

						if (!theFirst) {
							// es beginnt ein neues Logbook, daher das vorherige zunaechst abschliessen
							System.out.println("    </plan>");
							System.out.println("  </person>"); 
						}

						theFirst = false;
						// Nun das neue beginnen
						System.out.println("  <person id=\""+logbook_id+"\">");
						System.out.println("    <plan selected=\"yes\">");
						System.out.println("      <act type=\"home\" "+source_geom+" start_time=\"00:00:00\" end_time=\""+start_time+"\" />");

					}

					//  fahrt ausgeben und danach das ziel als neuen wegpunkt ausgeben
					System.out.println("        <leg mode=\"car\" dep_time=\""+start_time+"\" arr_time=\""+stop_time+"\" />");
					System.out.println("        <act type=\"home\" "+dest_geom+" start_time=\""+stop_time+"\" />");

				}

				// abschliessen
				System.out.println("    </plan>");
				System.out.println("  </person>");     
				System.out.println("</plans>");

	
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	

	/**
	 * This will print the usage requirements and exit.
	 */
	private static void usage() {
		System.err
		.println("Usage: java Pwvm [-arrange (b|h) x1 x2 x3 x4] [-logbookrep] [-simulate [-sample x]] [-printMatsimXML [-lb x] [-filename FILENAME]] [-printmatrix [-filename FILENAME]] [-v | -vv]\n"
				+ "  -scen name   Specifiy a scenario identifier\n"
				+ "  -arrange b|h X1 X2 X3 X4\n"
				+ "               (Re-)Arranges businesses (b) or households (h) with the specified landuse-distribution,\n"
				+ "               where X1 is the percentage for RESIDENTIAL,\n"
				+ "               X2 is the percentage for INDUSTRIAL,\n"
				+ "               X3 is the percentage for MIXED,\n"
				+ "               X4 is the percentage for SPECIAL.\n"
				+ "  -logbookrep  Generate logbook repository from KiD\n"
				+ "\n"
				+ "  -simulate    Run simulation\n"
				+ "  -sample x    optionally limit the simulation to a random sample of x percent of businesses\n"
				+ "\n"
				+ "  -printMatsimXML\n"
				+ "               Prints all records as Matsim XML to standard out\n"
				+ "               or to a file if the filename is provided with the -filename option.\n"
				+ "  -lb <id>     Only export the logbook of the given id into XML. If not specified, all logbooks will be exported.\n"
				+ "\n"
				+ "  -printmatrix Prints the matrix in Visum format to standard out\n"
				+ "               or to a file if the filename is provided with the -filename option.\n"
				+ "  -filename FILENAME\n"
				+ "               Specifies a filename for the trip output.\n"
				+ "               (for visum matrices, \".mtx\" is automatically added if not provided manually.)\n"
				+ "               (for xml files, \".xml\" is automatically added if not provided manually.)\n"
				+ "  -servicetraffic\n"
				+ "               Only export service traffic trips, all others are filtered out (ignored for XML export).\n"
				+ "\n"
				+ "  -v           Verbose mode\n"
				+ "  -vv          Be more verbose (not implemented at this time)\n"
				+ "\n"
				+ "Examples: java Pwvm -arrange b 0.7 0.15 0.15 0 -v\n"
				+ "          java Pwvm -simulate\n"
				+ "          java Pwvm -simulate -sample 0.1\n");
		System.exit(1);
	}

}