package playground.artemc.analysis;

import playground.artemc.analysis.postgresql.PostgresType;
import playground.artemc.analysis.postgresql.PostgresqlCSVWriter;
import playground.artemc.analysis.postgresql.PostgresqlColumnDefinition;
import playground.artemc.utils.DataBaseAdmin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by artemc on 10/6/15.
 */
public class IndividualCostToSQLWriter {

		HashMap<String, ArrayList<String>> dataMap;

		public IndividualCostToSQLWriter(HashMap<String, ArrayList<String>> dataMap) {
			this.dataMap = dataMap;
		}

		public void writeToDatabase(String connectionPropertiesFile, String schema, String tableName) {
			File connectionProperties = new File(connectionPropertiesFile);

			DateFormat df = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");
			String formattedDate = df.format(new Date());

			List<PostgresqlColumnDefinition> columns = new ArrayList<>();
			columns.add(new PostgresqlColumnDefinition("person_id", PostgresType.TEXT, "primary key"));
			columns.add(new PostgresqlColumnDefinition("TTC_Total", PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("SDC_total", PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("TTC_morning", PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("TTC_evening", PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("SDC_morning", PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("SDC_evening", PostgresType.FLOAT8));

			try {
				DataBaseAdmin individualScoresDBA = new DataBaseAdmin(connectionProperties);
				PostgresqlCSVWriter individualCostWriter = new PostgresqlCSVWriter("Cost", tableName, individualScoresDBA, 1000, columns);
				individualCostWriter.addComment(String.format("Cost per person created on %s.", formattedDate));
				for (String personId : dataMap.keySet()) {
					Object[] data = new Object[7];
					data[0] = personId;
					for (int i = 1; i < data.length; i++) {
						data[i] = dataMap.get(personId).get(i - 1);
					}
					individualCostWriter.addLine(data);
				}

				individualCostWriter.finish();
				individualScoresDBA.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
}
