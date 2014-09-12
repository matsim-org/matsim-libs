package playground.sergioo.neuralNetwork;

import playground.sergioo.neuralNetwork.neurons.Neuron;
import playground.sergioo.neuralNetwork.neurons.Perceptron;
import playground.sergioo.neuralNetwork.neurons.Neuron.TypeNeuron;

public class NeuralNetwork {
	
	protected Neuron[] neurons;
	
	public NeuralNetwork() {
		
	}
	
	public NeuralNetwork(int numInputs, int numOutputs) {
		neurons = new Perceptron[numOutputs];
		for(int i=0; i<numOutputs; i++)
			neurons[i] = new Perceptron(numInputs);
	}
	
	public NeuralNetwork(int numInputs, int numOutputs, TypeNeuron typeNeuron) {
		switch(typeNeuron) {
		case PERCEPTRON:
			neurons = new Perceptron[numOutputs];
			for(int i=0; i<numOutputs; i++)
				neurons[i] = new Perceptron(numInputs);
			break;
		}
	}
	
	public void learn(double[] input, double[] realOutput, double learningRate) {
		
	}

	public double[] getOutput(double[] input) {
		return null;
	}

}
