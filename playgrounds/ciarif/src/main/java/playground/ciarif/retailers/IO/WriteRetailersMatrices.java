package playground.ciarif.retailers.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import cern.colt.matrix.impl.AbstractMatrix;

public class WriteRetailersMatrices {
	
	private FileWriter fw = null;
	private BufferedWriter out = null;
	
	public WriteRetailersMatrices () {
		//super();
	}
	public void writeRetailersMatrices (AbstractMatrix matrix, String filename) {
		
		String outfile = "/scr_stardust/baug/ciarif/output/zurich_10pc/matrices/" + filename;
		//String outfile = "../../output/triangle/matrices/" + filename;
		
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
		try {
			out.write(matrix.toString() +"\n");
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void write(AbstractMatrix matrix) {
		
	}
}
