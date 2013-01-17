package playground.wdoering.grips.evacuationanalysis.control;

import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.measure.unit.NonSI;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.NumberRange;
import org.opengis.annotation.Obligation;
import org.opengis.annotation.Specification;
import org.opengis.annotation.UML;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.SampleDimension;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.Record;
import org.opengis.util.RecordType;

public class TiffExporter {
	
	private final static GeoTiffWriteParams DEFAULT_WRITE_PARAMS;
	 
    static {
        // setting the write parameters (we my want to make these configurable in the future
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setCompressionType("LZW");
        DEFAULT_WRITE_PARAMS.setCompressionQuality(0.75F);
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setTiling(512, 512);
    }
	
	public static boolean writeGEOTiff(Envelope env, String fileName, BufferedImage image) throws IOException
	{
		
		Color[] colorramp = { Color.BLUE, Color.GREEN };
		Category[] catset = {
				new Category("NoData", new Color(0, 0, 0, 0), -9999),
				new Category("Clouds", Color.WHITE, -8888),
				new Category("Elevation off benchmark", colorramp,
						NumberRange.create(0, 5000), NumberRange.create(-100.0,
								400.0)) };

		GridSampleDimension[] sampdims = { new GridSampleDimension(
				"Elevation data", catset, NonSI.FOOT) };

		RenderedImage img = image;


		GridCoverageFactory fac = CoverageFactoryFinder.getGridCoverageFactory(null);
		
		GridCoverage2D coverage = fac.create("Test Image", img, env, null, null, null);

		File file = new File(fileName);
		// TODO check file prior to writing
		GeoTiffWriter writer = new GeoTiffWriter(file);

		// setting the write parameters for this geotiff
		final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
		
		params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(DEFAULT_WRITE_PARAMS);
		
		final GeneralParameterValue[] wps = (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]);
		try
		{
			writer.write(coverage, wps);
			writer.dispose(); 
		}
		finally
		{
			try { writer.dispose(); } catch (Exception e) { e.printStackTrace(); return false; }
		}
		return true;
		
		
	}
	
}
