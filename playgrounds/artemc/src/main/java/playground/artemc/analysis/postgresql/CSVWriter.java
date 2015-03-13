package playground.artemc.analysis.postgresql;

import org.matsim.core.utils.io.IOUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CSVWriter extends TableWriter {


	private final String path;
     private CsvListWriter csvListWriter;

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


        BufferedWriter writer = IOUtils.getBufferedWriter(path + "/" + tableName);
			csvListWriter = new CsvListWriter(writer, CsvPreference.STANDARD_PREFERENCE);

		try {
			ArrayList<String> colnames=  new ArrayList<>();
            for (PostgresqlColumnDefinition col : columns) {
                colnames.add(col.name);
            }
			// drop the last comma
			String[] header = null;
			csvListWriter.write(colnames);

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
			csvListWriter.write(args);
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
//			writer.write(sb.toString());
			csvListWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(writerName + ": Processed line no " + lineCounter);
	}


}
