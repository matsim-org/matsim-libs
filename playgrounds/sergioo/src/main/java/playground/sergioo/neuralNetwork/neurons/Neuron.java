package playground.sergioo.neuralNetwork.neurons;

public abstract class Neuron {
	
	public enum TypeNeuron {
		PERCEPTRON;
	}
	public enum TypeActivationFuntion {
		SIGMOID, TANH;
	}
	
	protected double[] weights;
	protected double[] inputs;
	private TypeActivationFuntion typeActivationFuntion = TypeActivationFuntion.SIGMOID;

	public Neuron(int numInputs) {
		weights = new double[numInputs];
		for(int i=0; i<numInputs; i++)
			weights[i] = Math.random();
	}
	
	public abstract double getOutput(double[] inputs);
	

	public double[] getWeights() {
		return weights;
	}
	public void updateWeights(double[] weights) {
		this.weights = weights;
	}
	public double[] getInputs() {
		return inputs;
	}
	public TypeActivationFuntion getTypeActivation() {
		return typeActivationFuntion;
	}
	public void setTypeActivationFuntion(TypeActivationFuntion typeActivationFuntion) {
		this.typeActivationFuntion = typeActivationFuntion;
	}
	protected double activation(double input) {
		switch(typeActivationFuntion) {
		case SIGMOID:
			return sigmoidActivation(input, 1);
		case TANH:
			return tanHActivation(input, 1);
		}
		return 0;
	}
	private double sigmoidActivation(double input, double exp) {
		return 1/(1+Math.exp(-exp*input));
	}
	private double tanHActivation(double input, double factor) {
		return Math.tanh(factor*input);
	}

}
