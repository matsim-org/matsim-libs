package playground.julia.interpolation;

public class NeighbourInterpolation {

	public Double [][] getNeighbourMatrix(Double[][] matrix){
		Double[][] intmatrix = new Double[matrix.length][matrix[0].length];
		
		for (int i=0;i<matrix.length; i++){
			for(int j=0; j<matrix.length;j++){
				//case value
				if(matrix[i][j]!=null)intmatrix[i][j]=matrix[i][j];
				//case no value
				else{
					int numberOfNeighbours =0;
					Double sumOfWeights =0.0;
					//number of direct neighbours with value != null
					for(int k= i-1; k<=i+1; k++){
						for(int l= j-1; l<=i+1; l++){
							if(matrix[k][l]!=null){
								numberOfNeighbours++;
								sumOfWeights+=matrix[k][l];
							}
						}
					}
					intmatrix[i][j]=sumOfWeights/numberOfNeighbours;
				}
		
			}
		}
		
		return intmatrix;
	}
}
