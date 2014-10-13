package playground.pieter.singapore.hits;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HITSIndexingProcedures {

	/**
	 * @param args
	 */
	private final Connection conn;
	
	private HITSIndexingProcedures(Connection conn) {
		super();
		this.conn = conn;
	}

	private void createHITS2TypeIndexCrossRef() throws SQLException{
		Statement s = conn.createStatement();
		Statement s2 = conn.createStatement();
		// create a lookup table of hits types and type indices
		s.execute("drop table if exists hits_2_type_index;");
		s.execute("create table hits_2_type_index (hits_type VARCHAR(45) NULL ," +
				" type_index INT NULL );");

		s.executeQuery("select * from hits_placetypes ;");
		ResultSet rs = s.getResultSet();
		while(rs.next()){
			String hp = rs.getString(1);

			String sqlString = "insert into hits_2_type_index "
					+ "select "
					+ "\'" + hp + "\',"
					+ " type_index from real_estate_xref where hits_type like \'%" + hp
					+ "%\';";
			s2.execute(sqlString);
		}
		s.close();
		s2.execute("ALTER TABLE hits_2_type_index ADD INDEX ht (hits_type ASC), ADD INDEX ti (type_index ASC) ;");
		s2.execute("drop table if exists hits2type2zip;");
		s2.execute("CREATE TABLE hits2type2zip SELECT hits_type, type_index, zip FROM `hits`.`hits_2_type_index` natural join type_index_x_zip;");
		s2.execute("ALTER TABLE hits2type2zip ADD INDEX ht (hits_type ASC), ADD INDEX ti (type_index ASC), ADD INDEX zi (zip ASC)  ;");
		
		//now, put it all together
		s2.execute("drop table if exists hits2type2zip2dgp;");
		s2.execute("CREATE TABLE hits2type2zip2dgp SELECT * FROM hits2type2zip natural join pcodes_zone_xycoords where DGP is not null;");
		s2.execute("ALTER TABLE hits2type2zip2dgp ADD INDEX ht (hits_type ASC), ADD INDEX ti (type_index ASC), ADD INDEX zi (zip ASC)  , ADD INDEX gi (DGP ASC);");
		s2.close();
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		Connection conn = null;
		String userName = "root";
		String password = "kr";
		String url = "jdbc:mysql://localhost/hits";
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection(url, userName, password);
		System.out.println("Database connection established");
		
		HITSIndexingProcedures hip = new HITSIndexingProcedures(conn);
		hip.createHITS2TypeIndexCrossRef();
	}

}
