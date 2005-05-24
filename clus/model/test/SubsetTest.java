package clus.model.test;

import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;

public class SubsetTest extends NodeTest {

	protected int[] m_Values;
	protected NominalAttrType m_Type;
	protected double m_PosFreq;
	protected int m_MissIndex;
	
	public SubsetTest(NominalAttrType attr, int nb, boolean[] isin, double posfreq) {
		m_Type = attr;
		setArity(2);
		setPosFreq(posfreq);
		m_Values = initValues(nb, isin);
		m_MissIndex = attr.getNbValues();
	}

	// This constructor does not initialize posfreq !
	public SubsetTest(NominalAttrType attr, int nb) {
		setArity(2);		
		m_Type = attr;
		m_Values = new int[nb];		
		m_MissIndex = attr.getNbValues();
	}
	
	public ClusAttrType getType() {
		return m_Type;
	}		

	public void setType(ClusAttrType type) {
		m_Type = (NominalAttrType)type;
	}

	public String getString() {
		if (m_Values.length == 1) {
			return m_Type.getName()+" = "+m_Type.getValue(m_Values[0]);
		} else {
			StringBuffer buffer = new StringBuffer();
			buffer.append(m_Type.getName());
			if (m_Values.length == 0) {
				buffer.append(" in ?");
			} else {
				buffer.append(" in {");
				for (int i = 0; i < m_Values.length; i++) {
					if (i != 0) buffer.append(",");
					buffer.append(m_Type.getValue(m_Values[i]));
				}
				buffer.append("}");
			}
			return buffer.toString();
		}
	}
	
	public boolean hasConstants() {
		return m_Values.length > 0;
	}
	
	public int getNbValues() {
		return m_Values.length;
	}
	
	public int getValue(int i) {
		return m_Values[i];
	}

	public void setValue(int idx, int val) {
		m_Values[idx] = val;
	}
	
	public boolean equals(NodeTest test) {
		// Other test is of different type
		if (m_Type != test.getType()) return false;
		SubsetTest ntest = (SubsetTest)test;
		// Values are different
		int nb = m_Values.length;
		int[] ovalues = ntest.m_Values;
		if (nb != ovalues.length) return false;
		for (int i = 0; i < nb; i++) {
			if (m_Values[i] != ovalues[i]) return false;		
		}
		return true;
	}	
	
	public int hashCode() {
		int code = m_Type.getIndex()*1000;
		for (int i = 0; i < m_Values.length; i++) {
			code += m_Values[i];
		}
		return code + m_Values.length;
	}
	
	public int nominalPredict(int value) {
		// Missing value
		if (value == m_MissIndex) 
			return ClusRandom.nextDouble(ClusRandom.RANDOM_TEST_DIR) < m_PosFreq ? 
			       ClusNode.YES : ClusNode.NO;
		// Regular value
		for (int i = 0; i < m_Values.length; i++)
			if (m_Values[i] == value) return ClusNode.YES;
		return ClusNode.NO;
	}
	
	public int nominalPredictWeighted(int value) {
		// Missing value
		if (value == m_MissIndex) return hasUnknownBranch() ? ClusNode.UNK : UNKNOWN;
		// Regular value
		for (int i = 0; i < m_Values.length; i++)
			if (m_Values[i] == value) return ClusNode.YES;
		return ClusNode.NO;
	}	
	
	public int predictWeighted(DataTuple tuple) {
		int val = tuple.m_Ints[m_Type.getSpecialIndex()];
		return nominalPredictWeighted(val);
	}	
	
	public NodeTest getBranchTest(int i) {
		if (i == ClusNode.YES) {
			return this;
		} else {
			int pos = 0;
			int nb = m_Type.getNbValues() - getNbValues();
			SubsetTest test = new SubsetTest(m_Type, nb);
			boolean[] isin = getIsInArray();
			for (int j = 0; j < isin.length; j++) {
				if (!isin[j]) {
					test.setValue(pos++, j);
				}
			}
			test.setPosFreq(1.0 - getPosFreq());
			return test;
		}
	}
	
	public NodeTest simplifyConjunction(NodeTest other) {
		if (getType() != other.getType()) {
			return null;
		} else {
			if (other instanceof SubsetTest) {
				SubsetTest oset = (SubsetTest)other;
				boolean[] isin_me = getIsInArray();
				boolean[] isin_other = oset.getIsInArray();
				int count = 0;
				for (int i = 0; i < isin_me.length; i++) {
					if (isin_me[i] && isin_other[i]) count++;
				}
				int pos = 0;
				SubsetTest test = new SubsetTest(m_Type, count);
				for (int i = 0; i < isin_me.length; i++) {
					if (isin_me[i] && isin_other[i]) test.setValue(pos++, i);
				}
				test.setPosFreq(Math.min(getPosFreq(), oset.getPosFreq()));
				return test;
			} else {
				return null;
			}
		}
	}
	
	public boolean[] getIsInArray() {
		boolean[] res = new boolean[m_Type.getNbValues()];
		for (int i = 0; i < getNbValues(); i++) {
			res[getValue(i)] = true;
		}
		return res;
	}

/*	public int predict(ClusAttribute attr, int idx) {
		return nominalPredict(((NominalAttribute)attr).m_Data[idx]);
	}	
*/	
	private int[] initValues(int nb, boolean[] isin) {
		int i = 0;		
		int[] values = new int[nb];
		for (int j = 0; j < isin.length; j++) if (isin[j]) values[i++] = j;
		return values;
	}
}
