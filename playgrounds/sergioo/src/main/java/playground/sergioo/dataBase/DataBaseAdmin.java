package playground.sergioo.dataBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author Sergio Ordóñez
 */
public class DataBaseAdmin {
	
	//Attributes
	/**
	 * The data base connection
	 */
	private Connection connection;
	
	//Methods
	/**
	 * Constructs a database administrator without a connection
	 */
	public DataBaseAdmin() {
		
	}
	/**
	 * Constructs a database administrator according to a properties file
	 * @param file The properties file for the connection. The file is well defined
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	public DataBaseAdmin(File file) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(file));
		Class.forName(properties.getProperty("driver")).newInstance();
		connection = DriverManager.getConnection(properties.getProperty("url"), properties.getProperty("userName"), properties.getProperty("password"));
	}
	/**
	 * Connects the administrator from the database according to a properties file
	 * @param file The properties file for the connection. The file is well defined
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SQLException 
	 */
	public void connect(File file) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {		
		FileInputStream in = new FileInputStream(file);
		Properties properties = new Properties();
		properties.load(in);
		Class.forName(properties.getProperty("driver")).newInstance();
		connection = DriverManager.getConnection(properties.getProperty("url"), properties.getProperty("userName"), properties.getProperty("password"));
	}
	/**
	 * Disconnects the administrator from the database
	 * @throws SQLException 
	 * @throws NoConnectionException 
	 */
	public void disconnect() throws SQLException, NoConnectionException {		
		if (connection != null)
			connection.close();
		throw new NoConnectionException();
	}
	/**
	 * Executes an SQL statement
	 * @param statement In SQL language to be executed
	 * @throws SQLException 
	 * @throws NoConnectionException 
	 */
	public void executeStatement(String statement) throws SQLException, NoConnectionException {
		if (connection != null) {
			Statement st = connection.createStatement();
			st.execute(statement);
		}
		else
			throw new NoConnectionException();
	}
	/**
	 * Executes an SQL update
	 * @param statement In SQL language to be executed
	 * @throws SQLException 
	 * @throws NoConnectionException 
	 */
	public void executeUpdate(String statement) throws SQLException, NoConnectionException {
		if (connection != null) {
			Statement st = connection.createStatement();
			st.executeUpdate(statement);
		}
		else
			throw new NoConnectionException();
	}
	/**
	 * Executes an SQL query
	 * @param query In SQL language to be consulted
	 * @return The result set of the query
	 * @throws SQLException 
	 * @throws NoConnectionException 
	 */
	public ResultSet executeQuery(String query) throws SQLException, NoConnectionException {
		if (connection != null) {
			Statement st = connection.createStatement();
			return st.executeQuery(query);
		}
		throw new NoConnectionException();
	}
	/**
	 * Closes the connection
	 * @throws SQLException 
	 */
	public void close() throws SQLException {
		connection.close();
	}
	
}
