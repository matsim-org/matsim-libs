package playground.mmoyo.io;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import playground.mmoyo.analysis.counts.reader.TabularCountReader;

/**parses Counts Mean General Error file*/
public class MRE_reader implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(TabularCountReader.class);
	private static final String[] HEADER = {"hour","mean relative error","mean absolute bias"};
	private final TabularFileParserConfig tabFileParserConfig;
	private int rowNum;
	private double [] mre;
	private double [] mab;
	
	public MRE_reader(){
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
	}
	
	public void readFile(final String mreFile) throws IOException {
		this.mre = new double[24];	
		this.mab = new double[24];
		rowNum= 0;
		this.tabFileParserConfig.setFileName(mreFile);
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}
	
	@Override
	public void startRow(String[] row) {
		if (rowNum>0) {
			this.mre[rowNum-1]= Double.parseDouble(row[1]);
			this.mab[rowNum-1]= Double.parseDouble(row[2]);
		}else{
			boolean equalsHeader = true;
			int i = 0;
			for (String s : row) {
				if (!s.equalsIgnoreCase(HEADER[i])){
					equalsHeader = false;
					break;
				}
				i++;
			}
			if (!equalsHeader) {
				log.warn("the structure does not match. The header should be:  ");
				for (String g : HEADER) {
					System.out.print(g + " ");
				}
				System.out.println();
			}
		}
		rowNum++;
	}
	
	public double[] getMRE(){
		return this.mre;
	}
	
	public double[] getMAB(){
		return this.mab;
	}
	
	public static void main(String[] args) throws IOException {
		String filePath= "../../runs_manuel/CalibLineM44/outMatsimRoutes/ITERS/it.10/biasErrorGraphDataOccupancy.txt";
		MRE_reader mre_reader= new MRE_reader();
		mre_reader.readFile(filePath);
		
		for (int i=0;i<24;i++){
			System.out.println(mre_reader.getMRE()[i]);	
		}
		
	}

}
