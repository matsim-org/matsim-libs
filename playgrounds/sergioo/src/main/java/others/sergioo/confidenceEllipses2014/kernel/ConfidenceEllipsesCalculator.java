package others.sergioo.confidenceEllipses2014.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import javax.swing.JFileChooser;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import others.sergioo.confidenceEllipses2014.gui.WindowEllipse;
import others.sergioo.util.geometry.Line2D;
import others.sergioo.util.geometry.Point2D;
import others.sergioo.visUtils.PointLines;

public class ConfidenceEllipsesCalculator {

	private enum TypeEllipse {
		ALTERS,
		EGO_CENTER,
		HMEAN_CENTER,
		ALTERS_WEIGHT,
		EGO_CENTER_WEIGHT,
		HMEAN_CENTER_WEIGHT;
	}
	public static class PersonEllipse extends Observable implements PointLines {
		
		private String data;
		private EgoData ego;
		private Set<AlterData> alters = new HashSet<>();
		private EllipseData[] ellipses = new EllipseData[TypeEllipse.values().length];
		
		public PersonEllipse(String data) {
			super();
			this.data = data;
		}
		public EllipseData[] getEllipses() {
			return ellipses;
		}
		public void process(double confidence) {
			double sumWeight = 0, sumWeight2 = 0, meanX = 0, meanY = 0, meanWX = 0, meanWY = 0;
			for(AlterData alter:alters) {
				sumWeight += alter.weight;
				sumWeight2 += Math.pow(alter.weight, 2);
				meanX += alter.x;
				meanY += alter.y;
				meanWX += alter.weight*alter.x;
				meanWY += alter.weight*alter.y;
			}
			meanX /= alters.size();
			meanY /= alters.size();
			meanWX /= sumWeight;
			meanWY /= sumWeight;
			double sumDevsMeanX = 0, sumDevsMeanY = 0, sumDevsEgoX = 0, sumDevsEgoY = 0, sumDevsEgoHX = 0, sumDevsEgoHY = 0;
			double sumDevsMeanWX = 0, sumDevsMeanWY = 0, sumDevsEgoWX = 0, sumDevsEgoWY = 0, sumDevsEgoWHX = 0, sumDevsEgoWHY = 0;
			double sumDevsMeanXY = 0, sumDevsEgoXY = 0, sumDevsEgoHXY = 0, sumDevsMeanWXY = 0, sumDevsEgoWXY = 0, sumDevsEgoWHXY = 0;
			for(AlterData alter:alters) {
				sumDevsMeanX += Math.pow(alter.x-meanX, 2);
				sumDevsMeanY += Math.pow(alter.y-meanY, 2);
				sumDevsMeanXY += (alter.x-meanX)*(alter.y-meanY);
				sumDevsEgoX += Math.pow(alter.x-ego.x, 2);
				sumDevsEgoY += Math.pow(alter.y-ego.y, 2);
				sumDevsEgoXY += (alter.x-ego.x)*(alter.y-ego.y);
				sumDevsEgoHX += Math.pow(alter.x-ego.hx, 2);
				sumDevsEgoHY += Math.pow(alter.y-ego.hy, 2);
				sumDevsEgoHXY += (alter.x-ego.hx)*(alter.y-ego.hy);
				sumDevsMeanWX += alter.weight*Math.pow(alter.x-meanWX, 2);
				sumDevsMeanWY += alter.weight*Math.pow(alter.y-meanWY, 2);
				sumDevsMeanWXY += alter.weight*(alter.x-meanWX)*(alter.y-meanWY);
				sumDevsEgoWX += alter.weight*Math.pow(alter.x-ego.x, 2);
				sumDevsEgoWY += alter.weight*Math.pow(alter.y-ego.y, 2);
				sumDevsEgoWXY += alter.weight*(alter.x-ego.x)*(alter.y-ego.y);
				sumDevsEgoWHX += alter.weight*Math.pow(alter.x-ego.hx, 2);
				sumDevsEgoWHY += alter.weight*Math.pow(alter.y-ego.hy, 2);
				sumDevsEgoWHXY += alter.weight*(alter.x-ego.hx)*(alter.y-ego.hy);
			}
			double smxx = sumDevsMeanX/(alters.size()-1);
			double smyy = sumDevsMeanY/(alters.size()-1);
			double smxy = sumDevsMeanXY/(alters.size()-1);
			double sexx = sumDevsEgoX/(alters.size()-1);
			double seyy = sumDevsEgoY/(alters.size()-1);
			double sexy = sumDevsEgoXY/(alters.size()-1);
			double shexx = sumDevsEgoHX/(alters.size()-1);
			double sheyy = sumDevsEgoHY/(alters.size()-1);
			double shexy = sumDevsEgoHXY/(alters.size()-1);
			double smwxx = sumDevsMeanWX*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double smwyy = sumDevsMeanWY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double smwxy = sumDevsMeanWXY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double sewxx = sumDevsEgoWX*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double sewyy = sumDevsEgoWY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double sewxy = sumDevsEgoWXY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double shewxx = sumDevsEgoWHX*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double shewyy = sumDevsEgoWHY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			double shewxy = sumDevsEgoWHXY*sumWeight/(Math.pow(sumWeight, 2)-sumWeight2);
			ellipses[TypeEllipse.ALTERS.ordinal()] = new EllipseData(meanX, meanY, smxx, smyy, smxy, confidence);
			ellipses[TypeEllipse.EGO_CENTER.ordinal()] = new EllipseData(ego.x, ego.y, sexx, seyy, sexy, confidence);
			ellipses[TypeEllipse.HMEAN_CENTER.ordinal()] = new EllipseData(ego.hx, ego.hy, shexx, sheyy, shexy, confidence);
			ellipses[TypeEllipse.ALTERS_WEIGHT.ordinal()] = new EllipseData(meanWX, meanWY, smwxx, smwyy, smwxy, confidence);
			ellipses[TypeEllipse.EGO_CENTER_WEIGHT.ordinal()] = new EllipseData(ego.x, ego.y, sewxx, sewyy, sewxy, confidence);
			ellipses[TypeEllipse.HMEAN_CENTER_WEIGHT.ordinal()] = new EllipseData(ego.hx, ego.hy, shewxx, shewyy, shewxy, confidence);
		}
		@Override
		public Collection<Point2D> getPoints() {
			Collection<Point2D> points = new ArrayList<>();
			for(AlterData alter:alters)
				points.add(new Point2D(alter.x, alter.y));
			return points;
		}
		@Override
		public Collection<Line2D> getLines() {
			return new ArrayList<Line2D>();
		}
		
	}
	private static class EgoData {
		
		private String id;
		private double x;
		private double y;
		private double hx;
		private double hy;
		
		public EgoData(String id, double x, double y) {
			super();
			this.id = id;
			this.x = x;
			this.y = y;
		}
		public EgoData(String id, double x, double y, double hx, double hy) {
			super();
			this.id = id;
			this.x = x;
			this.y = y;
			this.hx = hx;
			this.hy = hy;
		}
		
	}
	private static class AlterData {
		
		private double x;
		private double y;
		private double weight;
		
		public AlterData(double x, double y, double frequency) {
			super();
			this.x = x;
			this.y = y;
			this.weight = frequency;
		}
		
	}
	public static class EllipseData {
		
		private double centerX;
		private double centerY;
		private double mayorSemiAxis;
		private double minorSemiAxis;
		private double angle;
		private double area;
		
		
		public EllipseData(double x, double y, double sxx, double syy, double sxy, double confidence) {
			super();
			double v = new ChiSquaredDistribution(2).inverseCumulativeProbability(confidence);
			double l1 = (sxx+syy+Math.sqrt(Math.pow(sxx-syy, 2)+(4*sxy*sxy)))/2;
			double l2 = (sxx+syy-Math.sqrt(Math.pow(sxx-syy, 2)+(4*sxy*sxy)))/2;
			this.centerX = x;
			this.centerY = y;
			this.mayorSemiAxis = Math.sqrt(v*l1);
			this.minorSemiAxis = Math.sqrt(v*l2);
			this.angle = Math.atan2((l1-sxx),sxy);
			this.area = v*Math.PI*Math.sqrt(sxx*syy-sxy*sxy);
		}
		public double getCenterX() {
			return centerX;
		}
		public double getCenterY() {
			return centerY;
		}
		public double getMayorSemiAxis() {
			return mayorSemiAxis;
		}
		public double getMinorSemiAxis() {
			return minorSemiAxis;
		}
		public double getAngle() {
			return angle;
		}
		public double getArea() {
			return area;
		}
		private String getData() {
			return centerX+SEP+centerY+SEP+mayorSemiAxis+SEP+minorSemiAxis+SEP+angle+SEP+area;
		}
		
	}
	
	private static final String SEP = "\t";
	
	public static void main(String[] args) throws IOException {
		Map<String, PersonEllipse> personEllipses = new HashMap<>();
		JFileChooser jfc = new JFileChooser("./data");
		jfc.showOpenDialog(null);
		String id = "36";
		File file = jfc.getSelectedFile();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String head = reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(SEP);
			PersonEllipse personEllipse = new PersonEllipse(line);
			if(parts[0].equals(id))
				personEllipse.ego = new EgoData(parts[0], new Double(parts[2]), new Double(parts[1]), new Double(parts[4]), new Double(parts[3]));
			else
				personEllipse.ego = new EgoData(parts[0], new Double(parts[2]), new Double(parts[1]));
			personEllipses.put(parts[0], personEllipse);
			line = reader.readLine();
			while(line!=null && line.startsWith(SEP)) {
				parts = line.split(SEP);
				personEllipse.alters.add(new AlterData(new Double(parts[2]), new Double(parts[1]), new Double(parts[3])/* *Math.hypot(personEllipse.ego.x-new Double(parts[2]), personEllipse.ego.y-new Double(parts[1]))*/));
				line = reader.readLine();
			}
		}
		reader.close();
		for(PersonEllipse personEllipse:personEllipses.values())
			personEllipse.process(0.95);
		PersonEllipse person = personEllipses.get(id);
		WindowEllipse windowEllipse = new WindowEllipse(person, "Ellipses of person "+id);
		windowEllipse.setVisible(true);
		File fileR = new File(file.getPath().substring(0,file.getPath().lastIndexOf("."))+"_RES.txt");
		PrintWriter printer = new PrintWriter(fileR);
		printer.print(head);
		for(int i=0; i<TypeEllipse.values().length; i++)
			printer.print(SEP+"centerX"+SEP+"centerY"+SEP+"mayorSemiAxis"+SEP+"minorSemiAxis"+SEP+"angle"+SEP+"area");
		printer.println();
		for(PersonEllipse personEllipse:personEllipses.values()) {
			printer.print(personEllipse.data+SEP);
			for(EllipseData ellipseData:personEllipse.ellipses)
				printer.print(ellipseData.getData()+SEP);
			printer.println();
		}
		printer.close();
	}

}
