package com.hpe.autoframework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.excel.XlsDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.testng.TestException;

public class DBClient {

	private static final String FILENAME_EXT = ".xls";
	
	private static AtomicInteger queryResultId = new AtomicInteger(1);

	protected JdbcDatabaseTester dbTester_;
	
	static {
		org.apache.log4j.BasicConfigurator.configure(new NullAppender());
		Logger.getRootLogger().setLevel(Level.ERROR);
	}
	
	public DBClient() {
		
	}

	public void init(String driver, String url, String user, String password, String schema) {
		try {
			if (schema == null)
				dbTester_ = new JdbcDatabaseTester(driver, url, user, password);
			else
				dbTester_ = new JdbcDatabaseTester(driver, url, user, password, schema);
		} catch (Exception exp) {
			exp.printStackTrace();
			throw new TestException("DB connecting failed", exp);
		}
	}
	
	public void init(String driver, String url, String user, String password) {
		init(driver, url, user, password, null);
	}
	
	public ITable query(String resultname, String query) {
		IDatabaseConnection dbcon = null;
		OutputStream ous = null;
		ITable table = null;
		try {
			dbcon = dbTester_.getConnection();
			dbcon.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ExtDataTypeFactory());
			table = dbcon.createQueryTable(resultname, query);
			ous = new FileOutputStream(ExProgressFormatter.getEvidenceDirName().resolve(resultname + FILENAME_EXT).toString());
			new ExXlsDataSetWriter().write(new DefaultDataSet(table), ous);
		} catch (Exception exp) {
			throw new TestException("DB access failed", exp);
		} finally {
			try {
				if (ous != null)
					ous.close();
				if (dbcon != null)
					dbcon.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return table;
	}
	
	public DBSnapshot snapshot(String query, String[] keys, String resultname) {
		IDatabaseConnection dbcon = null;
		OutputStream ous = null;
		ITable table = null;
		try {
			dbcon = dbTester_.getConnection();
			dbcon.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ExtDataTypeFactory());
			table = dbcon.createQueryTable("QR_" + queryResultId.getAndIncrement(), query);
		} catch (Exception exp) {
			throw new TestException("DB access failed", exp);
		} finally {
			try {
				if (ous != null)
					ous.close();
				if (dbcon != null)
					dbcon.close();
			} catch (Exception e) {
				// ignore
			}
		}
		
		DBSnapshot snapshot = new DBSnapshot(this, query, keys, resultname, table);
		return snapshot;
	}

	public ITable assertEqual(String expectExcelfilename, String tablename, String resultname, String query) {
		return assertEqual(expectExcelfilename, tablename, resultname, query, new String[0], new String[0]);
	}
	
	public ITable assertEqual(String expectExcelfilename, String tablename, String resultname, String query, String[] keys) {
		return assertEqual(expectExcelfilename, tablename, resultname, query, keys, new String[0]);
	}
	
	public ITable assertEqual(String expectExcelfilename, String tablename, String resultname, String query, String[] keys, String[] ignoreColumns) {
		IDataSet dataSet;
		IDatabaseConnection dbcon = null;
		OutputStream ous = null;
		ITable table = null;
		ITable exptable;
		boolean issame = false;
		try {
			dataSet = new XlsDataSet(new File(expectExcelfilename));
			exptable = dataSet.getTable(tablename);
			
			dbcon = dbTester_.getConnection();
			dbcon.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ExtDataTypeFactory());
			table = dbcon.createQueryTable(resultname, query);
			
			ous = new FileOutputStream(ExProgressFormatter.getEvidenceDirName().resolve(resultname + FILENAME_EXT).toString());
			if (ignoreColumns == null)
				ignoreColumns = new String[0];
			//issame = new DiffXlsDataSetWriter().write(table, exptable, keys, ignoreColumns, ous, false);
			DiffXlsDataSetWriter writer = new DiffXlsDataSetWriter();
			writer.write(table, exptable, keys, ignoreColumns, false);
			issame = writer.isSame();
			writer.getResultBook().write(ous);
		} catch (Exception exp) {
			throw new TestException("DB access failed", exp);
		} finally {
			try {
				if (ous != null)
					ous.close();
				if (dbcon != null)
					dbcon.close();
			} catch (Exception e) {
				// ignore
			}
		}
		if (!issame)
			throw new TestException("Query(" + query + ") is different from the expected:" + expectExcelfilename + " " + tablename);
		return table;
	}
	
	public ITable diff(ITable exptable, String resultname, String query) {
		return diff(exptable, resultname, query, new String[0]);
	}
	
	public ITable diff(ITable exptable, String resultname, String query, boolean diffOnly) {
		return diff(exptable, resultname, query, new String[0], diffOnly);
	}
	
	public ITable diff(ITable exptable, String resultname, String query, String[] keys) {
		return diff(exptable, resultname, query, keys, true);
	}
	
	public ITable diff(ITable exptable, String resultname, String query, String[] keys, boolean diffOnly) {
		assert exptable != null : "Snapshot has not been taken";
		
		IDatabaseConnection dbcon = null;
		OutputStream ous = null;
		ITable table = null;
		try {
			dbcon = dbTester_.getConnection();
			dbcon.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ExtDataTypeFactory());
			table = dbcon.createQueryTable(resultname, query);
			
			ous = new FileOutputStream(ExProgressFormatter.getEvidenceDirName().resolve(resultname + FILENAME_EXT).toString());
			//new DiffXlsDataSetWriter().write(table, exptable, keys, ous, diffOnly);
			DiffXlsDataSetWriter writer = new DiffXlsDataSetWriter();
			writer.write(table, exptable, keys, diffOnly);
			writer.getResultBook().write(ous);
		} catch (Exception exp) {
			throw new TestException("DB access failed", exp);
		} finally {
			try {
				if (ous != null)
					ous.close();
				if (dbcon != null)
					dbcon.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return table;
	}
	
	public void setupByExcelFileWithCleanInsert(String excelfilename) {
		IDataSet dataSet;
		try {
			dataSet = new XlsDataSet(new File(excelfilename));
			dbTester_.setDataSet(dataSet);
			dbTester_.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
			dbTester_.onSetup();
		} catch (Exception e) {
			throw new TestException("DB setup failed", e);
		}
	}
	
	public void setupByExcelFileWithRefresh(String excelfilename) {
		IDataSet dataSet;
		try {
			dataSet = new XlsDataSet(new File(excelfilename));
			dbTester_.setDataSet(dataSet);
			dbTester_.setSetUpOperation(DatabaseOperation.REFRESH);
			dbTester_.onSetup();
		} catch (Exception e) {
			throw new TestException("DB setup failed", e);
		}
	}
	
	public void setupByExcelFileWithDelete(String excelfilename) {
		IDataSet dataSet;
		try {
			dataSet = new XlsDataSet(new File(excelfilename));
			dbTester_.setDataSet(dataSet);
			dbTester_.setSetUpOperation(DatabaseOperation.DELETE);
			dbTester_.onSetup();
		} catch (Exception e) {
			throw new TestException("DB setup failed", e);
		}
	}
	
	public void setupTruncateTables(String[] tables) {
		DefaultDataSet dataSet;
		try {
			dataSet = new DefaultDataSet();
			for (String tablename: tables) {
				dataSet.addTable(new DefaultTable(tablename));
			}
			dbTester_.setDataSet(dataSet);
			dbTester_.setSetUpOperation(DatabaseOperation.TRUNCATE_TABLE);
			dbTester_.onSetup();
		} catch (Exception e) {
			throw new TestException("DB setup failed", e);
		}
	}
	
	public static void assertRowCount(ITable table, int rows) {
		assert table.getRowCount() == rows;
	}

	public static void assertValue(ITable table, int row, String column, String value) {
		try {
			DataType datatype = getDataType(table, column);
			assert datatype != null;
			if (datatype.equals(DataType.CHAR))
				assert ((String)table.getValue(row, column)).trim().equals(value);
			else if (datatype.equals(DataType.VARCHAR))
				assert ((String)table.getValue(row, column)).trim().equals(value);
			else if (datatype.equals(DataType.NCHAR))
				assert ((String)table.getValue(row, column)).trim().equals(value);
			else if (datatype.equals(DataType.NVARCHAR))
				assert ((String)table.getValue(row, column)).trim().equals(value);
			else if (datatype.equals(DataType.LONGNVARCHAR))
				assert ((String)table.getValue(row, column)).trim().equals(value);
			else if (datatype.equals(DataType.TIMESTAMP))
				assert ((Timestamp)table.getValue(row, column)).equals(Timestamp.valueOf(value));
			else if (datatype.equals(DataType.DATE))
				assert ((Date)table.getValue(row, column)).equals(Date.valueOf(value));
			else if (datatype.equals(DataType.TIME))
				assert ((Time)table.getValue(row, column)).equals(Time.valueOf(value));
			else if (datatype.equals(DataType.CLOB))
				assert (table.getValue(row, column)).toString().equals(value);
			else if (datatype.equals(DataType.BOOLEAN))
				assert (table.getValue(row, column)).toString().equalsIgnoreCase(value);
			else
				assert false;
			
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
	
	public static void assertValue(ITable table, int row, String column, int value) {
		try {
			DataType datatype = getDataType(table, column);
			assert datatype != null;
			if (datatype.equals(DataType.INTEGER))
				assert ((Integer)table.getValue(row, column)).intValue() == value;
			else if (datatype.equals(DataType.TINYINT))
				assert ((Integer)table.getValue(row, column)).intValue() == value;
			else if (datatype.equals(DataType.SMALLINT))
				assert ((Integer)table.getValue(row, column)).intValue() == value;
			else if (datatype.equals(DataType.NUMERIC))
				assert ((BigDecimal)table.getValue(row, column)).intValue() == value;
			else if (datatype.equals(DataType.DECIMAL))
				assert ((BigDecimal)table.getValue(row, column)).intValue() == value;
			else if (datatype.equals(DataType.BIGINT))
				assert ((BigDecimal)table.getValue(row, column)).intValue() == value;
			else
				assert false;
			
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
	
	public static void assertValue(ITable table, int row, String column, long value) {
		try {
			DataType datatype = getDataType(table, column);
			assert datatype != null;
			if (datatype.equals(DataType.NUMERIC))
				assert ((BigDecimal)table.getValue(row, column)).longValue() == value;
			else if (datatype.equals(DataType.DECIMAL))
				assert ((BigDecimal)table.getValue(row, column)).longValue() == value;
			else if (datatype.equals(DataType.BIGINT))
				assert ((BigDecimal)table.getValue(row, column)).longValue() == value;
			else
				assert false;
			
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
	
	public static void assertValue(ITable table, int row, String column, double value) {
		try {
			DataType datatype = getDataType(table, column);
			assert datatype != null;
			if (datatype.equals(DataType.NUMERIC))
				assert ((BigDecimal)table.getValue(row, column)).doubleValue() == value;
			else if (datatype.equals(DataType.REAL))
				assert ((Float)table.getValue(row, column)).doubleValue() == value;
			else if (datatype.equals(DataType.FLOAT))
				assert ((Double)table.getValue(row, column)).doubleValue() == value;
			else if (datatype.equals(DataType.DOUBLE))
				assert ((Double)table.getValue(row, column)).doubleValue() == value;
			else
				assert false;
			
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
	
	public static void assertValue(ITable table, int row, String column, byte[] value) {
		try {
			DataType datatype = getDataType(table, column);
			assert datatype != null;
			if (datatype.equals(DataType.BINARY))
				assert Arrays.equals((byte[])table.getValue(row, column), value);
			else if (datatype.equals(DataType.VARBINARY))
				assert Arrays.equals((byte[])table.getValue(row, column), value);
			else if (datatype.equals(DataType.LONGVARBINARY))
				assert Arrays.equals((byte[])table.getValue(row, column), value);
			else if (datatype.equals(DataType.BLOB))
				assert Arrays.equals((byte[])table.getValue(row, column), value);
			else
				assert false;
			
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
	
	private static DataType getDataType(ITable table, String column) {
		DataType datatype = null;
		try {
			ITableMetaData metadata = table.getTableMetaData();
			Column[] cols = metadata.getColumns();
			for (Column col : cols) {
				if (col.getColumnName().equalsIgnoreCase(column)) {
					datatype = col.getDataType();
					break;
				}
			}
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
		return datatype;
	}
	
	public static void assertNullValue(ITable table, int row, String column) {
		try {
			assert table.getValue(row, column) == null;
		} catch (DataSetException exp) {
			exp.printStackTrace();
			throw new TestException("DB assertion failed", exp);
		}
	}
}
