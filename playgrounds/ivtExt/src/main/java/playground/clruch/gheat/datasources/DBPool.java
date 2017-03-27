package playground.clruch.gheat.datasources;

public class DBPool {
    // final static String PROPSFILE = "db.properties";
    // private static DataSource ds;
    // private static SharedPoolDataSource tds;
    // static {
    // Properties dataSourceProperties = getProperties();
    // DriverAdapterCPDS cpds = new DriverAdapterCPDS();
    // try {
    // cpds.setDriver(dataSourceProperties.getProperty("driverClass"));
    // } catch (ClassNotFoundException e) {
    // e.printStackTrace();
    // }
    // cpds.setUrl(dataSourceProperties.getProperty("url"));
    // cpds.setUser(dataSourceProperties.getProperty("username"));
    // cpds.setPassword(dataSourceProperties.getProperty("password"));
    // tds = new SharedPoolDataSource();
    // tds.setConnectionPoolDataSource(cpds);
    // tds.setMaxActive(Integer.valueOf(dataSourceProperties.getProperty("maxActive")));
    // tds.setMaxWait(Integer.valueOf(dataSourceProperties.getProperty("maxWait")));
    // ds = tds;
    // }
    //
    // private static Properties getProperties() {
    // Properties dataSourceProperties = new Properties();
    // try {
    // dataSourceProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPSFILE));
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // return dataSourceProperties;
    // }
    //
    // public static Connection getConnection() throws SQLException {
    // return ds.getConnection();
    // }
    //
    // public static void CloseConnections() throws Exception {
    // if (ds != null) {
    // tds.close();
    // ds = null;
    // tds = null;
    // }
    // }
}