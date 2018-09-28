package com.hpe.autoframework;

import java.sql.Types;

import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;

public class ExtDataTypeFactory extends DefaultDataTypeFactory {
	
	@Override
	public DataType createDataType(int sqlType, String sqlTypeName)
			throws DataTypeException {
		if (sqlType == Types.DATE) {
			return DataType.VARCHAR;
		} else if (sqlType == Types.TIMESTAMP) {
			return DataType.VARCHAR;
		} else {
			return super.createDataType(sqlType, sqlTypeName);
		}
	}
}
