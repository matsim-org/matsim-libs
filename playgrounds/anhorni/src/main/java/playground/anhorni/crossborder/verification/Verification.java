package playground.anhorni.crossborder.verification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.entity.StandardEntityCollection;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;


import com.lowagie.text.PageSize;

public class Verification {
	
	//0:E; 1:P; 2:N; 3:S
	private int[][] xTripsPerHour;
	private double[][] aggregatedVolumePerHourTGZM;
	private int[] transitTripsPerHour;
	private double[] xDifference;
	
	public Verification() {
		this.xTripsPerHour=new int[4][24];
		this.aggregatedVolumePerHourTGZM= new double[4][24];
		this.transitTripsPerHour=new int[24];
		this.xDifference=new double[4];
		for (int i=0; i<4; i++) {
			this.xDifference[i]=0.0;
			for (int h=0; h<24; h++) {
				this.aggregatedVolumePerHourTGZM[i][h]=0.0;
			}
		}
		
	}
	
	public void addXDifference(String actType, double xDifference) {
		this.xDifference[this.getActTypeInt(actType)]+=xDifference;
	}
	
	public void addTransitTripsPerHour(int hour, int trips) {
		this.transitTripsPerHour[hour]+=trips;
	}
	
	public void addToAggregatedVolume(String actType, int hour, double val) {
		this.aggregatedVolumePerHourTGZM[this.getActTypeInt(actType)][hour]+=val;
	}
	
	public double getAggregatedVolume(String actType, int hour) {		
		return this.aggregatedVolumePerHourTGZM[this.getActTypeInt(actType)][hour];
	}
	
	
	public void setXTripsPerHour(String actType, int hour, int tripsPerHour) {
		this.xTripsPerHour[this.getActTypeInt(actType)][hour] = tripsPerHour;
	}
	
	private void writeTGZMGraph() {
				
		int width=(int)PageSize.A4.getHeight();
		int height=(int)PageSize.A4.getWidth();
		
		for (int i=0; i<4; i++) {
			TGZMCompare tgzm=new TGZMCompare(this.xTripsPerHour[i], this.aggregatedVolumePerHourTGZM[i]);

			JFreeChart chart=tgzm.createChart(this.getActTypeString(i));
			String fileName="TGZMCompare"+this.getActTypeString(i);

			try {
				ChartRenderingInfo info=null;
				 info = new ChartRenderingInfo(new StandardEntityCollection());
				 File file1 = new File("output/"+fileName+".png");
				 ChartUtilities.saveChartAsPNG(file1, chart, width, height, info);
				}
			catch (IOException e) {
				System.out.println(e.toString());
			}//catch	
		}
		
		int[] sumTripsPerHour=new int[24];
		double[] sumAggregatedVolumePerHour={0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
		
		for (int i=0; i<24; i++) {
			for (int j=0; j<4;j++) {
				sumTripsPerHour[i]+=this.xTripsPerHour[j][i];	
				sumAggregatedVolumePerHour[i]+=this.aggregatedVolumePerHourTGZM[j][i];
			}
		}
			
		TGZMCompare tgzm=new TGZMCompare(sumTripsPerHour, sumAggregatedVolumePerHour);
		JFreeChart chart=tgzm.createChart("All");
		String fileName="TGZMCompare All";

		try {
			ChartRenderingInfo info=null;
			 info = new ChartRenderingInfo(new StandardEntityCollection());
			 File file1 = new File("output/"+fileName+".png");
			 ChartUtilities.saveChartAsPNG(file1, chart, width, height, info);
			}
		catch (IOException e) {
			System.out.println(e.toString());
		}//catch
	}
	
	private void writeTripsPerActivityType() throws IOException {
		
		int[] sumTripsPerActivity={0,0,0,0};
		double[] sumAggregatedVolumePerActivity={0.0, 0.0, 0.0, 0.0};
		double sumAggregatedTripsTransit=0.0;
		
		for (int i=0; i<24; i++) {
			sumAggregatedTripsTransit+=this.transitTripsPerHour[i];
			for (int j=0; j<4;j++) {
				sumTripsPerActivity[j]+=this.xTripsPerHour[j][i];	
				sumAggregatedVolumePerActivity[j]+=this.aggregatedVolumePerHourTGZM[j][i];
			}
		}
		
		int sumAllActivities=0;
		double sumAllActivitiesTGMZ=0.0;
		double difference[]=new double[4];
		
		for (int i=0; i<4; i++) {
			sumAllActivities+=sumTripsPerActivity[i];
			sumAllActivitiesTGMZ+=sumAggregatedVolumePerActivity[i];
			difference[i]=sumAggregatedVolumePerActivity[i]-sumTripsPerActivity[i];
		}
		
		
		BufferedWriter out;
		try {
			out = IOUtils.getBufferedWriter("output/tripsPerActivity");
			String table1Header="Activity_Type \tCalc_Trips \tCalc_Share[%] \tTGZM_Trips \tTGZM_Share[%] \tDifference\n";
			out.write(table1Header);
			out.write("E \t" + sumTripsPerActivity[0] +"\t"+((double)sumTripsPerActivity[0])/sumAllActivities*100.0 +"\t"+
					sumAggregatedVolumePerActivity[0]+"\t" +sumAggregatedVolumePerActivity[0]/sumAllActivitiesTGMZ*100.0
					+"\t"+difference[0]+"\n");
			out.write("P \t" + sumTripsPerActivity[1] +"\t"+((double)sumTripsPerActivity[1])/sumAllActivities*100.0 +"\t"+
					sumAggregatedVolumePerActivity[1]+"\t" +sumAggregatedVolumePerActivity[1]/sumAllActivitiesTGMZ*100.0
					+"\t"+difference[1]+"\n");
			out.write("N \t" + sumTripsPerActivity[2] +"\t"+((double)sumTripsPerActivity[2])/sumAllActivities*100.0 +"\t"+
					sumAggregatedVolumePerActivity[2]+"\t" +sumAggregatedVolumePerActivity[2]/sumAllActivitiesTGMZ*100.0
					+"\t"+difference[2]+"\n");
			out.write("S \t" + sumTripsPerActivity[3] +"\t"+((double)sumTripsPerActivity[3])/sumAllActivities*100.0 +"\t"+
					sumAggregatedVolumePerActivity[3]+"\t" +sumAggregatedVolumePerActivity[3]/sumAllActivitiesTGMZ*100.0
					+"\t"+difference[3]+"\n");
			out.write("All \t" + sumAllActivities+ "\t-\t" +sumAllActivitiesTGMZ+ "\t-\t"+ (sumAllActivitiesTGMZ-sumAllActivities)+"\n");
			
			out.write("\n");
			out.write("Transit traffic: Trips "+ sumAggregatedTripsTransit +
					"\t Share: "+ sumAggregatedTripsTransit/sumAllActivities*100.0 +"\n");
			
			out.write("\n");
			String table2Header="Type \trounding error \tshare[%]\n";
			out.write(table2Header);
			out.write("E \t"+ this.xDifference[0]+"\t"+this.xDifference[0]/sumAggregatedVolumePerActivity[0]*100.0 +"\n");
			out.write("P \t"+ this.xDifference[1]+"\t"+this.xDifference[1]/sumAggregatedVolumePerActivity[1]*100.0 +"\n");
			out.write("N \t"+ this.xDifference[2]+"\t"+this.xDifference[2]/sumAggregatedVolumePerActivity[2]*100.0 +"\n");
			out.write("S \t"+ this.xDifference[3]+"\t"+this.xDifference[3]/sumAggregatedVolumePerActivity[3]*100.0 +"\n");
			
			
			
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 		
	}
	
	public void writeVerification() {
		this.writeTGZMGraph();
		
		try{
			this.writeTripsPerActivityType();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	public int getActTypeInt(String actType) {
		if (actType.equals("E")) return 0;
		else if (actType.equals("P")) return 1;
		else if (actType.equals("N")) return 2;
		else if (actType.equals("S")) return 3;
		else if (actType.equals("All")) return 4;
		else return -1;
	}
	
	public String getActTypeString(int actType) {
		if (actType==0) return "E";
		else if (actType==1) return "P";
		else if (actType==2) return "N";
		else if (actType==3) return "S";
		else if (actType==4) return "All";
		else return "ERROR";
	}
}