package playground.sergioo.neuralNetwork;

import playground.sergioo.neuralNetwork.neurons.Neuron;
import playground.sergioo.neuralNetwork.neurons.Perceptron;

public class FullNeuralNetwork extends NeuralNetwork {

	private boolean[][] connections;
	private int numInputs;
	private int numOutputs;
	
	public FullNeuralNetwork(int numInputs, int numNeurons, int numOutputs) {
		super();
		this.numInputs = numInputs;
		this.numOutputs = numOutputs;
		connections = new boolean[numInputs+numNeurons+numOutputs][numInputs+numNeurons+numOutputs];
		for(int i=numInputs; i<connections.length-numOutputs; i++)
			for(int j=0; j<numInputs; j++)
				connections[i][j] = true;
		for(int i=connections.length-numOutputs; i<connections.length; i++)
			for(int j=numInputs; j<connections.length-numOutputs; j++)
				connections[i][j] = true;
		createNetwork();
	}
	
	public FullNeuralNetwork(int numInputs, int numNeurons, int numOutputs, int[] layers) {
		super();
		this.numInputs = numInputs;
		this.numOutputs = numOutputs;
		connections = new boolean[numInputs+numNeurons+numOutputs][numInputs+numNeurons+numOutputs];
		int layersNumber = 0;
		for(int l=0; l<layers.length; l++) {
			for(int i=numInputs+layersNumber; i<numInputs+layersNumber+layers[l]; i++)
				for(int j=layersNumber; j<layersNumber+(l==0?numInputs:layers[l]); j++)
					connections[i][j] = true;
			layersNumber += layers[l];
		}
		for(int i=connections.length-numOutputs; i<connections.length; i++)
			for(int j=numInputs; j<connections.length-numOutputs; j++)
				connections[i][j] = true;
		createNetwork();
	}

	private void createNetwork() {
		neurons = new Perceptron[connections.length-numInputs];
		for(int i=numInputs; i<connections.length; i++) {
			int num = 0;
			for(int j=0; j<connections.length-numOutputs; j++)
				if(connections[i][j])
					num++;
			neurons[i-numInputs] = new Perceptron(num);
		}
	}
	public void recombinate(boolean[][] connections, double p) {
		for(int i=numInputs; i<connections.length; i++)
			for(int j=0; j<connections.length-numOutputs; j++)
				if(Math.random()<p)
					this.connections[i][j] = connections[i][j];
		createNetwork();
	}
	public void mutate(double p) {
		for(int i=numInputs; i<connections.length; i++)
			for(int j=0; j<connections.length-numOutputs; j++)
				if(i!=j && Math.random()<p) {
					connections[i][j] = !connections[i][j];
					if(connections[i][j] && i<connections.length-numOutputs && j>numInputs)
						connections[j][i] = false;
				}
		for(int j=0; j<connections.length; j++) {
			for(int i=0; i<connections.length; i++)
				if(connections[i][j])
					System.out.print("1 ");
				else
					System.out.print("0 ");
			System.out.println();
		}
		createNetwork();
	}
	public void mutateLayer(double p) {
		for(int i=numInputs; i<connections.length-numOutputs; i++)
			for(int j=0; j<numInputs; j++)
				if(Math.random()>p)
					connections[i][j] = !connections[i][j];
		for(int i=connections.length-numOutputs; i<connections.length; i++)
			for(int j=numInputs; j<connections.length-numOutputs; j++)
				if(Math.random()<p)
					connections[i][j] = !connections[i][j];
		createNetwork();
	}
	public void mutateNoShortCut(double p) {
		for(int i=numInputs; i<connections.length; i++)
			for(int j=0; j<connections.length-numOutputs; j++)
				if((i<connections.length-numOutputs || j>=numInputs) && Math.random()<p)
					connections[i][j] = !connections[i][j];
		createNetwork();
	}
	@Override
	public void learn(double[] input, double[] realOutput, double learningRate) {
		double[] output = getOutput(input);
		double[] gradError = new double[neurons.length];
		for(int i=0; i<gradError.length; i++)
			gradError[i] = Double.NaN;
		int numInner = connections.length-numInputs-numOutputs;
		for(int j=0; j<output.length; j++) {
			Neuron neuron = neurons[numInner+j];
			double grad = 0;
			switch(neuron.getTypeActivation()) {
			case SIGMOID:
				grad = (output[j]-realOutput[j])*output[j]*(1-output[j]);
				break;
			case TANH:
				//TODO
				grad = (output[j]-realOutput[j])*1;
				break;
			}
			double[] weights = neuron.getWeights();
			double[] inputs = neuron.getInputs();
			for(int i=0; i<weights.length; i++)
				weights[i] -= learningRate*inputs[i]*grad;
			gradError[numInner+j] = grad;
		}
		int numRemainder = numInner;
		int j=numInputs;
		while(numRemainder>0) {
			Neuron neuron = neurons[j-numInputs];
			boolean all = true;
			double sum = 0;
			for(int i=j+1; i<connections.length && all; i++) {
				int index = i-numInputs;
				if(connections[i][j] && Double.isNaN(gradError[index]))
					all = false;
				else if(connections[i][j])
					sum += gradError[index]*getWeight(neurons[index].getWeights(), i, j);
			}
			if(all) {
				double[] weights = neuron.getWeights();
				double[] inputs = neuron.getInputs();
				double outputN = neuron.getOutput(inputs);
				gradError[j-numInputs] = sum*outputN*(1-outputN);
				for(int i=0; i<weights.length; i++)
					weights[i] -= learningRate*inputs[i]*gradError[j-numInputs];
				numRemainder--;
			}
			j++;
			if(j>=connections.length-numOutputs)
				j=numInputs;
		}
	}
	private double getWeight(double[] weights, int indexN, int index) {
		int w = 0;
		for(int j=0; j<index; j++)
			if(connections[indexN][j])
				w++;
		return weights[w];
	}

	@Override
	public double[] getOutput(double[] input) {
		int numReminder = connections.length-numInputs;
		double[] results = new double[connections.length];
		for(int i=0; i<numInputs; i++)
			results[i] = input[i];
		for(int i=numInputs; i<results.length; i++)
			results[i] = Double.NaN;
		int i=numInputs;
		while(numReminder>0) {
			if(Double.isNaN(results[i])) {
				Neuron neuron = neurons[i-numInputs];
				double[] inputs = new double[neuron.getWeights().length];
				boolean all = true;
				int k=0;
				for(int j=0; j<connections.length-numOutputs && all; j++)
					if(connections[i][j] && Double.isNaN(results[j]))
						all = false;
					else if(connections[i][j])
						inputs[k++] = results[j];
				if(all) {
					results[i] = neuron.getOutput(inputs);
					numReminder--;
				}
			}
			i++;
			if(i>=connections.length)
				i=numInputs;
		}
		double[] finalResult = new double[numOutputs];
		for(i=0; i<numOutputs; i++)
			finalResult[i] = results[connections.length-numOutputs+i];
		return finalResult;
	}
	
}
