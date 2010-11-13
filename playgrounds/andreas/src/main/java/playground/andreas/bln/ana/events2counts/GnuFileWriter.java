package playground.andreas.bln.ana.events2counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


public class GnuFileWriter {
	
	private BufferedWriter writer; 
	private String outputDir;
	
	public GnuFileWriter(String outputDir) {
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(outputDir + "plot.gnu")));
			this.outputDir = outputDir;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void write(TransitSchedule transitSchedule) {
		try {
			for (Id line : transitSchedule.getTransitLines().keySet()) {
				
				for (TransitRoute transitRoute : transitSchedule.getTransitLines().get(line).getRoutes().values()) {

					this.writer.newLine();
					this.writer.write("reset"); this.writer.newLine();
					this.writer.write("cd \"" + this.outputDir + "\""); this.writer.newLine();

					this.writer.write("set loadpath \"E:/Tools/gnuplot/bin/share/postscript/\""); this.writer.newLine();
					this.writer.write("set encoding utf8"); this.writer.newLine();
					this.writer.write("set terminal win"); this.writer.newLine();
					this.writer.write("set terminal postscript eps color lw 1 \"Arial\" 10"); this.writer.newLine();
					this.writer.write("set datafile separator \";\""); this.writer.newLine();

					this.writer.write("set lmargin 3"); this.writer.newLine();
					this.writer.write("set bmargin 1"); this.writer.newLine();
					this.writer.write("set rmargin 3"); this.writer.newLine();
					this.writer.write("set tmargin 0"); this.writer.newLine();

					this.writer.write("unset key"); this.writer.newLine();
					this.writer.write("unset title"); this.writer.newLine();

					this.writer.write("set output \"" + line.toString().trim() + "_" + transitRoute.getId().toString().trim() + ".ps"); this.writer.newLine();

					this.writer.write("set multiplot layout 4,1"); this.writer.newLine();
					this.writer.write("set grid ytics"); this.writer.newLine();

					this.writer.write("set style data histogram"); this.writer.newLine();
					this.writer.write("set style histogram cluster gap 1 "); this.writer.newLine();
					this.writer.write("set style fill solid noborder"); this.writer.newLine();
					this.writer.write("set boxwidth"); this.writer.newLine();

					this.writer.write("set style line 1 linetype 1 linecolor rgb \"orange\" linewidth 3 "); this.writer.newLine();
					this.writer.write("set style line 2 linetype 1 linecolor rgb \"blue\" linewidth 3"); this.writer.newLine();
					this.writer.write("set style line 3 linetype 1 linecolor rgb \"black\" linewidth 3"); this.writer.newLine();

					//				this.writer.write("set yrange [0 : 500] "); this.writer.newLine();
					//				this.writer.write("set ytics (0, 100, 200, 300, 400) "); this.writer.newLine();
					this.writer.write("set tics scale 0 "); this.writer.newLine();
					this.writer.write("unset xtics"); this.writer.newLine();

					this.writer.write("file = '" + line.toString().trim() + "_" + transitRoute.getId().toString().trim() + ".txt'"); this.writer.newLine();
					// plot access
					this.writer.write("plot for [i = 5:76:3] file using i ls 1 "); this.writer.newLine();
					// plot egress
					this.writer.write("plot for [i = 6:76:3] file using i ls 2 "); this.writer.newLine();

					this.writer.write("set xtics nomirror "); this.writer.newLine();
					this.writer.write("set tics scale 0 "); this.writer.newLine();
					this.writer.write("set xtics rotate "); this.writer.newLine();

//					this.writer.write("file = '" + line.toString().trim() + "_H.txt'"); this.writer.newLine();
					// plot occupancy rate
					this.writer.write("plot for [i = 7:76:3] file using i:xticlabels(1) ls 3 "); this.writer.newLine();

					this.writer.write("unset multiplot"); this.writer.newLine();
					this.writer.flush();
					
					
					// plot 24h graph
					
					this.writer.newLine();
					this.writer.write("reset"); this.writer.newLine();
					this.writer.write("cd \"" + this.outputDir + "\""); this.writer.newLine();

					this.writer.write("set loadpath \"E:/Tools/gnuplot/bin/share/postscript/\""); this.writer.newLine();
					this.writer.write("set encoding utf8"); this.writer.newLine();
					this.writer.write("set terminal win"); this.writer.newLine();
					this.writer.write("set terminal postscript eps color lw 1 \"Arial\" 10"); this.writer.newLine();
					this.writer.write("set datafile separator \";\""); this.writer.newLine();

					this.writer.write("set lmargin 3"); this.writer.newLine();
					this.writer.write("set bmargin 1"); this.writer.newLine();
					this.writer.write("set rmargin 3"); this.writer.newLine();
					this.writer.write("set tmargin 0"); this.writer.newLine();

					this.writer.write("unset key"); this.writer.newLine();
					this.writer.write("unset title"); this.writer.newLine();

					this.writer.write("set output \"" + line.toString().trim() + "_" + transitRoute.getId().toString().trim() + "_24h.ps"); this.writer.newLine();

					this.writer.write("set multiplot layout 4,1"); this.writer.newLine();
					this.writer.write("set grid ytics"); this.writer.newLine();

					this.writer.write("set style data histogram"); this.writer.newLine();
					this.writer.write("set style histogram cluster gap 1 "); this.writer.newLine();
					this.writer.write("set style fill solid noborder"); this.writer.newLine();
					this.writer.write("set boxwidth"); this.writer.newLine();

					this.writer.write("set style line 1 linetype 1 linecolor rgb \"orange\" linewidth 3 "); this.writer.newLine();
					this.writer.write("set style line 2 linetype 1 linecolor rgb \"blue\" linewidth 3"); this.writer.newLine();
					this.writer.write("set style line 3 linetype 1 linecolor rgb \"black\" linewidth 3"); this.writer.newLine();

					//				this.writer.write("set yrange [0 : 500] "); this.writer.newLine();
					//				this.writer.write("set ytics (0, 100, 200, 300, 400) "); this.writer.newLine();
					this.writer.write("set tics scale 0 "); this.writer.newLine();
					this.writer.write("unset xtics"); this.writer.newLine();

					this.writer.write("file = '" + line.toString().trim() + "_" + transitRoute.getId().toString().trim() + ".txt'"); this.writer.newLine();
					// plot access
					this.writer.write("plot file using 2 ls 1 "); this.writer.newLine();
					// plot egress
					this.writer.write("plot file using 3 ls 2 "); this.writer.newLine();

					this.writer.write("set xtics nomirror "); this.writer.newLine();
					this.writer.write("set tics scale 0 "); this.writer.newLine();
					this.writer.write("set xtics rotate "); this.writer.newLine();

//					this.writer.write("file = '" + line.toString().trim() + "_H.txt'"); this.writer.newLine();
					// plot occupancy rate
					this.writer.write("plot file using 4:xticlabels(1) ls 3 "); this.writer.newLine();

					this.writer.write("unset multiplot"); this.writer.newLine();
					this.writer.flush();
				
				}

//				if(transitSchedule.get(line).size() > 1){
//					this.writer.newLine();
//					this.writer.write("reset"); this.writer.newLine();
//					this.writer.write("cd \"" + this.outputDir + "\""); this.writer.newLine();
//
//					this.writer.write("set loadpath \"E:/Tools/gnuplot/bin/share/postscript/\""); this.writer.newLine();
//					this.writer.write("set encoding utf8"); this.writer.newLine();
//					this.writer.write("set terminal win"); this.writer.newLine();
//					this.writer.write("set terminal postscript eps color lw 1 \"Arial\" 10"); this.writer.newLine();
//					this.writer.write("set datafile separator \";\""); this.writer.newLine();
//
//					this.writer.write("set lmargin 3"); this.writer.newLine();
//					this.writer.write("set bmargin 1"); this.writer.newLine();
//					this.writer.write("set rmargin 3"); this.writer.newLine();
//					this.writer.write("set tmargin 0"); this.writer.newLine();
//
//					this.writer.write("unset key"); this.writer.newLine();
//					this.writer.write("unset title"); this.writer.newLine();
//
//					this.writer.write("set output \"" + line.toString().trim() + "_R.ps\""); this.writer.newLine();
//
//					this.writer.write("set multiplot layout 4,1"); this.writer.newLine();
//					this.writer.write("set grid ytics"); this.writer.newLine();
//
//					this.writer.write("set style data histogram"); this.writer.newLine();
//					this.writer.write("set style histogram cluster gap 1 "); this.writer.newLine();
//					this.writer.write("set style fill solid noborder"); this.writer.newLine();
//					this.writer.write("set boxwidth"); this.writer.newLine();
//
//					this.writer.write("set style line 1 linetype 1 linecolor rgb \"orange\" linewidth 3 "); this.writer.newLine();
//					this.writer.write("set style line 2 linetype 1 linecolor rgb \"blue\" linewidth 3"); this.writer.newLine();
//					this.writer.write("set style line 3 linetype 1 linecolor rgb \"black\" linewidth 3"); this.writer.newLine();
//
//					//				this.writer.write("set yrange [0 : 500] "); this.writer.newLine();
//					//				this.writer.write("set ytics (0, 100, 200, 300, 400) "); this.writer.newLine();
//					this.writer.write("set tics scale 0 "); this.writer.newLine();
//					this.writer.write("unset xtics"); this.writer.newLine();
//
//					this.writer.write("file = '" + line.toString().trim() + "_R.txt'"); this.writer.newLine();
//					// plot access
//					this.writer.write("plot for [i = 2:73:3] file using i ls 1 "); this.writer.newLine();
//					// plot egress
//					this.writer.write("plot for [i = 3:73:3] file using i ls 2 "); this.writer.newLine();
//
//					this.writer.write("set xtics nomirror "); this.writer.newLine();
//					this.writer.write("set tics scale 0 "); this.writer.newLine();
//					this.writer.write("set xtics rotate "); this.writer.newLine();
//
//					this.writer.write("file = '" + line.toString().trim() + "_R.txt'"); this.writer.newLine();
//					// plot occupancy rate
//					this.writer.write("plot for [i = 4:73:3] file using i:xticlabels(1) ls 3 "); this.writer.newLine();
//
//					this.writer.write("unset multiplot"); this.writer.newLine();
//					this.writer.flush();
//				}
			
			}
			this.writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
