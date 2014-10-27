package playground.artemc.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.utils.io.IOUtils;

public class IterationTableWriter {

	private static final String separator = "\t"; 
	private static final String lineEnd = "\n"; 
	BufferedWriter out;

	public IterationTableWriter(String filename, String[] columns) throws IOException {
		
		out = IOUtils.getBufferedWriter(filename);
		StringBuffer sb = new StringBuffer();
		
		for(Integer i=1;i<=columns.length;i++){
			sb.append(columns[i-1]);
			if(i!=columns.length){
				sb.append(separator);
			}
		}
		sb.append(lineEnd);
		out.write(sb.toString());		
	}
	
	
	public void addData(String[] data) throws IOException{
		StringBuffer sb = new StringBuffer();
		for(Integer i=1;i<=data.length;i++){
			sb.append(data[i-1]);
			if(i!=data.length){
				sb.append(separator);
			}
		}
		sb.append(lineEnd);
		out.write(sb.toString());		
	}
	
	public void finish() throws IOException
	{
		out.flush();
		out.close();
	};
	
	
}
