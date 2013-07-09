package playground.pieter.singapore.utils.postgresql;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.IOUtils;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;



 public class CSVWriter extends TableWriter {


	private String path;
	private BufferedWriter writer;

	public CSVWriter(String tableName, String path,
			int batchSize, List<PostgresqlColumnDefinition> columns) {
		super(tableName, batchSize, columns);
		this.tableName = tableName.toLowerCase().endsWith(".csv")?tableName:tableName+".csv";
		
		this.path = path;
		init();
	}


	public CSVWriter(String writerName, String tableName,
			String path, int batchSize,
			List<PostgresqlColumnDefinition> columns) {
		this(tableName, path, batchSize, columns);
		this.writerName = writerName;
	}



	public void init() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd_hhmm");
		String formattedDate = df.format(new Date());
		
			writer = IOUtils.getBufferedWriter(path+"/"+tableName);

		try {
			StringBuilder createString = new StringBuilder();
			for (int i = 0; i < columns.size(); i++) {
				PostgresqlColumnDefinition col = columns.get(i);
				createString.append(col.name + ",");
			}
			// drop the last comma
			createString.deleteCharAt(createString.length() - 1);
			createString.append("\n");
			writer.write(createString.toString());
			reader = new PushbackReader(new StringReader(""), pushBackSize);

		} catch (IOException e) {
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
				sqlInserter += (args[i]==null?"NULL":args[i].toString()) + ",";
			}
			// trim the last comma, add a newline
			sb.append(sqlInserter);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
			if (lineCounter % batchSize == 0) {
				writer.write(sb.toString());
			}
			if (lineCounter >= modfactor && lineCounter % modfactor == 0) {
				System.out.println(writerName + ": Processed line no "
						+ lineCounter);
				modfactor = lineCounter;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finish() {
		// write out the rest
		try {
			writer.write(sb.toString());
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		sb.delete(0, sb.length());
		System.out.println(writerName + ": Processed line no " + lineCounter);
	}


}
