package playground.artemc.analysis.postgresql;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class PostgresqlCSVWriter  extends TableWriter {

	private final DataBaseAdmin dba;
	private CopyManager cpManager;

	private PostgresqlCSVWriter(String tableName, DataBaseAdmin dba,
                               int batchSize, List<PostgresqlColumnDefinition> columns) {
		super(tableName, batchSize, columns);
		this.tableName = tableName;
		this.dba = dba;
		init();

	}


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
					tableName,tableName.split("\\.")[1]+"_replaced_on_"+formattedDate));
		} catch (SQLException e) {
			System.err.println("Table "+tableName+" doesn't exist.");
       } catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			StringBuilder createString = new StringBuilder(String.format(
					"CREATE TABLE %s(", tableName));
           for (PostgresqlColumnDefinition col : columns) {
               createString.append(col.name).append(" ").append(col.type).append(" ").append(col.extraParams).append(" ,");
           }
			// drop the last comma
			createString.deleteCharAt(createString.length() - 1);
			createString.append(");");
			dba.executeStatement(createString.toString());
			cpManager = ((PGConnection) dba.getConnection()).getCopyAPI();
			reader = new PushbackReader(new StringReader(""), pushBackSize*4);
		} catch (SQLException | NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   }
	public void addComment(String comment){
		try {
			dba.executeStatement(String.format("COMMENT ON TABLE %s IS \'%s\';", tableName,comment));
		} catch (SQLException | NoConnectionException e) {
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
           for (Object arg : args) {
               // if(columns.get(i).type != PostgresType.TEXT){
               // sqlInserter += args[i].toString()+",";
               // }else{
               // sqlInserter += "\'"+args[i].toString()+"\',";
               // }
               sqlInserter += (arg == null ? "NULL" : arg.toString()) + ",";
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

		} catch (SQLException | IOException e) {
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
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       sb.delete(0, sb.length());
		System.out.println(writerName + ": Processed line no " + lineCounter);
	}

}
