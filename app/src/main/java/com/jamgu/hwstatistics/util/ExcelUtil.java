package com.jamgu.hwstatistics.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.jamgu.common.thread.ThreadPool;
import com.jamgu.common.widget.toast.JToast;
import com.jamgu.hwstatistics.net.upload.DataSaver;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *     author: Bruce Yang
 *     blog  : https://yangjianan.gitee.io
 *     time  : 2019/11/7
 *     desc  : Excel Read Write
 *     thanks  : https://blog.csdn.net/u014743890/article/details/91045033
 *     https://github.com/andruhon/android5xlsx
 * </pre>
 */
public class ExcelUtil {
    private static final String TAG = ExcelUtil.class.getSimpleName();

    public static List<Map<Integer, Object>> readExcelNewLineByLine(Context context, Uri uri, String filePath) {
        List<Map<Integer, Object>> list = null;
        Workbook wb;
        if (filePath == null) {
            return null;
        }
        String extString;
        if (!filePath.endsWith(".xls") && !filePath.endsWith(".xlsx")) {
            Log.e(TAG, "Please select the correct Excel file");
            return null;
        }
        extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is;
        try {
            is = context.getContentResolver().openInputStream(uri);
//            Log.i(TAG, "readExcel: " + extString);
            if (".xls".equals(extString)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                wb = new XSSFWorkbook(is);
            } else {
                wb = null;
            }
            if (wb != null) {
                // used to store data
                list = new ArrayList<>();
                // get the first sheet
                Sheet sheet = wb.getSheetAt(0);

                // get the maximum number of rows
                int rownum = sheet.getPhysicalNumberOfRows();
                //index starts from 1,exclude header.
                //if you want read line by line, index should from 0.
                for (int i = 0; i < rownum; i++) {
                    Row row = sheet.getRow(i);
                    //storing content
                    Map<Integer, Object> itemMap = new HashMap<>();
                    final int colnum = row.getPhysicalNumberOfCells();
                    if (row != null) {
                        for (int j = 0; j < colnum; j++) {
                            Object value = getCellFormatValue(row.getCell(j));
                            String cellInfo = "r: " + i + "; c:" + j + "; v:" + value;
                            itemMap.put(j, value);
                        }
                    } else {
                        break;
                    }
                    list.add(itemMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "readExcelNew: import error " + e);
            ThreadPool.runUITask(() -> {
                JToast.INSTANCE.showToast(context, "import error " + e);
            });
        }
        return list;
    }

    public static List<Map<Integer, Object>> readExcelNewByHeader(Context context, Uri uri, String filePath) {
        List<Map<Integer, Object>> list = null;
        Workbook wb;
        if (filePath == null) {
            return null;
        }
        String extString;
        if (!filePath.endsWith(".xls") && !filePath.endsWith(".xlsx")) {
            Log.e(TAG, "Please select the correct Excel file");
            return null;
        }
        extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is;
        try {
            is = context.getContentResolver().openInputStream(uri);
            Log.i(TAG, "readExcel: " + extString);
            if (".xls".equals(extString)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                wb = new XSSFWorkbook(is);
            } else {
                wb = null;
            }
            if (wb != null) {
                // used to store data
                list = new ArrayList<>();
                // get the first sheet
                Sheet sheet = wb.getSheetAt(0);
                // get the first line header
                Row rowHeader = sheet.getRow(0);
                int cellsCount = rowHeader.getPhysicalNumberOfCells();
                //store header to the map
                Map<Integer, Object> headerMap = new HashMap<>();
                for (int c = 0; c < cellsCount; c++) {
                    Object value = getCellFormatValue(rowHeader.getCell(c));
                    String cellInfo = "header " + "; c:" + c + "; v:" + value;
//                    Log.i(TAG, "readExcelNew: " + cellInfo);

                    headerMap.put(c, value);
                }
                //add  headermap to list
                list.add(headerMap);

                // get the maximum number of rows
                int rownum = sheet.getPhysicalNumberOfRows();
                // get the maximum number of columns
                int colnum = headerMap.size();
                //index starts from 1,exclude header.
                //if you want read line by line, index should from 0.
                for (int i = 1; i < rownum; i++) {
                    Row row = sheet.getRow(i);
                    //storing subcontent
                    Map<Integer, Object> itemMap = new HashMap<>();
                    if (row != null) {
                        for (int j = 0; j < colnum; j++) {
                            Object value = getCellFormatValue(row.getCell(j));
                            String cellInfo = "r: " + i + "; c:" + j + "; v:" + value;
                            Log.i(TAG, "readExcelNew: " + cellInfo);
                            itemMap.put(j, value);
                        }
                    } else {
                        break;
                    }
                    list.add(itemMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "readExcelNew: import error " + e);
            ThreadPool.runUITask(() -> {
                JToast.INSTANCE.showToast(context, "import error " + e);
            });
        }
        return list;
    }

    public static void writeWithRetry(Context context, ArrayList<ArrayList<Object>> exportExcel, Uri uri) {
        boolean success = writeExcelNew(context, exportExcel, uri);
        if (!success) {
            try {
                boolean retryResult = writeExcelNew(context, exportExcel, uri);
                DataSaver.INSTANCE.addInfoTracker(TAG, "uri = " + uri.toString()
                        + " old file retry result = " + retryResult);
            } catch (Exception e) {
                DataSaver.INSTANCE.addInfoTracker(TAG, "writeWithRetry:: err happened "
                        + e.getMessage() + " stackTrace + " + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public static boolean writeExcelNew(Context context, ArrayList<ArrayList<Object>> exportExcel, Uri uri) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"));

            int colums = exportExcel.get(0).size();
            for (int i = 0; i < colums; i++) {
                //set the cell default width to 15 characters
                sheet.setColumnWidth(i, 15 * 256);
            }

            for (int i = 0; i < exportExcel.size(); i++) {
                Row row = sheet.createRow(i);
                ArrayList<Object> realData = exportExcel.get(i);
                for (int j = 0; j < realData.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object val = realData.get(j);
                    if (val instanceof Float) {
                        float fVal = Float.parseFloat(val.toString());
                        cell.setCellValue(fVal);
                    } else {
                        cell.setCellValue(String.valueOf(realData.get(j)));
                    }
                }
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
            Log.i(TAG, "writeExcel: export successful");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "writeExcel: error" + e);
            DataSaver.INSTANCE.addInfoTracker(TAG, uri.toString() +
                    ", err happened when store file, msg = " + e.getMessage() + ", e = " + Arrays.toString(e.getStackTrace()));
            return false;
        }
    }

    /**
     * get single cell data
     *
     * @param cell </>
     * @return cell
     */
    private static Object getCellFormatValue(Cell cell) {
        Object cellValue;
        if (cell != null) {
            // 判断cell类型
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    cellValue = cell.getBooleanCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC: {
                    cellValue = String.valueOf(cell.getNumericCellValue());
                    break;
                }
                case Cell.CELL_TYPE_FORMULA: {
                    // determine if the cell is in date format
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Convert to date format YYYY-mm-dd
                        cellValue = cell.getDateCellValue();
                    } else {
                        // Numeric
                        cellValue = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case Cell.CELL_TYPE_STRING: {
                    cellValue = cell.getRichStringCellValue().getString();
                    break;
                }
                default:
                    cellValue = "";
            }
        } else {
            cellValue = "";
        }
        return cellValue;
    }
}
