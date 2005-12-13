package clus.ext.hierarchical;

import java.io.*;

import clus.io.*;
import clus.main.Settings;
import clus.util.*;
import clus.data.rows.*;

public class FlatClassesAttrType extends ClassesAttrType {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected ClassesAttrType m_Mimic;

	public FlatClassesAttrType(String name, ClassesAttrType mimic) {
		super(name);
		m_Mimic = mimic;
	}
	
	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}
	
	public class MySerializable extends RowSerializable {

		public MySerializable(RowData data) {
			super(data);
		}

		public void read(ClusReader data, DataTuple tuple) throws IOException {
			ClassesTuple other = (ClassesTuple)tuple.getObjVal(m_Mimic.getArrayIndex());
			tuple.setObjectVal(other.toFlat(m_Table), getArrayIndex());
		}
	}
}
