package com.hpe.autoframework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.TypeCastException;

public class DiffXlsDataSetWriter {
	public static final String DATE_FORMAT_AS_NUMBER_DBUNIT = "####################";
	public static final int MATCH_NOT_FOUND = 0x7fffffff;
	private static final String UPDATE_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";
    private static final int MAX_CELL_SIZE = 32767;
	private static final int MAX_ROW_CNT = 65535;

	private CellStyle nullCellStyle;

	private CellStyle dateCellStyle;
	private CellStyle diffDateCellStyle;

	private CellStyle titleCellStyle;

	private CellStyle cellStyle;
	// private CellStyle diffCellStyle;

	private CellStyle headerCellStyle;
	private CellStyle headerDiffCellStyle;

	private boolean isSame = true;
	private Workbook resultBook;

	private CellStyle titleStyle;

	private CellStyle commHeaderStyle;
	private CellStyle unmatchedHeaderStyle;
	private CellStyle ignoredHeaderStyle;
	private CellStyle pkHeaderStyle;

	private CellStyle commCellStyle;
	private CellStyle unmatchedColStyle;
	private CellStyle ignoredCellStyle;
	private CellStyle diffCellStyle;

	private String[] ignoreColumns;

	public DiffXlsDataSetWriter() {
		resultBook = createWorkbook();

		titleStyle = createTitleCellStyle(resultBook);

		commHeaderStyle = createCommonHeaderCellStyle(resultBook);
		unmatchedHeaderStyle = createUnmatchedColHeaderCellStyle(resultBook);
		ignoredHeaderStyle = createIgnoredColHeaderCellStyle(resultBook);
		pkHeaderStyle = createPkColHeaderCellStyle(resultBook);

		commCellStyle = createCellStyle(resultBook);
		diffCellStyle = createDiffCellStyle(resultBook);
		unmatchedColStyle = createUnmatchedColStyle(resultBook);
		ignoredCellStyle = createIgnoredCellStyle(resultBook);
	}

	@Deprecated
	public boolean write(ITable table, ITable expectTable, String[] keys, OutputStream out, boolean diffOnly)
			throws IOException, DataSetException {
		return write(table, expectTable, keys, new String[0], out, diffOnly);
	}

	@Deprecated
	@SuppressWarnings("static-access")
	public boolean write(ITable table, ITable expectTable, String[] keys, String[] ignoreColumns, OutputStream out, boolean diffOnly) throws IOException, DataSetException {
		boolean issame = true;
		Workbook workbook = createWorkbook();

		nullCellStyle = createNullCellStyle(workbook);
		dateCellStyle = createDateCellStyle(workbook);
		diffDateCellStyle = createDiffDateCellStyle(workbook);
		titleCellStyle = createTitleCellStyle(workbook);
		cellStyle = createCellStyle(workbook);
		diffCellStyle = createDiffCellStyle(workbook);
		headerCellStyle = createHeaderCellStyle(workbook);
		headerDiffCellStyle = createHeaderDiffCellStyle(workbook);

		ITableMetaData metaData = table.getTableMetaData();
		ITableMetaData expMetaData = expectTable.getTableMetaData();
		Column[] columns = metaData.getColumns();
		Column[] expColumns = expMetaData.getColumns();
		Sheet sheet = workbook.createSheet(metaData.getTableName());

		workbook.setSheetName(0, metaData.getTableName());

		Row headerRow = sheet.createRow(1);
		Cell cell = null;
		Cell expcell = null;
		for (int j = 0; j < Math.max(columns.length, expColumns.length); j++) {
			String colname = "";
			String expcolname = "";
			if (j < columns.length) {
				Column column = columns[j];
				cell = headerRow.createCell(j);
				colname = column.getColumnName();
			}
			if (j < expColumns.length) {
				Column expcolumn = expColumns[j];
				expcell = headerRow.createCell(j + columns.length + 1);
				expcolname = expcolumn.getColumnName();
			}
			boolean same = colname.equalsIgnoreCase(expcolname);
			if (!same)
				issame = false;
			if (j < columns.length) {
				setHeaderCellValue(cell, colname, workbook, same);
			}
			if (j < expColumns.length) {
				setHeaderCellValue(expcell, expcolname, workbook, same);
			}
		}

		int rowindex = 0;
		int exprowindex = 0;
		int i = 2;
		while (rowindex < table.getRowCount() || exprowindex < expectTable.getRowCount()) {
			Row row = sheet.createRow(i ++);
			if (rowindex < table.getRowCount() && exprowindex < expectTable.getRowCount()) {
				int move = matchSameKeyMove(table, expectTable, rowindex, exprowindex, columns, expColumns, metaData, expMetaData, keys);
				int expmove = matchSameKeyMove(expectTable, table, exprowindex, rowindex, expColumns, columns, expMetaData, metaData, keys);
				if (move == 0) {
					boolean rowsame = true;
					boolean ignorecolumns = false;
					for (int k = 0; k < Math.max(columns.length, expColumns.length); k++) {
						Object value = null;
						Object expvalue = null;
						String strvalue = "";
						String strexpvalue = "";
						if (k < columns.length) {
							Column column = columns[k];
							value = table.getValue(rowindex, column.getColumnName());
							if (value != null)
								strvalue = column.getDataType().asString(value);
							if (isIgnoreColumns(column.getColumnName(), ignoreColumns))
								ignorecolumns = true;
						}
						if (k < expColumns.length) {
							Column expcolumn = expColumns[k];
							expvalue = expectTable.getValue(exprowindex, expcolumn.getColumnName());
							if (expvalue != null)
								strexpvalue = expcolumn.getDataType().asString(expvalue);
							if (isIgnoreColumns(expcolumn.getColumnName(), ignoreColumns))
								ignorecolumns = true;
						}
						boolean same = strvalue.equals(strexpvalue);
						if (ignorecolumns)
							same = true;
						if (!same) {
							issame = false;
							rowsame = false;
						}
						if (k < columns.length) {
							cell = row.createCell(k);
							if (value != null) {
								if ((value instanceof Date)) {
									setDateCell(cell, (Date)value, workbook, same);
								} else if ((value instanceof BigDecimal)) {
									setNumericCell(cell, (BigDecimal)value, workbook, same);
								} else if ((value instanceof Long)) {
									setDateCell(cell, new Date(((Long)value).longValue()), workbook, same);
								} else {
									setCellValue(cell, DataType.asString(value), workbook, same);
								}
							} else {
								setNullCell(cell);
							}
						}
						if (k < expColumns.length) {
							expcell = row.createCell(k + columns.length + 1);
							if (expvalue != null) {
								if ((expvalue instanceof Date)) {
									setDateCell(expcell, (Date) expvalue, workbook, same);
								} else if ((expvalue instanceof BigDecimal)) {
									setNumericCell(expcell, (BigDecimal)expvalue, workbook, same);
								} else if ((expvalue instanceof Long)) {
									setDateCell(expcell, new Date(((Long)expvalue).longValue()), workbook, same);
								} else {
									setCellValue(expcell, DataType.asString(expvalue), workbook, same);
								}
							} else {
								setNullCell(expcell);
							}
						}
					}
					if (diffOnly) {
						if (rowsame) {
							// delete current row
							i --;
							row = sheet.createRow(i);
						}
					}
					rowindex ++;
					exprowindex ++;
				} else if (move < expmove) {
					if (move == MATCH_NOT_FOUND)
						move = 1;
					issame = false;
					for (int j = 0; j < move; j ++) {
						if (j > 0)
							row = sheet.createRow(i ++);
						for (int k = 0; k < expColumns.length; k++) {
							Column expcolumn = expColumns[k];
							Object value = expectTable.getValue(exprowindex, expcolumn.getColumnName());
							expcell = row.createCell(k + columns.length + 1);
							if (value != null) {
								if ((value instanceof Date)) {
									setDiffDateCell(expcell, (Date) value, workbook);
								} else if ((value instanceof BigDecimal)) {
									setDiffNumericCell(expcell, (BigDecimal)value, workbook);
								} else if ((value instanceof Long)) {
									setDiffDateCell(expcell, new Date(((Long)value).longValue()), workbook);
								} else {
									setDiffCellValue(expcell, DataType.asString(value), workbook);
								}
							} else {
								setNullCell(expcell);
							}
						}
						exprowindex ++;
					}
				} else { // move >= expmove
					if (expmove >= MATCH_NOT_FOUND)
						expmove = 1;
					issame = false;
					for (int j = 0; j < expmove; j ++) {
						if (j > 0)
							row = sheet.createRow(i ++);
						
						for (int k = 0; k < columns.length; k++) {
							Column column = columns[k];
							Object value = table.getValue(rowindex, column.getColumnName());
							cell = row.createCell(k);
							if (value != null) {
								if ((value instanceof Date)) {
									setDiffDateCell(cell, (Date)value, workbook);
								} else if ((value instanceof BigDecimal)) {
									setDiffNumericCell(cell, (BigDecimal)value, workbook);
								} else if ((value instanceof Long)) {
									setDiffDateCell(cell, new Date(((Long)value).longValue()), workbook);
								} else {
									setDiffCellValue(cell, DataType.asString(value), workbook);
								}
							} else {
								setNullCell(cell);
							}
						}
						rowindex ++;
					}					
				}
				
				
			} else if (rowindex < table.getRowCount()) {
				issame = false;
				for (int k = 0; k < columns.length; k++) {
					Column column = columns[k];
					Object value = table.getValue(rowindex, column.getColumnName());
					if (value != null) {
						cell = row.createCell(k);
						if ((value instanceof Date)) {
							setDiffDateCell(cell, (Date)value, workbook);
						} else if ((value instanceof BigDecimal)) {
							setDiffNumericCell(cell, (BigDecimal)value, workbook);
						} else if ((value instanceof Long)) {
							setDiffDateCell(cell, new Date(((Long)value).longValue()), workbook);
						} else {
							setDiffCellValue(cell, DataType.asString(value), workbook);
						}
					}
				}
				rowindex ++;
			} else { // exprowindex < expectTable.getRowCount()
				issame = false;
				for (int k = 0; k < expColumns.length; k++) {
					Column expcolumn = expColumns[k];
					Object value = expectTable.getValue(exprowindex, expcolumn.getColumnName());
					if (value != null) {
						expcell = row.createCell(k + columns.length + 1);
						if ((value instanceof Date)) {
							setDiffDateCell(expcell, (Date) value, workbook);
						} else if ((value instanceof BigDecimal)) {
							setDiffNumericCell(expcell, (BigDecimal)value, workbook);
						} else if ((value instanceof Long)) {
							setDiffDateCell(expcell, new Date(((Long)value).longValue()), workbook);
						} else {
							setDiffCellValue(expcell, DataType.asString(value), workbook);
						}
					}
				}
				exprowindex ++;
			}		
		}
		
		for (int j = 0; j < columns.length; j++) {
			sheet.autoSizeColumn(j, true);
		}
		for (int j = 0; j < expColumns.length; j++) {
			sheet.autoSizeColumn(j + columns.length + 1, true);
		}
		
		Row titleRow = sheet.createRow(0);
		Cell titlecell = titleRow.createCell(0);
		setTitleCellValue(titlecell, diffOnly ? "After" : "Acutual:", workbook);
		Cell expTitlecell = titleRow.createCell(columns.length + 1);
		setTitleCellValue(expTitlecell, diffOnly ? "Before" : "Expected:", workbook);
		
		workbook.write(out);
		out.flush();
		
		return issame;
	}

	private boolean isIgnoreColumns(String columnName, String[] ignoreColumns) {
		for (String column : ignoreColumns) {
			if (columnName.equals(column))
				return true;
		}		
		return false;
	}

	private int matchSameKeyMove(ITable tableToMove, ITable table, int rowindexToMove, int rowindex, Column[] columnsToMove, Column[] columns, ITableMetaData metaDataToMove, ITableMetaData metaData, String[] keys) {
		int i = rowindex;
		
		for (; i < table.getRowCount(); i ++) {
			if (equalKeyValue(tableToMove, table, rowindexToMove, i, columnsToMove, columns, metaDataToMove, metaData, keys)) {
				return i - rowindex;
			}
		}
		
		return MATCH_NOT_FOUND;
	}

	@SuppressWarnings("static-access")
	private boolean equalKeyValue(ITable tableToMove, ITable table,	int rowindexToMove, int rowindex, Column[] columnsToMove, Column[] columns, ITableMetaData metaDataToMove, ITableMetaData metaData, String[] keys) {
		Object valueToMove = null;
		Object value = null;
		String strvalueToMove = "";
		String strvalue = "";
		for (int i = 0; i < keys.length; i ++) {
			try {
				int index = metaDataToMove.getColumnIndex(keys[i]);
				valueToMove = tableToMove.getValue(rowindexToMove, keys[i]);
				if (valueToMove != null)
					strvalueToMove = columnsToMove[index].getDataType().asString(valueToMove);
			} catch (DataSetException e) {
				strvalueToMove = "";
			}
			try {
				int index = metaData.getColumnIndex(keys[i]);
				value = table.getValue(rowindex, keys[i]);
				if (value != null)
					strvalue = columns[index].getDataType().asString(value);
			} catch (DataSetException e) {
				strvalue = "";
			}
			if (!strvalueToMove.equals(strvalue))
				return false;
		}
		
		return true;
	}

	protected static CellStyle createDateCellStyle(Workbook workbook) {
		DataFormat format = workbook.createDataFormat();
		short dateFormatCode = format.getFormat(DATE_FORMAT_AS_NUMBER_DBUNIT);
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setDataFormat(dateFormatCode);
		return cellStyle;
	}
	
	protected static CellStyle createNullCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}
	
	protected static CellStyle createDiffDateCellStyle(Workbook workbook) {
		DataFormat format = workbook.createDataFormat();
		short dateFormatCode = format.getFormat(DATE_FORMAT_AS_NUMBER_DBUNIT);
		CellStyle cellStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        cellStyle.setFont(font);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setDataFormat(dateFormatCode);
		return cellStyle;
	}

	protected void setNullCell(Cell cell) {
		cell.setCellStyle(nullCellStyle);
	}

	protected void setDateCell(Cell cell, Date value, Workbook workbook, boolean same) {
		long timeMillis = value.getTime();
		cell.setCellValue(timeMillis);
		cell.setCellType(0);
		if (same)
			cell.setCellStyle(dateCellStyle);
		else
			cell.setCellStyle(diffDateCellStyle);
	}

	protected void setDiffDateCell(Cell cell, Date value, Workbook workbook) {
		long timeMillis = value.getTime();
		cell.setCellValue(timeMillis);
		cell.setCellType(0);
		cell.setCellStyle(diffDateCellStyle);
	}
	
	protected void setNumericCell(Cell cell, BigDecimal value, Workbook workbook, boolean same) {
		cell.setCellValue(value.doubleValue());
		if (same)
			cell.setCellStyle(cellStyle);
		else
			cell.setCellStyle(diffCellStyle);
	}
	
	protected void setDiffNumericCell(Cell cell, BigDecimal value, Workbook workbook) {
		cell.setCellValue(value.doubleValue());
		cell.setCellStyle(diffCellStyle);
	}
	
	protected void setTitleCellValue(Cell cell, String value, Workbook workbook) {
		cell.setCellValue(value);
		cell.setCellStyle(titleCellStyle);
	}
	
	private static CellStyle createTitleCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		HSSFFont txtFont = (HSSFFont)workbook.createFont();
		txtFont.setFontName("Arial");
		txtFont.setFontHeightInPoints((short)18);
		txtFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		cellStyle.setFont(txtFont);
		return cellStyle;
	}
	
	protected void setHeaderCellValue(Cell cell, String value, Workbook workbook, boolean same) {
		cell.setCellValue(value);
		if (same)
			cell.setCellStyle(headerCellStyle);
		else
			cell.setCellStyle(headerDiffCellStyle);
	}
	
	private static CellStyle createHeaderCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setFillForegroundColor(HSSFColor.LIGHT_GREEN.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}
	
	private static CellStyle createHeaderDiffCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        cellStyle.setFont(font);
		cellStyle.setFillForegroundColor(HSSFColor.BRIGHT_GREEN.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}
	
	protected void setCellValue(Cell cell, String value, Workbook workbook, boolean same) {
		cell.setCellValue(value);
		if (same)
			cell.setCellStyle(cellStyle);
		else
			cell.setCellStyle(diffCellStyle);
	}
	
	protected void setDiffCellValue(Cell cell, String value, Workbook workbook) {
		cell.setCellValue(value);
		cell.setCellStyle(diffCellStyle);
	}
	
	private static CellStyle createCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}

	private static CellStyle createDiffCellStyle(Workbook workbook) {
		CellStyle cellStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        cellStyle.setFont(font);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}

	protected static CellStyle createIgnoredCellStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		Font font = workbook.createFont();
		font.setColor(IndexedColors.GREY_25_PERCENT.getIndex());
		cellStyle.setFont(font);
		return cellStyle;
	}

	private static CellStyle createCommonHeaderCellStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		cellStyle.setFillForegroundColor(HSSFColor.LIGHT_GREEN.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		return cellStyle;
	}

	private static CellStyle createUnmatchedColHeaderCellStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		cellStyle.setFillForegroundColor(HSSFColor.RED.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		return cellStyle;
	}

	private static CellStyle createPkColHeaderCellStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		cellStyle.setFillForegroundColor(HSSFColor.BRIGHT_GREEN.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		return cellStyle;
	}

	private static CellStyle createIgnoredColHeaderCellStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		return cellStyle;
	}

	protected static CellStyle createUnmatchedColStyle(Workbook workbook) {
		CellStyle cellStyle = createCellStyle(workbook);

		return cellStyle;
	}

	protected Workbook createWorkbook() {
		return new HSSFWorkbook();
	}

	public void write(ITable table, ITable expectTable, String[] keys, String[] ignoreColumns, boolean diffOnly)
			throws IOException, DataSetException {
		this.ignoreColumns = ignoreColumns;
		write(table, expectTable, keys, diffOnly);
	}

	public void write(ITable table, ITable expectTable, String[] keys, boolean diffOnly)
			throws IOException, DataSetException {

		List<String> actColNames = getTableColNameList(table);
		List<String> expColNames = getTableColNameList(expectTable);

		Sheet sht = resultBook.createSheet(table.getTableMetaData().getTableName());

		// title output
		Row titleRow = sht.createRow(0);
		Cell titlecell = titleRow.createCell(0);
		titlecell.setCellStyle(titleStyle);
		titlecell.setCellValue((diffOnly || ignoreColumns == null) ? "After" : "Acutual:");
		Cell expTitlecell = titleRow.createCell(actColNames.size() + 1);
		expTitlecell.setCellStyle(titleStyle);
		expTitlecell.setCellValue((diffOnly || ignoreColumns == null) ? "Before" : "Expected:");

		// header output
		Row headerRow = sht.createRow(1);
		List<String> pkColNames = getPkCols(actColNames, expColNames, keys);
		List<String> actUnmatchedColNames = getUnmathedCols(actColNames, expColNames);
		List<String> expUnmatchedColNames = getUnmathedCols(expColNames, actColNames);
		if (actUnmatchedColNames.size() > 0 || expUnmatchedColNames.size() > 0) {
			isSame = false;
		}
		int actHdColNo = 0;
		int expHdColNo = actHdColNo + actColNames.size() + 1;

		Map<String, CellStyle> colStyleMap = new HashMap<String, CellStyle>();
		for (String expColName : expColNames) {
			CellStyle headerStyle = commHeaderStyle;
			colStyleMap.put(expColName, commCellStyle);
			if (ignoreColumns != null && listContains(Arrays.asList(ignoreColumns), expColName)) {
				headerStyle = ignoredHeaderStyle;
				colStyleMap.put(expColName, ignoredCellStyle);
			} else if (listContains(pkColNames, expColName)) {
				headerStyle = pkHeaderStyle;
			} else if (listContains(expUnmatchedColNames, expColName)) {
				headerStyle = unmatchedHeaderStyle;
				colStyleMap.put(expColName, unmatchedColStyle);
			}

			if (!listContains(expUnmatchedColNames, expColName)) {
				Cell actCol = headerRow.createCell(actHdColNo);
				actCol.setCellStyle(headerStyle);
				actCol.setCellValue(expColName);
				actHdColNo++;
			}
			Cell expCol = headerRow.createCell(expHdColNo);

			expCol.setCellStyle(headerStyle);
			expCol.setCellValue(expColName);
			expHdColNo++;
		}
		for (String actUnmatchedCol : actUnmatchedColNames) {
			colStyleMap.put(actUnmatchedCol, unmatchedColStyle);

			Cell actCol = headerRow.createCell(actHdColNo);
			actCol.setCellStyle(unmatchedHeaderStyle);
			actCol.setCellValue(actUnmatchedCol);
			actHdColNo++;
		}

		// auto size
		for (int j = 0; j < expHdColNo; j++) {
			sht.autoSizeColumn(j, true);
		}

		// data compare part output
		List<Integer> actUnWroteRowNos = new ArrayList<Integer>();
		for (int row = 0; row < table.getRowCount(); row++) {
			actUnWroteRowNos.add(row);
		}
		int expRowNo = 0;

		List<Map<Integer, CellInfo>> writeRowList = new ArrayList<Map<Integer, CellInfo>>();

		for (; expRowNo < expectTable.getRowCount(); expRowNo++) {
			int actRowNo = getKeyMatchedActRow(table, expectTable, expRowNo, pkColNames, actUnWroteRowNos);

			int actColNo = 0;
			int expColNo = actColNo + actColNames.size() + 1;
			boolean lineIsSame = true;

			Map<Integer, CellInfo> writeRow = new HashMap<Integer, CellInfo>();

			for (String expColName : expColNames) {
				CellStyle cellStyle = colStyleMap.get(expColName);
				Object expVal = expectTable.getValue(expRowNo, expColName);
				Object actVal = "";
				if (actRowNo < 0) {
					lineIsSame = false;
				}
				if (!listContains(expUnmatchedColNames, expColName)) {
					if (actRowNo >= 0) {
						actVal = table.getValue(actRowNo, expColName);
					}
					if (!ignoredCellStyle.equals(cellStyle) && !valueFormat(actVal).equals(valueFormat(expVal))) {
						cellStyle = diffCellStyle;
						isSame = false;
						lineIsSame = false;
					}

					writeRow.put(actColNo, new CellInfo(actVal, cellStyle, expColName));
					actColNo++;
				}
				writeRow.put(expColNo, new CellInfo(expVal, cellStyle, expColName));
				expColNo++;
			}
			for (String actUnmatchedCol : actUnmatchedColNames) {
				lineIsSame = false;
				if (actRowNo >= 0) {
					writeRow.put(actColNo, new CellInfo(table.getValue(actRowNo, actUnmatchedCol), unmatchedColStyle, actUnmatchedCol));
				} else {
					writeRow.put(actColNo, new CellInfo("", unmatchedColStyle, actUnmatchedCol));
				}
				actColNo++;
			}
			if (!diffOnly || !lineIsSame) {
				writeRowList.add(writeRow);
			}
			actUnWroteRowNos.remove(new Integer(actRowNo));
		}
		for (int actRowNo : actUnWroteRowNos) {
			Map<Integer, CellInfo> writeRow = new HashMap<Integer, CellInfo>();
			isSame = false;

			int actColNo = 0;
			for (String expColName : expColNames) {
				if (!listContains(expUnmatchedColNames, expColName)) {
					writeRow.put(actColNo, new CellInfo(table.getValue(actRowNo, expColName), diffCellStyle, expColName));
					actColNo++;
				}
			}
			for (String actUnmatchedCol : actUnmatchedColNames) {
				writeRow.put(actColNo, new CellInfo(table.getValue(actRowNo, actUnmatchedCol), diffCellStyle, actUnmatchedCol));
				actColNo++;
			}
			writeRowList.add(writeRow);
		}

		int currentRowNo = 2;
		for (Map<Integer, CellInfo> rowMap : writeRowList) {
			if (currentRowNo > MAX_ROW_CNT) {
				break;
			}
			Row currentRow = sht.createRow(currentRowNo);
			for (int colNo = 0; colNo < expHdColNo; colNo++) {
				CellInfo info = rowMap.get(colNo);
				if (info != null) {
					Cell actCol = currentRow.createCell(colNo);
					actCol.setCellStyle(info.getStyle());
					String value = "";
					if (info.getValue() instanceof byte[] || info.getValue() instanceof Byte[]) {
						String filePrefix = colNo <= actHdColNo ? titlecell.getStringCellValue() : expTitlecell.getStringCellValue();
						String fileName = currentRowNo + "_" + filePrefix + "_" + info.getColName();
						value = "「" + sht.getSheetName() + "/" + fileName + "」をご参照";
						createFile((byte[]) info.getValue(), fileName, sht.getSheetName());
					} else if (valueFormat(info.getValue()).length() > MAX_CELL_SIZE) {
						String filePrefix = colNo <= actHdColNo ? titlecell.getStringCellValue() : expTitlecell.getStringCellValue();
						String fileName = currentRowNo + "_" + filePrefix + "_" + info.getColName();
						value = "「" + sht.getSheetName() + "/" + fileName + "」をご参照";
						createFile(valueFormat(info.getValue()).getBytes(), fileName, sht.getSheetName());
					} else {
						value = valueFormat(info.getValue());
					}

					actCol.setCellValue(value);
				}
			}
			currentRowNo++;
		}

	}

	private void createFile(byte[] fileContent, String fileName, String sheetName) {
		File path = new File(ExProgressFormatter.getEvidenceDirName().resolve(sheetName).toString());
		if (!path.exists()) {
			path.mkdir();
		}
		File resultFile = new File(ExProgressFormatter.getEvidenceDirName().resolve(sheetName).toString() + "/" + fileName);
		OutputStream ous = null;
		
		try {
			ous = new FileOutputStream(resultFile);
			ous.write(fileContent);
		} catch (IOException ioe) {
			// ignore
		} finally {
			try {
				if (ous != null)
					ous.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private static List<String> getTableColNameList(ITable table) throws DataSetException {
		List<String> colNames = new ArrayList<String>();

		for (Column col : table.getTableMetaData().getColumns()) {
			colNames.add(col.getColumnName());
		}
		return colNames;
	}

	private List<String> getPkCols(List<String> actCols, List<String> expCols, String[] keys) {
		List<String> pkCols = new ArrayList<String>();
		if (keys != null) {
			for (String key : keys) {
				if (listContains(actCols, key) && listContains(expCols, key)) {
					pkCols.add(key);
				}
			}
		}
		if (ignoreColumns != null) {
			for (String ignoreCol : ignoreColumns) {
				pkCols.remove(ignoreCol);
			}
		}
		return pkCols;
	}

	private static List<String> getUnmathedCols(List<String> srcList, List<String> tarList) {
		List<String> unmatchedList = new ArrayList<String>();
		for (String src : srcList) {
			if (!listContains(tarList, src)) {
				unmatchedList.add(src);
			}
		}
		return unmatchedList;
	}

	private static int getKeyMatchedActRow(ITable table, ITable expectTable, int expRowNo, List<String> pkList,
			List<Integer> actMatchedRowNos) throws DataSetException {
		int tableRowNo = -1;
		for (int rowIdx : actMatchedRowNos) {
			if (compareTablesPk(table, rowIdx, expectTable, expRowNo, pkList) == 0) {
				tableRowNo = rowIdx;
				break;
			}
		}
		return tableRowNo;
	}

    private static int compareTablesPk(ITable tarTable, int tarRowNo, ITable srcTable, int srcRowNo,
            List<String> pkList) throws DataSetException {
        int compRes = 0;
        for (String pk : pkList) {
            if (compRes != 0) {
                break;
            }
            Object tarVal = tarTable.getValue(tarRowNo, pk);
            Object srcVal = srcTable.getValue(srcRowNo, pk);
            if (tarVal == null && srcVal != null) {
                compRes = -1;
            } else if (tarVal != null && srcVal == null) {
                compRes = 1;
            } else if (tarVal != null && srcVal != null) {
                compRes = DataType.asString(tarVal).compareTo(DataType.asString(srcVal));
            }
        }
        return compRes;
    }

	private static String valueFormat(Object value) throws TypeCastException {
		if (value == null) {
			return "";
		} else if ((value instanceof Date)) {
			return formatDateToStr((Date) value, UPDATE_DATE_FORMAT);
		} else {
			return DataType.asString(value);
		}
	}

	private static boolean listContains(List<?> srcList, Object target) {
		boolean result = false;
		for (Object src : srcList) {
			if (target == null && target == src) {
				result = true;
				break;
			} else if (target != null && target.equals(src)) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * format date to string
	 *
	 * @param date
	 *            the date
	 * @param format
	 *            the format
	 * @return the formatted string
	 */
	public static String formatDateToStr(Date date, String format) {
		if (date == null || format == null) {
			return null;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format, new DateFormatSymbols());
		return sdf.format(date);
	}

	private static class CellInfo {
		private Object value_;
		private CellStyle style_;
		private String colName_;

		public CellInfo(Object value, CellStyle style, String colName) {
			value_ = value;
			style_ = style;
			colName_ = colName;
		}

		public Object getValue() {
			return value_;
		}

		public CellStyle getStyle() {
			return style_;
		}

		public String getColName() {
			return colName_;
		}
	}

	public boolean isSame() {
		return isSame;
	}

	public Workbook getResultBook() {
		return resultBook;
	}
}
