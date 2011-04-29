package playground.sergioo.simpleNetworkPainter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;

import util.geometry.Line2D;
import util.geometry.Point2D;
import visUtils.PointLines;
import visUtils.Window;

public class SimpleNetworkCSV extends Observable implements PointLines{
	
	private static final File FILE_NE= new File("./data/youssef/NEDraw.csv");
	private static final File FILE_SC= new File("./data/youssef/SCDraw.csv");
	private static final File FILE_P= new File("./data/youssef/EvacPoints.csv");
	private Collection<Line2D> lines;
	private Collection<Point2D> points;
	
	/**
	 * @throws IOException 
	 * 
	 */
	public SimpleNetworkCSV(boolean nash) throws IOException {
		super();
		lines = new ArrayList<Line2D>();
		BufferedReader reader;
		if(nash)
			reader = new BufferedReader(new FileReader(FILE_NE));
		else
			reader = new BufferedReader(new FileReader(FILE_SC));
		String line = reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(";");
			Line2D lin = new Line2D(new Point2D(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])), new Point2D(Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
			lin.setThickness(10*(Double.parseDouble(parts[4])+0.01));
			lines.add(lin);
			line = reader.readLine();
		}
		reader.close();
		points = new ArrayList<Point2D>();
		reader = new BufferedReader(new FileReader(FILE_P));
		line = reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(";");
			Point2D point = new Point2D(Double.parseDouble(parts[1]),Double.parseDouble(parts[2]));
			points.add(point);
			line = reader.readLine();
		}
		reader.close();
	}

	@Override
	public Collection<Point2D> getPoints() {
		return points;
	}

	@Override
	public Collection<Line2D> getLines() {
		return lines;
	}
	
	public static void main(String[] args) {
		try {
			SimpleNetworkCSV nash = new SimpleNetworkCSV(true);
			Window windowN = new Window(nash,"Nash");
			windowN.setVisible(true);
			SimpleNetworkCSV social = new SimpleNetworkCSV(false);
			Window windowS = new Window(social,"Social");
			windowS.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
