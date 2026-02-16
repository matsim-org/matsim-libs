package org.matsim.utils.tablesaw;

import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Page;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TablesawUtils{
	// if we want to move this to a more public module, then that module needs to have tablesaw as input.  kai, nov'25

	private TablesawUtils(){} // do not instantiate

	/**
	 * This is taken from {@link tech.tablesaw.plotly.Plot} but I have commented out the command that starts the browser.   Intended
	 * use: Be able to write the html to file so that it can be used later.
	 *
	 * @param pathname
	 * @param figure
	 */
	public static void writeFigureToHtmlFile( String pathname, Figure figure ){
		File outputFile = new File( pathname );
		String output = Page.pageBuilder( figure, "divName" ).build().asJavascript();
		try {
			try ( Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
				writer.write(output);
			}
//			new Browser().browse(outputFile );
		} catch ( IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
