package com.hpe.autoframework;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;

public class ExXlsDataSetWriter {
	public static final String ZEROS = "0000000000000000000000000000000000000000000000000000";
	public static final String DATE_FORMAT_AS_NUMBER_DBUNIT = "####################";

	private CellStyle dateCellStyle;

	public void write(IDataSet dataSet, OutputStream out) throws IOException, DataSetException {

		Workbook workbook = createWorkbook();

		dateCellStyle = createDateCellStyle(workbook);

		int index = 0;
		ITableIterator iterator = dataSet.iterator();
		while (iterator.next()) {
			ITable table = iterator.getTable();
			ITableMetaData metaData = table.getTableMetaData();
			Sheet sheet = workbook.createSheet(metaData.getTableName());

			workbook.setSheetName(index, metaData.getTableName());

			Row headerRow = sheet.createRow(0);
			Column[] columns = metaData.getColumns();
			for (int j = 0; j < columns.length; j++) {
				Column column = columns[j];
				Cell cell = headerRow.createCell(j);
				setHeaderCellValue(cell, column.getColumnName(), workbook);
			}

			for (int j = 0; j < table.getRowCount(); j++) {
				Row row = sheet.createRow(j + 1);
				for (int k = 0; k < columns.length; k++) {
					Column column = columns[k];
					Object value = table.getValue(j, column.getColumnName());
					if (value != null) {
						Cell cell = row.createCell(k);
						if ((value instanceof Date)) {
							setDateCell(cell, (Date) value, workbook);
						} else if ((value instanceof BigDecimal)) {
							setNumericCell(cell, (BigDecimal) value, workbook);
						} else if ((value instanceof Long)) {
							setDateCell(cell, new Date(((Long) value).longValue()),	workbook);
						} else {
							setCellValue(cell, DataType.asString(value), workbook);
						}
					}
				}
			}

			for (int j = 0; j < columns.length; j++) {
				sheet.autoSizeColumn(j, true);
			}
			
			index++;
		}

		workbook.write(out);
		out.flush();
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

	protected void setDateCell(Cell cell, Date value, Workbook workbook) {
		long timeMillis = value.getTime();
		cell.setCellValue(timeMillis);
		cell.setCellType(0);
		cell.setCellStyle(dateCellStyle);
	}

	protected void setNumericCell(Cell cell, BigDecimal value, Workbook workbook) {

		cell.setCellValue(value.doubleValue());

		DataFormat df = workbook.createDataFormat();
		int scale = value.scale();
		short format;
		if (scale <= 0) {
			format = df.getFormat("####");
		} else {
			String zeros = createZeros(value.scale());
			format = df.getFormat("####." + zeros);
		}
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setDataFormat(format);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		cell.setCellStyle(cellStyle);
	}
	
	protected void setHeaderCellValue(Cell cell, String value, Workbook workbook) {
		cell.setCellValue(value);
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
		cell.setCellStyle(cellStyle);
	}
	
	protected void setCellValue(Cell cell, String value, Workbook workbook) {
		cell.setCellValue(value);
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		cell.setCellStyle(cellStyle);
	}

	private static String createZeros(int count) {
		return ZEROS.substring(0, count);
	}

	protected Workbook createWorkbook() {
		return new HSSFWorkbook();
	}
}
