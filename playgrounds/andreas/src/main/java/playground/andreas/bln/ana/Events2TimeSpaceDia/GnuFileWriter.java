package playground.andreas.bln.ana.Events2TimeSpaceDia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileWriter;

public class GnuFileWriter {
	
	private BufferedWriter writer; 
	
	public GnuFileWriter(String outputDir) {
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(outputDir + "plot.gnu")));
			this.writer.write("reset"); this.writer.newLine();
			this.writer.write("cd \"" + outputDir + "\""); this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void write(HashMap<Id, Double> stopIdDistanceMap, String line) {
		try {
			this.writer.write("#set loadpath \"E:\\Tools\\gnuplot\\bin\\share\\postscript\\\""); this.writer.newLine();
			this.writer.write("set terminal win"); this.writer.newLine();
			this.writer.write("#set terminal postscript"); this.writer.newLine();
			this.writer.write("set datafile separator \",\""); this.writer.newLine();
			this.writer.write("set autoscale"); this.writer.newLine();
			this.writer.write("set grid"); this.writer.newLine();
			this.writer.write("set xdata time"); this.writer.newLine();
			this.writer.write("set timefmt \"%H:%M:%S\""); this.writer.newLine();

			this.writer.write("set xtics rotate \"00:00:00\", 1800, \"30:00:00\""); this.writer.newLine();
			this.writer.write("set xrange [\"03:00:00\" :  \"23:00:00\"]"); this.writer.newLine();

			this.writer.write(writeYTics(stopIdDistanceMap)); this.writer.newLine();
			
			this.writer.write("set key off"); this.writer.newLine();
			this.writer.write("set xlabel \"Zeit [HH:MM:SS]\" 40, 0"); this.writer.newLine();
			this.writer.write("set ylabel \"ID der Haltestelle [ ]\" 0, 15"); this.writer.newLine();
			
			this.writer.write("set style line 1  linetype 1 linecolor rgb \"blue\"  linewidth 2.000 pointtype 1 pointsize default"); this.writer.newLine();
			this.writer.write("set style line 2  linetype 1 linecolor rgb \"red\"  linewidth 2.000 pointtype 2 pointsize default"); this.writer.newLine();

			this.writer.write("#set output \"" + line.trim() + ".ps\""); this.writer.newLine();

			this.writer.write("filename(n) = sprintf(\"veh_%d\", n)"); this.writer.newLine();
			this.writer.write("plot for [i=1:20] filename(i) using 2:3 axes x1y1 title filename(i) with lines ls 1 ,\\"); this.writer.newLine();
			this.writer.write("\"veh_14\" using 2:3 axes x1y1 title \"veh_14\" with lines ls 2"); this.writer.newLine();

			this.writer.flush();
			this.writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String writeYTics(HashMap<Id, Double> stopIdDistanceMap) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("set ytics (");
		boolean first = true;
		for (Entry<Id, Double> entry : stopIdDistanceMap.entrySet()) {
			
			if(first){
				first = false;
			} else {
				buffer.append(", ");					
			}
			
			buffer.append("\"");
			buffer.append(entry.getKey().toString());
			buffer.append("\" ");
			buffer.append(entry.getValue());
			
		}
		buffer.append(")");
		return buffer.toString();	
	}

}
