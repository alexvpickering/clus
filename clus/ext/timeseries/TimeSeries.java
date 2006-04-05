package clus.ext.timeseries;

public class TimeSeries {
	private double[] values;
	

	public TimeSeries(double[] values){
		this.values = new double[values.length];
		System.arraycopy(values, 0, this.values, 0, values.length); 
	}

	public int length(){
		if (values==null)
			return 0;
		return values.length;
	}


	public double[] getValues() {
		double[] result = new double[values.length];
		System.arraycopy(values, 0, result, 0, values.length);
		return result;
	}


	public void setValues(double[] values) {
		System.arraycopy(values, 0, this.values, 0, values.length);
	}
	
	

}
