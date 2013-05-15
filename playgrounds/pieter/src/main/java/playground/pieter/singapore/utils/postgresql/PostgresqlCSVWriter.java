package playground.pieter.singapore.utils.postgresql;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;



 public class PostgresqlCSVWriter {
	String writerName = "";
	String tableName;
	DataBaseAdmin dba;
	int modfactor = 1;
	int lineCounter = 0;
	int batchSize = 2000;
	int pushBackSize = 200000;
	StringBuilder sb = new StringBuilder();
	CopyManager cpManager;
	List<PostgresqlColumnDefinition> columns;
	private PushbackReader reader;

	public PostgresqlCSVWriter(String tableName, DataBaseAdmin dba,
			int batchSize, List<PostgresqlColumnDefinition> columns) {
		super();
		this.tableName = tableName;
		this.dba = dba;
		this.batchSize = batchSize;
		this.columns = columns;
		this.pushBackSize = 0;
		for (PostgresqlColumnDefinition col : columns) {
			pushBackSize += col.type.size();
		}
		pushBackSize *= batchSize;
		init();
	}

	/**
	 * @param writerName the name of this particular writer instance, used in syso logging
	 * @param tableName
	 * @param dba
	 * @param batchSize the larger the batchsize, the more lines are sent in one request.
	 * @param columns column definitions, assuming only the 4 basic data types for now.
	 */
	public PostgresqlCSVWriter(String writerName, String tableName,
			DataBaseAdmin dba, int batchSize,
			List<PostgresqlColumnDefinition> columns) {
		this(tableName, dba, batchSize, columns);
		this.writerName = writerName;
	}

	public void init() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd_hhmm");
		String formattedDate = df.format(new Date());
		try {
			dba.executeStatement(String.format("ALTER TABLE %s RENAME TO %s;",
					tableName,tableName+"_replaced_on_"+formattedDate));
		} catch (SQLException e) {
			System.err.println("Table "+tableName+" doesn't exist.");;
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			StringBuilder createString = new StringBuilder(String.format(
					"CREATE TABLE %s(", tableName));
			for (int i = 0; i < columns.size(); i++) {
				PostgresqlColumnDefinition col = columns.get(i);

				createString.append(col.name + " " + col.type + " "
						+ col.extraParams + " ,");

			}
			// drop the last comma
			createString.deleteCharAt(createString.length() - 1);
			createString.append(");");
			dba.executeStatement(createString.toString());
			cpManager = ((PGConnection) dba.getConnection()).getCopyAPI();
			reader = new PushbackReader(new StringReader(""), pushBackSize);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void addComment(String comment){
		try {
			dba.executeStatement(String.format("COMMENT ON TABLE %s IS \'%s\';", tableName,comment));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addLine(Object[] args) {
		lineCounter++;
		try {
			if (args.length != columns.size()) {
				throw new RuntimeException(
						"not the same number of parameters sent as there are columns in the table");
			}
			String sqlInserter = "";
			for (int i = 0; i < args.length; i++) {
				// if(columns.get(i).type != PostgresType.TEXT){
				// sqlInserter += args[i].toString()+",";
				// }else{
				// sqlInserter += "\'"+args[i].toString()+"\',";
				// }
				sqlInserter += args[i].toString() + ",";
			}
			// trim the last comma, add a newline
			sb.append(sqlInserter);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
			if (lineCounter % batchSize == 0) {
				reader.unread(sb.toString().toCharArray());
				cpManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV",
						reader);
				sb.delete(0, sb.length());
			}
			if (lineCounter >= modfactor && lineCounter % modfactor == 0) {
				System.out.println(writerName + ": Processed line no "
						+ lineCounter);
				modfactor = lineCounter;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finish() {
		// write out the rest
		try {
			reader.unread(sb.toString().toCharArray());
			cpManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV",
					reader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sb.delete(0, sb.length());
		System.out.println(writerName + ": Processed line no " + lineCounter);
	}
}
