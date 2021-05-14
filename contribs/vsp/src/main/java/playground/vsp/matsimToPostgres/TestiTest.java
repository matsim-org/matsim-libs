package playground.vsp.matsimToPostgres;


public class TestiTest {

    public static void main(String[] args) {

    }

    public void testExport(){

        
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
