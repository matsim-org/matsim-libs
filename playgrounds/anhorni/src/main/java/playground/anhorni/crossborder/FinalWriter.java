package playground.anhorni.crossborder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

class FinalWriter {
	
	private BufferedWriter out;
	private boolean fileOpened = false;
	private ArrayList<String> files;
	
	public FinalWriter(ArrayList<String> files) {
		this.files=files;
	}
	
		
	// <plans ... > ... </plans> --------------------------------------------------------------
	private void startPlans() throws IOException {
		this.out.write("<plans");
		this.out.write(" name=\"" + Config.plansName + "\"");
		this.out.write(">\n\n");
	}

	private void endPlans() throws IOException {
		this.out.write("</plans>\n");
	}


	public void writeSeparator() throws IOException {
		this.out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}

	private void writeXmlHead() throws IOException {
		this.out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}

	private void writeDoctype() throws IOException {
		this.out.write("<!DOCTYPE plans SYSTEM \"" + Config.DTD + "\">\n");
	}

	private final void writeStartPlans() {
		try {
			this.out = IOUtils.getBufferedWriter(Config.OUTFILE);
			this.fileOpened = true;
			this.writeXmlHead();
			this.writeDoctype();
			this.out.flush();
			this.startPlans();
			this.writeSeparator();
			this.out.flush();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private final void writeEndPlans() {
		if (this.fileOpened) {
			try {
				this.endPlans();
				this.out.flush();
				this.out.close();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	public void write() {

		this.writeStartPlans();
		try {
			this.mergeFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.writeEndPlans();
	}
		
	public void mergeFiles() throws IOException 	{

		
		Iterator<String> s_it = this.files.iterator();
		while (s_it.hasNext()) {
			String s = s_it.next();
		
			try {
				FileReader file_reader = new FileReader(s);
				BufferedReader buffered_reader = new BufferedReader(file_reader);
	
				String curr_line; 
				
				while ((curr_line = buffered_reader.readLine()) != null) {
					this.out.write(curr_line+"\n");
				}
				buffered_reader.close();
				(new File(s)).delete();
			}
			catch (IOException e) {
		        throw e;
		    }	
		}
		this.out.flush();
	}	
}
