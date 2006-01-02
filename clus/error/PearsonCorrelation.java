package clus.error;

import java.io.*;
import java.text.*;

import clus.data.rows.DataTuple;
import clus.data.type.NumericAttrType;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class PearsonCorrelation extends ClusNumericError {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected double[] m_SumPi, m_SumSPi;
	protected double[] m_SumAi, m_SumSAi;
	protected double[] m_SumPiAi;

	public PearsonCorrelation(ClusErrorParent par, NumericAttrType[] num) {
		super(par, num);
		m_SumPi = new double[m_Dim];
		m_SumSPi = new double[m_Dim];
		m_SumAi = new double[m_Dim];
		m_SumSAi = new double[m_Dim];
		m_SumPiAi = new double[m_Dim];
	}
	
	public void reset() {
		for (int i = 0; i < m_Dim; i++) {
			m_SumPi[i] = 0.0;
			m_SumSPi[i] = 0.0;
			m_SumAi[i] = 0.0;
			m_SumSAi[i] = 0.0;
			m_SumPiAi[i] = 0.0;			
		}
	}

	public boolean shouldBeLow() {
		return false;
	}

	public double getCorrelation(int i) {
		int nb = getNbExamples();		
		double Pi_ss = m_SumSPi[i]-m_SumPi[i]*m_SumPi[i]/nb;
		double Ai_ss = m_SumSAi[i]-m_SumAi[i]*m_SumAi[i]/nb;
		double root = Math.sqrt(Pi_ss*Ai_ss);
		double above = m_SumPiAi[i] - m_SumPi[i]*m_SumAi[i]/nb;
		return above/root;		
	}
	
	public double getModelErrorComponent(int i) {
		return getCorrelation(i);
	}

	public double getModelError() {
		double mean = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			mean += getCorrelation(i);
		}
		return mean/m_Dim;
	}

	public void addExample(double[] real, double[] predicted) {
		for (int i = 0; i < m_Dim; i++) {
			// Predicted
			m_SumPi[i] += predicted[i];
			m_SumSPi[i] += predicted[i] * predicted[i];
			// Real
			m_SumAi[i] += real[i];
			m_SumSAi[i] += real[i] * real[i];
			// Cross real, predicted
			m_SumPiAi[i] += predicted[i] * real[i];
		}
	}
	
	public void addInvalid(DataTuple tuple) {
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double[] predicted = pred.getNumericPred();
		for (int i = 0; i < m_Dim; i++) {
				double real_i = getAttr(i).getNumeric(tuple);
				// Predicted
				m_SumPi[i] += predicted[i];
				m_SumSPi[i] += predicted[i] * predicted[i];
				// Real
				m_SumAi[i] += real_i;
				m_SumSAi[i] += real_i * real_i;
				// Cross real, predicted
				m_SumPiAi[i] += predicted[i] * real_i;	
		}		
	}		

	public void add(ClusError other) {
		PearsonCorrelation oe = (PearsonCorrelation)other;
		for (int i = 0; i < m_Dim; i++) {
			// Predicted
			m_SumPi[i] += oe.m_SumPi[i];
			m_SumSPi[i] += oe.m_SumSPi[i];
			// Real
			m_SumAi[i] += oe.m_SumAi[i];
			m_SumSAi[i] += oe.m_SumSAi[i];
			// Cross real, predicted
			m_SumPiAi[i] += oe.m_SumPiAi[i];
		}
	}

	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr = getFormat();
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		int nb = getNbExamples();
		for (int i = 0; i < m_Dim; i++) {
			double Pi_ss = m_SumSPi[i]-m_SumPi[i]*m_SumPi[i]/nb;
			double Ai_ss = m_SumSAi[i]-m_SumAi[i]*m_SumAi[i]/nb;
			double root = Math.sqrt(Pi_ss*Ai_ss);
			double above = m_SumPiAi[i] - m_SumPi[i]*m_SumAi[i]/nb;
			double el = above/root;
			if (i != 0) buf.append(",");
			buf.append(fr.format(el));
		}
		buf.append("]");
		/*
		{
			double Pi_ss = m_SumSPi[0]-m_SumPi[0]*m_SumPi[0]/nb;
			double Ai_ss = m_SumSAi[0]-m_SumAi[0]*m_SumAi[0]/nb;
			double root = Math.sqrt(Pi_ss*Ai_ss);
			double above = m_SumPiAi[0] - m_SumPi[0]*m_SumAi[0]/nb;

			buf.append("] "+getNbExamples()+" "+Pi_ss+" "+Ai_ss+" "+above+" "+root);
		}*/
		out.println(buf.toString());
	}

	public boolean hasSummary() {
		return false;
	}

	public String getName() {
		return "Pearson correlation coefficient";
	}

	public ClusError getErrorClone(ClusErrorParent par) {
		return new PearsonCorrelation(par, m_Attrs);
	}
}

