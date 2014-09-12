package playground.sergioo.neuralNetwork;

import playground.sergioo.neuralNetwork.neurons.Neuron.TypeNeuron;

public class LayersNeuralNetwork extends NeuralNetwork {
	
	private NeuralNetwork[] layers;
	
	public LayersNeuralNetwork(int[] layerSizes) {
		super(layerSizes[layerSizes.length-2], layerSizes[layerSizes.length-1]);
		layers = new NeuralNetwork[layerSizes.length-1];
		for(int i=0; i<layers.length-1; i++)
			layers[i] = new NeuralNetwork(layerSizes[i], layerSizes[i+1]);
	}
	
	public LayersNeuralNetwork(int[] layerSizes, TypeNeuron layerType) {
		super(layerSizes[layerSizes.length-2], layerSizes[layerSizes.length-1], layerType);
		layers = new NeuralNetwork[layerSizes.length-1];
		for(int i=0; i<layers.length-1; i++)
			layers[i] = new NeuralNetwork(layerSizes[i], layerSizes[i+1], layerType);
	}
	
	public LayersNeuralNetwork(int[] layerSizes, TypeNeuron[] layerTypes) {
		super(layerSizes[layerSizes.length-2], layerSizes[layerSizes.length-1]);
		layers = new NeuralNetwork[layerSizes.length-1];
		for(int i=0; i<layers.length-1; i++)
			layers[i] = new NeuralNetwork(layerSizes[i], layerSizes[i+1], layerTypes[i]);
	}

}
