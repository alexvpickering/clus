package clus.error;

import clus.data.rows.DataTuple;
import clus.data.type.NumericAttrType;
import clus.data.type.TimeSeriesAttrType;
import clus.ext.timeseries.TimeSeries;
import clus.ext.timeseries.TimeSeriesStat;
import clus.statistic.ClusStatistic;

public class QDMRMSError extends ClusTimeSeriesError {

	protected double m_SumErr;
	protected double m_SumSqErr;

	public QDMRMSError(ClusErrorParent par, TimeSeriesAttrType[] ts) {
		super(par, ts);
	}

	public void add(ClusError other) {
		QDMRMSError oe = (QDMRMSError)other;
			m_SumErr += oe.m_SumErr;
			m_SumSqErr += oe.m_SumSqErr;
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		TimeSeries predicted = pred.getTimeSeriesPred();
		double err = sqr(((TimeSeriesStat)pred).calcDistance(getAttr(0).getTimeSeries(tuple),  predicted));			
		m_SumErr += err;
		m_SumSqErr += sqr(err);
	}

	public final static double sqr(double value) {
		return value*value;
	}
	
	
	public TimeSeriesAttrType getAttr(int i) {
		return m_Attrs[i];
	}

	public void addInvalid(DataTuple tuple) {
		// TODO Auto-generated method stub
		
	}

	public ClusError getErrorClone(ClusErrorParent par) {
		// TODO Auto-generated method stub
		return new QDMRMSError(par, m_Attrs);
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "QDMRMSError";
	}

	public double getModelErrorComponent(int i) {
		int nb = getNbExamples();
		double err = nb != 0 ? m_SumErr/nb : 0.0;
		return Math.sqrt(err);
	}


}
