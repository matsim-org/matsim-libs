package playground.mzilske.deteval;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class CountsFromMunichToDatabase {

	// 2005: Ab 18.1. wie 2006, davor was anderes
	// 2006: Intervall 18, Menge 19
	// 2008: Intervall 20, Menge 21
	
	private static final class DetektorLog2008Parser implements
	TabularFileHandler {
		private final int detektor;
		private final PreparedStatement statement;
		private String date;
		private SimpleDateFormat dateFormat;

		private DetektorLog2008Parser(String date, int detektor, PreparedStatement statement) {
			this.date = date;
			this.detektor = detektor;
			this.statement = statement;
			this.dateFormat = new SimpleDateFormat("yyyyMMdd");
		}

		public void startRow(String[] row) {
			// System.out.println(row[0] + " "+ row[19] + " "+row[20]);
			try {
				statement.setInt(1, Integer.parseInt(row[0]));
				statement.setInt(2, Integer.parseInt(row[19]));
				statement.setDouble(3, Double.parseDouble(row[20]));
				statement.setInt(4, detektor);
				statement.setDate(5, new java.sql.Date(dateFormat.parse(date).getTime()));
				statement.addBatch();
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class DetektorLogOldParser implements
	TabularFileHandler {
		private final int detektor;
		private final PreparedStatement statement;
		private String date;
		private SimpleDateFormat dateFormat;

		private DetektorLogOldParser(String date, int detektor, PreparedStatement statement) {
			this.detektor = detektor;
			this.statement = statement;
			this.date = date;
			this.dateFormat = new SimpleDateFormat("yyyyMMdd");
		}

		public void startRow(String[] row) {
			// System.out.println(row[0] + " "+ row[19] + " "+row[20]);
			try {
				statement.setInt(1, Integer.parseInt(row[0]));
				statement.setInt(2, Integer.parseInt(row[17]));
				statement.setDouble(3, Double.parseDouble(row[18]));
				statement.setInt(4, detektor);
				statement.setDate(5, new java.sql.Date(dateFormat.parse(date).getTime()));
				statement.addBatch();
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static Collection<String> basePathsOld = Arrays.asList(
			"/Users/michaelzilske/workspace/detailedEval/eingangsdaten/Detektoren/2005/",
			"/Users/michaelzilske/workspace/detailedEval/eingangsdaten/Detektoren/2006/");

	static Collection<String> basePaths2008 = Arrays.asList(
			"/Users/michaelzilske/workspace/detailedEval/eingangsdaten/Detektoren/2008/");

	public static void main(String[] args) throws Exception {
		for (String basePath : basePathsOld) {
			boolean old = true;
			importCounts(basePath, old);
		}
		for (String basePath : basePaths2008) {
			boolean old = false;
			importCounts(basePath, old);
		}
	}

	private static void importCounts(String basePath, boolean old) throws SQLException,
	IOException {
		File baseDir = new File(basePath);
		for (File dir : baseDir.listFiles()) {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					Pattern p = Pattern.compile("(\\d*)\\.txt");
					Matcher m = p.matcher(file.getName());
					if (m.matches()) {
						int detektor = Integer.parseInt(m.group(1));
						System.out.println("date: " + dir.getName() + " detektor: "+detektor);
						insertCounts(file.getAbsolutePath(), detektor, old, dir.getName());
					}
				}
			}
		}
	}

	private static void insertCounts(String string, final int detektor, boolean old, String date) throws SQLException,
	IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(string);
		tabFileParserConfig.setDelimiterTags(new String[] { ";" });
		tabFileParserConfig.setCommentTags(new String[] { "#" });
		Connection connection = DriverManager.getConnection(
				"jdbc:postgresql:munich", "postgres", "postgres"); // connect to
		// the db
		DatabaseMetaData dbmd = connection.getMetaData(); // get MetaData to
		// confirm
		// connection
		System.out.println("Connection to " + dbmd.getDatabaseProductName()
				+ " " + dbmd.getDatabaseProductVersion() + " successful.\n");
		final PreparedStatement statement = connection.prepareStatement("insert into counts values (?, ?, ?, ?, ?)"); 
		if (old) {
			new TabularFileParser().parse(tabFileParserConfig,
					new DetektorLogOldParser(date, detektor, statement));
		} else {
			new TabularFileParser().parse(tabFileParserConfig,
					new DetektorLog2008Parser(date, detektor, statement));
		}
		statement.executeBatch();
	}
}
