package playground.gregor.grips.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.postgis.*;
import org.postgis.binary.BinaryParser;

public class PostGISTest {


	String driver     = "org.postgresql.Driver";
	String host       = "192.168.35.76";
	String port       = "5432";
	String database   = "gis";
	String user = "gisuser";
	String password = "NULL";

	String url = "jdbc:postgresql:" + (this.host != null ? ("//" + this.host) + (this.port != null ? ":" + this.port : "") + "/" : "") + this.database;
	Connection connection = null;

	public PostGISTest()
	{
		loadJdbcDriver();
		openConnection();
		testQuery();
		closeConnection();
	}

	private void closeConnection()
	{
		try
		{
			this.connection.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace ();
		}

	}

	private void loadJdbcDriver()
	{
		try
		{
			Class.forName(this.driver);
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace ();
		}

	}

	private void openConnection()
	{
		try
		{
			this.connection = DriverManager.getConnection (this.url, this.user, this.password);
		}
		catch (SQLException e)
		{
			e.printStackTrace ();
		}

	}

	private void testQuery()
	{
		try
		{
			//			((org.postgresql.PGConnection)this.connection).addDataType("geometry",Class.forName("org.postgis.PGgeometry"));
			//			((org.postgresql.PGConnection)this.connection).addDataType("box2d",Class.forName("org.postgis.PGbox2d"));
			//			((org.postgresql.PGConnection)this.connection).addDataType("box3d",Class.forName("org.postgis.PGbox3d"));
			Statement statement = this.connection.createStatement ();

			ResultSet resultSet = statement.executeQuery ("SELECT * FROM public.roadnode");
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData ();

			System.out.println (resultSetMetaData.getColumnLabel (1) + " " +
					resultSetMetaData.getColumnLabel (2) + " " +
					resultSetMetaData.getColumnLabel (3));

			BinaryParser bp = new BinaryParser();

			while (resultSet.next ())
			{
				System.out.println (resultSet.getString (1) + " " + resultSet.getString (2) + " " + resultSet.getString (3) + resultSet.getString (4) + " " + resultSet.getString (5) + " " + resultSet.getString (6));
				Geometry ggg = bp.parse(resultSet.getString(2));
				if (ggg instanceof Point) {
					Point p = (Point)ggg;
					System.out.print("dim = " + p.getDimension() + " ");
				}

				System.out.println(ggg);
			}

			resultSet.close ();
			statement.close ();
		}
		catch (SQLException e)
		{
			e.printStackTrace ();
		}
	}





	public static void main(String[] args)
	{

		new PostGISTest();
	}


}
