package playground.sergioo.weeklySetup2016;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import others.sergioo.util.probability.ContinuousRealDistribution;

public class ClusterDistributions {

	private static class Person {
		private double[] actTimes;
		private double[] homeTimes;
		private double[] demogs;
		
		private Person(double[] actTimes) {
			super();
			this.actTimes = actTimes;
		}
		private void setArrays(double[] homeTimes, double[] demogs) {
			this.homeTimes = homeTimes;
			this.demogs = demogs;
		}
		
	}
	
	private static Map<Integer, Person> persons = new HashMap<>();
	private static Map<Integer, Set<Person>> clusters = new HashMap<>();
	private static Map<Integer, List<ContinuousRealDistribution>> clusterDistributions = new HashMap<>();
	private static Map<Integer, List<Integer>> zeros = new HashMap<>();
	private static Map<Integer, Integer> clusterSizes = new HashMap<>();
	
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			clusterSizes.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			clusters.put(Integer.parseInt(parts[0]), new HashSet<Person>());
			line = reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(args[1]));
		line = reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			Set<Person> persons = clusters.get(Integer.parseInt(parts[15]));
			if(persons!=null) {
				double[] actTimes = new double[14];
				for(int i=0; i<14; i++)
					actTimes[i] = Double.parseDouble(parts[i]);
				Person person = new Person(actTimes);
				persons.add(person);
				ClusterDistributions.persons.put((int)Double.parseDouble(parts[14]), person);
			}
			line = reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(args[2]));
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			Person person = persons.get((int)Double.parseDouble(parts[parts.length-1]));
			if(person!=null) {
				double[] homeTimes = new double[14];
				for(int i=0; i<14; i++)
					homeTimes[i] = Double.parseDouble(parts[i]);
				double[] demogs = new double[parts.length-1-14];
				for(int i=0; i<parts.length-1-14; i++)
					demogs[i] = Double.parseDouble(parts[14+i]);
				person.setArrays(homeTimes, demogs);
			}
			line = reader.readLine();
		}
		reader.close();
		for(Entry<Integer, Set<Person>> persons:clusters.entrySet()) {
			List<Integer> clusterZeros = new ArrayList<>();
			for(int i=0; i<14; i++)
				clusterZeros.add(0);
			for(int i=0; i<14; i++)
				clusterZeros.add(0);
			for(Person person:persons.getValue()) {
				int n=0;
				for(double actTime:person.actTimes) {
					if((n%2==0 && actTime<0)||(n%2==1 && actTime==0))
						clusterZeros.set(n, clusterZeros.get(n)+1);
					n++;
				}
				for(double homeTime:person.homeTimes) {
					if((n%2==0 && homeTime<0)||(n%2==1 && homeTime==0))
						clusterZeros.set(n, clusterZeros.get(n)+1);
					n++;
				}
			}
			ClusterDistributions.zeros.put(persons.getKey(), clusterZeros);
		}
		PrintWriter writer = new PrintWriter(args[3]);
		for(int i=0; i<clusters.values().iterator().next().iterator().next().demogs.length; i++)
			writer.print("DEM"+i+"	");
		writer.println("CHOICE");
		for(Entry<Integer, Set<Person>> persons:clusters.entrySet()) {
			List<Integer> clusterZeros = zeros.get(persons.getKey());
			List<ContinuousRealDistribution> clusterDistributions = new ArrayList<>();
			for(int i=0; i<14; i++)
				clusterDistributions.add(new ContinuousRealDistribution());
			for(int i=0; i<14; i++)
				clusterDistributions.add(new ContinuousRealDistribution());
			for(int i=0; i<persons.getValue().iterator().next().demogs.length; i++)
				clusterDistributions.add(new ContinuousRealDistribution());
			for(Person person:persons.getValue()) {
				int desZeros = 0;
				for(double dem:person.demogs)
					if(dem==0)
						desZeros++;
				if(desZeros<person.demogs.length/2) {
					int n=0;
					for(double actTime:person.actTimes) {
						if(!(n%2==1 && actTime==0) && (clusterZeros.get(n)/(double)persons.getValue().size()<0.9 && ((n%2==0 && actTime>0)||(n%2==1 && actTime>0))))
							clusterDistributions.get(n).addValue(actTime);
						n++;
					}
					for(double homeTime:person.homeTimes) {
						if(!(n%2==1 && homeTime==0) && (clusterZeros.get(n)/(double)persons.getValue().size()<0.9 && ((n%2==0 && homeTime>0)||(n%2==1 && homeTime>0))))
							clusterDistributions.get(n).addValue(homeTime);
						n++;
					}
					for(double dem:person.demogs) {
						clusterDistributions.get(n++).addValue(dem);
						writer.print(dem+"	");
					}
					writer.println(persons.getKey());
				}
			}
			ClusterDistributions.clusterDistributions.put(persons.getKey(), clusterDistributions);
		}
		writer.close();
		writer = new PrintWriter(args[4]);
		for(Entry<Integer, List<ContinuousRealDistribution>> cluster:clusterDistributions.entrySet()) {
			int n=0;
			for(ContinuousRealDistribution distribution:cluster.getValue()) {
				for(Entry<Double, Integer> val:distribution.getValues().entrySet())
					for(int i=0; i<val.getValue(); i++)
						writer.println(cluster.getKey()+","+n+","+val.getKey());
				n++;
			}
		}
		writer.close();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[5]));
		oos.writeObject(clusterDistributions);
		oos.close();
	}

}
