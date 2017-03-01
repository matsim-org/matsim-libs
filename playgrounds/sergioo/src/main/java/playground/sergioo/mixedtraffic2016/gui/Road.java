package playground.sergioo.mixedtraffic2016.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.vehicles.VehicleType;
import others.sergioo.util.geometry.Line2D;
import others.sergioo.util.geometry.Point2D;
import others.sergioo.visUtils.PointLines;

public class Road extends Observable implements PointLines{

	private final static double SCALE = 10;
	private final static double GAP = SCALE*0.2;
	private final static double REACTION_TIME = 1.5;
	public static final double RATE = 0.1;
	
	private static class LineFunction {
		private enum Type {
			FINAL, SLOPE;
		}
		private double to;
		private double tf;
		private double m = 0;
		private double b;
		private double bf;
		
		public LineFunction(double to, double tf, double b, double v, Type type) {
			super();
			this.to = to;
			this.tf = tf;
			this.b = b;
			if(type==Type.FINAL) {
				this.m = (v-b)/(tf-to);
				this.bf = v;
			}
			else if(type==Type.SLOPE) {
				this.m = v;
				this.bf = m*(tf-to)+b;
			}
		}
		public LineFunction(double to, double tf, double b) {
			super();
			this.to = to;
			this.tf = tf;
			this.b = b;
			this.bf = b;
		}
		public double getArea() {
			return (tf-to)*(b+bf)/2;
		}
		public double getValue(double t) {
			return m*(t-to)+b;
		}
		public double getArea(double time) {
			return (time-to)*(b+getValue(time))/2;
		}
		public double getValueArea(double area) {
			double r = (bf-b)/(tf-to);
			if(r==0)
				return (tf-to)*area/getArea();
			else
				return (-b+Math.sqrt(b*b+2*r*area))/r;
		}
	}
	private static class Mode {
		double length;
		double width;
		double speed;
		double acceleration = 1;
		double position;
		public Mode(double length, double width, double speed) {
			super();
			this.length = length;
			this.width = width;
			this.speed = speed;
		}
	}
	public static class Vehicle {
		private String mode;
		private double inTime;
		private double outTime;
		private List<LineFunction> speedFunction = new LinkedList<>();
		public Vehicle(String mode, double inTime) {
			super();
			this.mode = mode;
			this.inTime = inTime;
		}
		public void setOutTime(double outTime) {
			this.outTime = outTime;
		}
		private double getPosition(double time) {
			double l = 0;
			for(LineFunction function:speedFunction) {
				if(time>=function.tf)
					l+=function.getArea();
				else {
					l+=function.getArea(time);
					break;
				}
			}
			return l;
		}
		private double getTotalDistance() {
			double lengthT = 0;
			for(LineFunction function:speedFunction)
				lengthT+=function.getArea();
			return lengthT;
		}
	}
	public enum TypeRoad {
		MODE_INDEPENDENT,
		MODE_SHARED;
	}
	private Collection<Point2D> points = new LinkedList<>();
	private Map<Double, Collection<Line2D>> lines = new HashMap<>();
	private SortedMap<Double, Vehicle> vehicles = new TreeMap<>();
	private Map<String, Mode> modes;
	public double startTime = Double.MAX_VALUE;
	public double endTime = -Double.MAX_VALUE;
	private double length;
	private double currentTime;
	
	public Road(TypeRoad type, Collection<VehicleType> vehicleTypes, double length, double linkSpeed, Collection<Vehicle> vehicles) throws Exception {
		for(VehicleType vehicleType: vehicleTypes)
			modes.put(vehicleType.getId().toString(), new Road.Mode(vehicleType.getLength(), vehicleType.getWidth(), vehicleType.getMaximumVelocity()));
		length/=5/2;
		this.length = length;
		double maxWidth = 0, sumWidth = 0;
		for(Mode mode:modes.values()) {
			if(mode.speed > linkSpeed)
				mode.speed = linkSpeed;
			sumWidth += mode.width;
			if(maxWidth<mode.width)
				maxWidth = mode.width;
		}
		for(Vehicle vehicle:vehicles) {
			Mode mode = modes.get(vehicle.mode);
			if(mode==null)
				throw new Exception(vehicle.mode + " mode is not specified.");
			if(startTime>vehicle.inTime)
				startTime = vehicle.inTime;
			if(endTime<vehicle.outTime)
				endTime = vehicle.outTime;
		}
		this.currentTime = startTime;
		if(type == TypeRoad.MODE_INDEPENDENT) {
			double pos=0;
			for(Mode mode:modes.values()) {
				pos += SCALE*mode.width/2;
				mode.position = pos;
				pos += SCALE*mode.width/2;
			}
			
		}
		else if(type == TypeRoad.MODE_SHARED) {
			for(Mode mode:modes.values())
				mode.position = SCALE*maxWidth/2;
		}
		for(Vehicle vehicle:vehicles)
			this.vehicles.put(vehicle.outTime, vehicle);
		Map<String, Vehicle> prevVehicles = new HashMap<>();
		Vehicle prevVehicle = null;
		for(Vehicle vehicle:this.vehicles.values()) {
			String mode = type == TypeRoad.MODE_INDEPENDENT?vehicle.mode:null;
			calculateSpeedFunction(vehicle, prevVehicles.get(mode), prevVehicle);
			prevVehicles.put(mode, vehicle);
			prevVehicle = vehicle;
		}
		for(Vehicle vehicle:this.vehicles.values()) {
			if(Math.abs(vehicle.getTotalDistance()-this.length)>0.1)
			System.out.println(vehicle.getTotalDistance()+","+vehicle.outTime);
		}
		double widthRoad = type==TypeRoad.MODE_INDEPENDENT?sumWidth:maxWidth;
		Point2D pbl = new Point2D(0, 0);
		Point2D pbr = new Point2D(length, 0);
		Point2D ptr = new Point2D(length, SCALE*widthRoad);
		Point2D ptl = new Point2D(0, SCALE*widthRoad);
		List<Line2D> linesA = new LinkedList<>();
		linesA.add(new Line2D(pbl, ptl));
		linesA.add(new Line2D(ptl, ptr));
		linesA.add(new Line2D(ptr, pbr));
		linesA.add(new Line2D(pbr, pbl));
		points.add(new Point2D(-10, -length/3));
		points.add(new Point2D(length+10, length/3));
		for(double time=startTime; time<endTime; time+=RATE) {
			time = RATE *Math.round(time/RATE);
			Collection<Line2D> lines = new LinkedList<>(linesA);
			for(Vehicle vehicle:this.vehicles.values())
				if(vehicle.inTime<time && vehicle.outTime>time) {
					Mode mode = modes.get(vehicle.mode);
					double thick = 2, y;
					if(time-vehicle.inTime>this.length/mode.speed) {
						thick = 5;
						//y=SCALE*widthRoad/2;
					}
					//else
						y=mode.position;
					double x = vehicle.getPosition(time);
					double w = SCALE*mode.width, l = mode.length;
					pbl = new Point2D(x+GAP/4, y-w/2+GAP);
					ptl = new Point2D(x+GAP/4, y+w/2-GAP);
					pbr = new Point2D(x+l-GAP/4, y-w/2+1.5*GAP);
					ptr = new Point2D(x+l-GAP/4, y+w/2-1.5*GAP);
					lines.add(new Line2D(pbl, ptl, thick));
					lines.add(new Line2D(ptl, ptr, thick));
					lines.add(new Line2D(ptr, pbr, thick));
					lines.add(new Line2D(pbr, pbl, thick));
				}
			this.lines.put(time, lines);
		}
	}
	private void calculateSpeedFunction(Vehicle vehicle, Vehicle prevVehicleM, Vehicle prevVehicle) {
		Mode mode = modes.get(vehicle.mode);
		if(prevVehicleM==null)
			vehicle.speedFunction.addAll(getSpeedFuntionA(mode.speed, mode.acceleration, vehicle.inTime, vehicle.outTime, length/(vehicle.outTime-vehicle.inTime), mode.length));
		else {
			if(prevVehicleM.inTime>=vehicle.inTime) {
				double length = 0;
				for(LineFunction function:prevVehicleM.speedFunction) {
					if(length + function.getArea()>mode.length) {
						vehicle.inTime = function.to + function.getValueArea(mode.length -length);
						break;
					}
					length += function.getArea();
				}
			}
			if(vehicle.outTime-prevVehicleM.outTime<=1) {
				/*for(Vehicle vehicleT:this.vehicles.values())
					if(vehicleT.outTime>vehicle.outTime)
						vehicleT.outTime += prevVehicle.outTime+1.5 - vehicle.outTime;*/
				vehicle.outTime=prevVehicleM.outTime+1.5;
			}
			double length = 0;
			LineFunction last = ((LinkedList<LineFunction>)prevVehicleM.speedFunction).getLast();
			double lastDis = Math.min(mode.length, last.bf*(vehicle.outTime-prevVehicleM.outTime)), rTime = Math.min(REACTION_TIME, vehicle.outTime-prevVehicleM.outTime-(lastDis==0?0:lastDis/last.bf));
			lastDis = mode.length;
			rTime = 0.5;
			for(int i=0; i<prevVehicleM.speedFunction.size(); i++) {
				LineFunction function = prevVehicleM.speedFunction.get(i);
				length += function.getArea();
				if(length-lastDis>0) {
					double dif = mode.speed*(function.tf-vehicle.inTime)-(length-lastDis);
					if(dif>0) {
						double max = mode.speed, t;
						if(max>function.bf)
							t = Math.abs((length-lastDis+vehicle.inTime*max - Math.pow(max-function.bf,2)/(2*mode.acceleration)-function.bf*(function.tf+rTime))/(max-function.bf));
						else
							t = Math.abs((length-lastDis+vehicle.inTime*max + Math.pow(max-function.bf,2)/(2*mode.acceleration)-function.bf*(function.tf+rTime))/(max-function.bf));
						if(Double.isNaN(t) || t<vehicle.inTime || t>function.tf || t+Math.abs(max-function.bf)/mode.acceleration>function.tf) {
							double insqrt = Math.pow(function.tf+function.bf/mode.acceleration-vehicle.inTime, 2)+2*(function.bf*(rTime-function.bf/(2*mode.acceleration))+lastDis-length)/mode.acceleration;
							if(insqrt>=0) {
								max = -mode.acceleration*(vehicle.inTime-function.bf/mode.acceleration-function.tf+Math.sqrt(insqrt));
								t = function.tf - Math.abs(max-function.bf)/mode.acceleration;
								if(max<function.bf) {
									insqrt = Math.pow(function.tf-function.bf/mode.acceleration-vehicle.inTime, 2)-2*(function.bf*(rTime+function.bf/(2*mode.acceleration))+lastDis-length)/mode.acceleration;
									if(insqrt>=0) {
										max = mode.acceleration*(vehicle.inTime+function.bf/mode.acceleration-function.tf+Math.sqrt(insqrt));
										t = function.tf - Math.abs(max-function.bf)/mode.acceleration;
										if(max>function.bf)
											t=vehicle.inTime;
									}
								}
							}
							if(insqrt<0 || t<=vehicle.inTime || max<0) {
								max = 2*(length-lastDis)/(function.tf+rTime-vehicle.inTime) - function.bf;
								if(max<0) {
									max = 0;
									t = function.tf+rTime-2*(length-lastDis)/function.bf;
								}
								else	
									t=vehicle.inTime;
							}
						}
						if(t>vehicle.inTime)
							vehicle.speedFunction.add(new LineFunction(vehicle.inTime, t, max));
						double time = function.tf+rTime;
						double nextTime = t==vehicle.inTime || t==function.tf+rTime-2*(length-lastDis)/function.bf ?time:t+Math.abs(max-function.bf)/mode.acceleration;
						if(max!=function.bf && nextTime>t || (t==vehicle.inTime && max==function.bf))
							vehicle.speedFunction.add(new LineFunction(t, nextTime, max, function.bf, LineFunction.Type.FINAL));
						if((time-nextTime)>0)
							vehicle.speedFunction.add(new LineFunction(nextTime, time, function.bf));
						for(int j=i+1; j<prevVehicleM.speedFunction.size();j++) {
							LineFunction f = prevVehicleM.speedFunction.get(j);
							vehicle.speedFunction.add(new LineFunction(time, time+=(f.tf-f.to), f.b, f.bf, LineFunction.Type.FINAL));
						}
						solveLast(vehicle.speedFunction, time, vehicle.outTime, last.bf, mode.acceleration, lastDis);
						return;
					}
				}
			}
			vehicle.speedFunction.addAll(getSpeedFuntionA(mode.speed, mode.acceleration, vehicle.inTime, vehicle.outTime, this.length/(vehicle.outTime-vehicle.inTime), mode.length));
		}
	}
	private List<LineFunction> getSpeedFuntionA(double maxSpeed, double acceleration, double inTime, double outTime, double speed, double vLength) {
		List<LineFunction> speedFunction = new LinkedList<>();
		if(maxSpeed==speed)
			speedFunction.add(new LineFunction(inTime, outTime, maxSpeed));
		else {
			double t = outTime - Math.sqrt(2*(outTime-inTime)*Math.abs(maxSpeed-speed)/acceleration);
			if(t>inTime && acceleration*(outTime-t)<maxSpeed) {
				speedFunction.add(new LineFunction(inTime, t, maxSpeed));
				speedFunction.add(new LineFunction(t, outTime, maxSpeed, (speed>maxSpeed?1:-1)*acceleration, LineFunction.Type.SLOPE));
			}
			else if(t<inTime)
				speedFunction.add(new LineFunction(inTime, outTime, speed+acceleration*(outTime-inTime)/2, -acceleration, LineFunction.Type.SLOPE));
			else {
				if(speed>maxSpeed)
					maxSpeed = speed;
				t = inTime - maxSpeed/(2*acceleration) + (speed*(outTime-inTime)-vLength)/maxSpeed;
				speedFunction.add(new LineFunction(inTime, t, maxSpeed));
				solveLast(speedFunction, t, outTime, maxSpeed, acceleration, vLength + maxSpeed*maxSpeed/(2*acceleration));
			}
		}
		return speedFunction;
	}
	private void solveLast(List<LineFunction> speedFunction, double t, double outTime, double speed, double acceleration, double totalLength) {
		if(totalLength>0) {
			double tf = t+speed/acceleration;
			double dif = outTime-tf;
			double vLength = totalLength - speed*speed/(2*acceleration);
			double base = Math.sqrt(4*vLength/acceleration);
			if(dif>=base) {
				if(tf>t || speed>0)
					speedFunction.add(new LineFunction(t, tf, speed, 0, LineFunction.Type.FINAL));
				speedFunction.add(new LineFunction(tf, outTime-base, 0));
				speedFunction.add(new LineFunction(outTime-base, outTime-base/2, 0, acceleration, LineFunction.Type.SLOPE));
				speedFunction.add(new LineFunction(outTime-base/2, outTime, acceleration*(base/2), 0, LineFunction.Type.FINAL));
			}
			else if(dif>=base/Math.sqrt(2)) {
				if(tf>t || speed>0)
					speedFunction.add(new LineFunction(t, tf, speed, 0, LineFunction.Type.FINAL));
				speedFunction.add(new LineFunction(tf, outTime-base/Math.sqrt(2), 0));
				speedFunction.add(new LineFunction(outTime-base/Math.sqrt(2), outTime, 0, acceleration, LineFunction.Type.SLOPE));
			}
			else {
				double fSpeed = -speed+2*totalLength/(outTime-t);
				if(fSpeed<0) {
					double t2 = t+2*totalLength/speed;
					speedFunction.add(new LineFunction(t, t2, speed, 0, LineFunction.Type.FINAL));
					speedFunction.add(new LineFunction(t2, outTime, 0));
				}
				else {
					/*double acc = (fSpeed-speed)/(outTime - t);
					if(acc>acceleration || acc<-acceleration)
						outTime = t+(-speed+Math.sqrt(speed*speed+2*acceleration*totalLength))/acceleration;
					speedFunction.add(new LineFunction(t, outTime, speed, speed + (acc>0?1:-1)*acceleration*(outTime-t), LineFunction.Type.FINAL));*/
					speedFunction.add(new LineFunction(t, outTime, speed, fSpeed, LineFunction.Type.FINAL));
				}
			}
		}
		else if(speed>0 && t<outTime)
			System.out.println("Maaaaaaal");
	}
	@Override
	public Collection<Point2D> getPoints() {
		return points;
	}

	@Override
	public Collection<Line2D> getLines() {
		Collection<Line2D> lines  = this.lines.get(currentTime);
		if(lines==null)
			return new LinkedList<>();
		else
			return lines;
	}
	public void setCurrentTime(double time) {
		time = RATE *Math.round(time/RATE);
		this.currentTime = time;
		this.setChanged();
	}

}
