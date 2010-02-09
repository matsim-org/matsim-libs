package playground.pieter.counts;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.matsim.utils.io.IOUtils;

import corejava.Format;

public class CountsFromSQL {
	/**
	 * @param args
	 */
	private Connection sqlConn;
	private String xmlString;

	public CountsFromSQL() {
		super();
		makeConnection();
		xmlString = "";
	}

	private void makeConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.sqlConn = DriverManager.getConnection(
					"jdbc:mysql://localhost/countstations", "root", "kr");
		} catch (Exception sqlEx) {
			System.err.println(sqlEx);
		}
	}

	public static void main(String[] args) throws SQLException {
		CountsFromSQL test = new CountsFromSQL();
		test.run();
		test.closeConnection();

	}

	private void run() {
		// TODO Auto-generated method stub
		Statement myStmt;
		xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		xmlString+="<counts xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
		"xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/counts_v1.xsd\"\n";
		xmlString+="name=\"eThekwini Counts 2001\" \n" +
		"desc=\"SANRAL vehicle counts for eThekwini\" year=\"2001\" layer=\"0\">\n\n";
		try {

			myStmt = this.sqlConn.createStatement();
			ResultSet result = myStmt.executeQuery
			("select concat(sid,\"_\",d1,\"_\",descd1) as sitedesc, linkd1 as link, hr, " +
					"avd1tot as totcount, avd1lite as litecount" +
			" from countsout where linkd1 is not null order by sitedesc, hr");
			readCounts(result);
			result = myStmt.executeQuery
			("select concat(sid,\"_\",d2,\"_\",descd2) as sitedesc, linkd2 as link, hr, " +
					"avd2tot as totcount, avd2lite as litecount" +
			" from countsout where linkd2 is not null order by sitedesc, hr");
			readCounts(result);
			xmlString+="</count>\n</counts>";
		} catch (Exception sqlEx) {
			// System.err.println(sqlEx);
		}
		try {
			BufferedWriter output = IOUtils.getBufferedWriter("southafrica/IPDM_ETH_EmmeMOD/counts2001.xml");
			output.write(xmlString);
			output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void closeConnection() throws SQLException {
		this.sqlConn.close();
	}
	private void readCounts(ResultSet result) throws SQLException {
		result.next();
		String siteID = result.getString("sitedesc");
		int linkID = result.getInt("link");
		xmlString += "<count loc_id=\"" + linkID + "\" " + "cs_id=\"" + siteID
		+ "\">\n";
		xmlString += String.format("\t<volume h=\"%d\" val=\"%d\"/>\n", result
				.getInt("hr"), result.getInt("litecount"));

		while (result.next()) {
			try {
				if (result.getString("sitedesc").equals(siteID)) {
					xmlString += String.format(
							"\t<volume h=\"%d\" val=\"%d\"/>\n", result
							.getInt("hr"), result.getInt("litecount"));
				} else {
					xmlString += "</count>\n";
					siteID = result.getString("sitedesc");
					linkID = result.getInt("link");
					xmlString += "<count loc_id=\"" + linkID + "\" "
					+ "cs_id=\"" + siteID + "\">\n";
					xmlString += String.format(
							"\t<volume h=\"%d\" val=\"%d\"/>\n", result
							.getInt("hr"), result.getInt("litecount"));
				}

			} catch (Exception sqlEx) {
				System.err.println("Hotdamn!");
			}

		}
	}

}
