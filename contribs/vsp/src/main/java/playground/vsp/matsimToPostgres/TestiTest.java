package playground.vsp.matsimToPostgres;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class TestiTest {
    private static final Logger log = Logger.getLogger( TestiTest.class );

    public static void main(String[] args) {
        new TestiTest().testExport();

    }


    public void testExport(){
        String url = "jdbc:postgresql://wedekind-matsim-analysis.c7lku8hegspn.eu-central-1.rds.amazonaws.com/testitest";
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","dWS8GFklQZltTFwZmdvz");

        try {
            Connection conn = DriverManager.getConnection(url, props);
            createTestTable(conn);

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createTestTable(Connection conn) throws SQLException {

        // Connection test
        if (conn != null) {
            System.out.println("Connected to the database!");
        } else {
            System.out.println("Failed to make connection!");
        }

        String sql = "CREATE TABLE IF NOT EXISTS testitest_schema.accounts (user_id serial PRIMARY KEY, user_name VARCHAR (50) NOT NULL, password VARCHAR (50) NOT NULL);";

        assert conn != null;
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.execute();
        conn.commit();
        conn.close();

    }


//List<Employee> result = new ArrayList<>();
//
//String SQL_SELECT = "Select * from EMPLOYEE";
//
//// auto close connection and preparedStatement
//    try (Connection conn = DriverManager.getConnection(
//        "jdbc:postgresql://localhost:5432/", "postgres", "");
//PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
//
//    ResultSet resultSet = preparedStatement.executeQuery();
//
//    while (resultSet.next()) {
//
//        long id = resultSet.getLong("ID");
//        String name = resultSet.getString("NAME");
//        BigDecimal salary = resultSet.getBigDecimal("SALARY");
//        Timestamp createdDate = resultSet.getTimestamp("CREATED_DATE");
//
//        Employee obj = new Employee();
//        obj.setId(id);
//        obj.setName(name);
//        obj.setSalary(salary);
//        // Timestamp -> LocalDateTime
//        obj.setCreatedDate(createdDate.toLocalDateTime());
//
//        result.add(obj);
//
//    }
//    result.forEach(x -> System.out.println(x));
//
//} catch (SQLException e) {
//    System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
//} catch (Exception e) {
//    e.printStackTrace();
//}

}
