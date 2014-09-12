package playground.sergioo.neuralNetwork.neurons;


public class Perceptron extends Neuron {
	
	public Perceptron(int numInputs) {
		super(numInputs);
	}

	@Override
	public double getOutput(double[] inputs) {
		this.inputs = inputs;
		double sum = 0;
		for(int i=0; i<weights.length; i++)
			sum += weights[i]*inputs[i];
		return activation(sum);
	}

}
