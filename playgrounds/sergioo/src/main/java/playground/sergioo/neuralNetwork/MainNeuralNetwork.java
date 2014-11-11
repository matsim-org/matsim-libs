package playground.sergioo.neuralNetwork;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainNeuralNetwork {

	private static final double DATA_LEARN_FRAC = 0.75;

	public static void main(String[] args) throws IOException {
		int option = new Integer(args[0]);
		switch(option) {
		case 0:
			run0(args);
		}
	}

	private static void run0(String[] args) throws IOException {
		int numInputs = new Integer(args[1]), numOutputs = new Integer(args[3]);
		NeuralNetwork network = new FullNeuralNetwork(numInputs, new Integer(args[2]), numOutputs);
		//((FullNeuralNetwork)network).mutate(0.1);
		BufferedReader reader = new BufferedReader(new FileReader(args[4]));
		reader.readLine();
		String line = reader.readLine();
		List<double[][]> dataLearn = new ArrayList<double[][]>();
		List<double[][]> dataValidate = new ArrayList<double[][]>();
		double[] minOutput = new double[numOutputs];
		for(int i=0; i<numOutputs; i++)
			minOutput[i] = Double.MAX_VALUE;
		double[] maxOutput = new double[numOutputs];
		for(int i=0; i<numOutputs; i++)
			maxOutput[i] = -Double.MAX_VALUE;
		double[] minInput = new double[numInputs];
		for(int i=0; i<numInputs; i++)
			minInput[i] = Double.MAX_VALUE;
		double[] maxInput = new double[numInputs];
		for(int i=0; i<numInputs; i++)
			maxInput[i] = -Double.MAX_VALUE;
		while(line!=null) {
			String[] parts = line.split("\t");
			double[] output = new double[numOutputs];
			for(int i=0; i<numOutputs; i++) {
				output[i] = new Double(parts[i]);
				if(output[i]<minOutput[i])
					minOutput[i] = output[i];
				if(output[i]>maxOutput[i])
					maxOutput[i] = output[i];
			}
			double[] input = new double[numInputs];
			for(int i=numOutputs+1; i<numInputs+numOutputs+1; i++) {
				input[i-numOutputs-1] = new Double(parts[i]);
				if(input[i-numOutputs-1]<minInput[i-numOutputs-1])
					minInput[i-numOutputs-1] = input[i-numOutputs-1];
				if(input[i-numOutputs-1]>maxInput[i-numOutputs-1])
					maxInput[i-numOutputs-1] = input[i-numOutputs-1];
			}
			if(Math.random()<DATA_LEARN_FRAC)
				dataLearn.add(new double[][]{input, output});
			else
				dataValidate.add(new double[][]{input, output});
			line = reader.readLine();
		}
		System.out.println("rrrrrrrrrrrrrrrrrrrrrrr");
		reader.close();
		for(double[][] datum:dataLearn) {
			for(int i=0; i<numOutputs; i++)
				datum[1][i] = (datum[1][i]-minOutput[i])/(maxOutput[i]-minOutput[i]);
			for(int i=0; i<numInputs; i++)
				datum[0][i] = (datum[0][i]-minInput[i])/(maxInput[i]-minInput[i]);
		}
		for(double[][] datum:dataValidate) {
			for(int i=0; i<numOutputs; i++)
				datum[1][i] = (datum[1][i]-minOutput[i])/(maxOutput[i]-minOutput[i]);
			for(int i=0; i<numInputs; i++)
				datum[0][i] = (datum[0][i]-minInput[i])/(maxInput[i]-minInput[i]);
		}
		for(int i=0; i<new Integer(args[5]);i++) {
			for(double[][] datum:dataLearn)
				network.learn(datum[0], datum[1], new Double(args[6]));
			if(i%100==0) {
				double error = 0;
				for(double[][] datum:dataValidate)
					error+=Math.pow(datum[1][0]-network.getOutput(datum[0])[0], 2);
				System.out.println(i+"\t"+error/dataValidate.size());
				error = 0;
				for(double[][] datum:dataLearn)
					error+=Math.pow(datum[1][0]-network.getOutput(datum[0])[0], 2);
				System.out.println(i+"\t"+error/dataLearn.size());
			}
		}
		int i=0;
		for(double[][] datum:dataValidate)
			if(i++<50)
				System.out.println(network.getOutput(datum[0])[0]+" "+datum[1][0]);
	}

}
