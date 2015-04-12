package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Pwvm_DatabaseConnection {
	
//	private Connection conn;
//	private boolean printtoscreen = false;  // print SQL stmts to screen rather than db 	
//	public ResultSet resultSet;
	boolean verbosemode = false;
	
	
	public static Connection dbconnect() {

		try {
			Class.forName("org.postgresql.Driver");
//			String dburl = "jdbc:postgresql://129.247.221.172/pwvm";
			//String dburl = "jdbc:postgresql://localhost/pwvm";
			String dburl = "jdbc:postgresql://129.247.221.173:5433/pwvm";
			Properties props = new Properties();
			props.setProperty("user", "sebastian");
			props.setProperty("password", "sebastian");
			return DriverManager.getConnection(dburl, props);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

		/**
		 * Manages escape characters
		 * "'" is replaced with "\'"
		 * "\'" stays untouched
		 * same for '"' and '\"'
		 * @param text The variable to check
		 * @return the string with correct escape characters
		 */	
		public static String quote(String text) {
			
			if (text == null)
				return "null";
			
			String s = text.replaceAll("'", "\\\\'");
			s = s.replaceAll("\\\\{2}'", "\\\\'"); // '
			
			s = s.replaceAll("\\\"", "\\\\\"");
			s = s.replaceAll("\\\\{2}\\\"", "\\\\\""); // "
			
			s = "'" + s;
			s += "'";
			
			return s;
		}
		
		public static String quote(boolean b) {
			if (b == false)
				return "'0'";
			else
				return "'1'";	
		}
		
		public static String quote(int i) {
			Integer a = new Integer(i);
			return "'"+a.toString()+"'";
		}

		public void setVerbosemode(boolean verbosemode) {
			this.verbosemode = verbosemode;
		}

	}