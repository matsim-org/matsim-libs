/* *********************************************************************** *
 * project: org.matsim.*
 * OutputDelegate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.counts.algorithms.graphs.helper;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.gbl.MatsimResource;
import org.matsim.utils.io.IOUtils;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;


public class OutputDelegate {

	private final List<Section> sections_;
	private final List<CountsGraph> cg_list_;
	private String iterPath_;

	public OutputDelegate(final String iterPath) {
		this.iterPath_ = iterPath;
		this.sections_ = new Vector<Section>();
		this.cg_list_ = new Vector<CountsGraph>();
	}

	public void addSection(final Section section) {
		this.sections_.add(section);
	}

	public void addCountsGraph(final CountsGraph cg) {
		this.cg_list_.add(cg);
	}

	public void setIterPath(final String iterPath) {
		this.iterPath_ = iterPath;
	}

	public List<CountsGraph> getGraphs() {
		return this.cg_list_;
	}

	public void outPutAll(final boolean htmlset, final boolean pdfset){
		if (htmlset) {
			new File(this.iterPath_+"/png").mkdir();
		}
		if (pdfset) {
			new File(this.iterPath_+"/pdf").mkdir();
		}

		Iterator<CountsGraph> cg_it = this.cg_list_.iterator();
		while (cg_it.hasNext()) {
			CountsGraph cg=cg_it.next();

			if (htmlset) {
				writeHtml(cg, this.iterPath_, false);
			}
			if (pdfset){
				try {
					saveChartAsPDF(cg, this.iterPath_+"/pdf/", (int)PageSize.A4.rotate().getWidth(), (int)PageSize.A4.rotate().getHeight(), new DefaultFontMapper());
				}
				catch (IOException e) {
			        System.out.println(e.toString());
			    }//catch
			}
		}
		if (htmlset) {
			writeHtml(null, this.iterPath_, true);
			try {
				new File(this.iterPath_+"/div").mkdir();
				copyResourceToFile("style1.css", this.iterPath_ + "/div/style1.css");
				copyResourceToFile("logo.png", this.iterPath_ + "/div/logo.png");
				copyResourceToFile("overlib.js", this.iterPath_ + "/div/overlib.js");
				copyResourceToFile("title.png", this.iterPath_ + "/div/title.png");
			}
			catch (IOException e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}//catch
		}//if
	}
	
	private void copyResourceToFile(final String resourceFilename, final String destinationFilename) throws IOException {
		final InputStream inStream = MatsimResource.getAsInputStream(resourceFilename);
		final OutputStream outStream = new FileOutputStream(destinationFilename);
		IOUtils.copyStream(inStream, outStream);
		try {
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			outStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeHtml(final CountsGraph cg, final String iter_path, boolean indexFile){
		/* we want landscape, thus exchange width / height */
		int width=(int)PageSize.A4.getHeight();
		int height=(int)PageSize.A4.getWidth();

		JFreeChart chart=null;
		String fileName="";
		File file2;
		if (!indexFile){
			chart=cg.getChart();
			fileName=cg.getFilename();
			file2 = new File(iter_path+"/"+fileName+".html");
		}
		else {
			file2 = new File(iter_path+"/start.html");
		}

		PrintWriter writer = null;
		try {
			ChartRenderingInfo info=null;
			File file1;
			if (!indexFile){
				 info = new ChartRenderingInfo(new StandardEntityCollection());
				 file1 = new File(iter_path+"/png/"+fileName+".png");
				ChartUtilities.saveChartAsPNG(file1, chart, width, height, info);
			}

			writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file2)));

			writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			//writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
			writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
			writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
			writer.println("<head>");
			//writer.println("<meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=UTF-8\" />");
			writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
			writer.println("<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\"/>");
			writer.println("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\"/>");

			writer.println("<title> MATSim validation </title>");
			writer.println("<script type=\"text/javascript\" src=\"div/overlib.js\"></script>");
			writer.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"div/style1.css\"/>");
			writer.println("</head>");
			writer.println("<body>");

			writer.println("<div id=\"overDiv\" style=\"Z-INDEX: 1; POSITION: absolute\"></div>");

			writer.println("<div id=\"header\">");
			writer.println("<div id=\"logo\">");
			writer.println("<img src=\"div/logo.png\" width=\"224\" height=\"52\" style=\"border:none;\" alt=\"logo\"/><br/>Multi-Agent Transport Simulation Toolkit");
			writer.println("</div>");
			writer.println("<h3>Counting Volumes</h3>");
			writer.println("</div>");

			writer.println("<div id=\"footer\">");

			GregorianCalendar cal = new GregorianCalendar();
			writer.println(cal.get(Calendar.DATE) + ".");
			writer.println(cal.get(Calendar.MONTH) + 1 + ".");
			writer.println(cal.get(Calendar.YEAR) + "\t ");
			writer.println(System.getProperty("user.name"));

			writer.println("</div>");
			writer.println("<div id=\"links\">");

			Iterator<Section> sec_it = this.sections_.iterator();
			while (sec_it.hasNext()) {
				Section sec= sec_it.next();

				writer.print("<h3>");
				writer.print(sec.getTitle()+":<br />");
				writer.print("</h3>");

				Iterator<MyURL> url_it = sec.getURLs().iterator();
				while (url_it.hasNext()) {
					MyURL url=url_it.next();
					writer.println("<a href=\""+url.address+"\">"+url.displayText+"</a><br />");
				}//while
			}
			writer.println("</div>");

			writer.println("<div id=\"contents\">");
			writer.println("<p>");
			if (!indexFile) {
				ChartUtilities.writeImageMap(writer, "chart", info, true);

				/*
	        	chart=cg.getChart();
	        	CategoryPlot plot=chart.getCategoryPlot();
				Renderer renderer=(BarRenderer) plot.getRenderer();
				renderer.getSeriesToolTipGenerator(0);
				 */

				/*	how to get tooltips in there, without using xxxToolTipTagFragmentGenerator?
	        		Wait for next version of jFreeChart? Meanwhile doing slight changes to the libarary
	        		String imagemap=ChartUtilities.getImageMap("chart", info);
	        		System.out.println(imagemap);
				 */

				// reference '#chart' not working without '#'
				writer.println("<img src=\"png/"+fileName+".png\" "
						+ "width=\""+width+"\" height=\""+height+"\" style=\"border:none;\" alt=\"graph\" usemap=\"#chart\"/>");
			}
			else {writer.println("<img src=\"div/title.png\" "
					+ "width=\"972\" height=\"602\" style=\"border:none;\" alt=\"title\"/>");
			}
			writer.println("</p>");
			writer.println("</div>");
			writer.println("</body>");
			writer.println("</html>");

		}//try
		catch (IOException e) {
			System.out.println(e.toString());
		}//catch
		finally {
			if (writer != null) {
				writer.close();
			}
		}//finally
	}//writeHtml


	private void saveChartAsPDF(final CountsGraph cg,final String iter_path, final int width,	final int height,	final FontMapper mapper) throws IOException {
		JFreeChart chart=cg.getChart();
		String fileName=cg.getFilename();
		final File file = new File(iter_path+"/"+fileName+".pdf");
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
			writeChartAsPDF(chart, out, width, height, mapper);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}//saveChartAsPDF


	//Writes a chart to an output stream in PDF format
	private void writeChartAsPDF(final JFreeChart chart, final OutputStream out,final int width,	final int height,	final FontMapper mapper) {
		Rectangle pagesize = new Rectangle(width, height);
		Document document = new Document(pagesize);
		try {
			PdfWriter writer = PdfWriter.getInstance(document, out);
			document.addAuthor("MATSim");
			document.addSubject("Counts");
			document.setPageSize(PageSize.A4.rotate());
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			PdfTemplate tp = cb.createTemplate(width, height);
			Graphics2D g2 = tp.createGraphics(width, height, mapper);
			Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
			chart.draw(g2, r2D);
			g2.dispose();
			cb.addTemplate(tp, 0, 0);
			document.close();
		}//try
		catch (Exception e) {
			System.err.println(e.getMessage());
		}//catch
	}//writeChartAsPDF

}
