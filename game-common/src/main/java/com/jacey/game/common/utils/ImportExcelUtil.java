package com.jacey.game.common.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: excel导入工具类
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ImportExcelUtil {


    private final static String excel2003L = ".xls"; // 2003- 版本的excel
    private final static String excel2007U = ".xlsx"; // 2007+ 版本的excel


    /**
     *
     * @param in        输入流
     * @param fileName 文件名（判断Excel版本）
     * @param mapping  字段名称映射
     * @param scope     取前几列数据（1表示取一列数据）
     * @return
     * @throws Exception
     */
    public static Map<String, Object> parseExcel(InputStream in, String fileName,
                                                       Map<String, String> mapping, int scope) throws Exception {
        // 根据文件名来创建Excel工作薄
        Workbook work = getWorkbook(in, fileName);
        if (null == work) {
            throw new Exception("创建Excel工作薄为空！");
        }
        Sheet sheet = null;
        Row row = null;
        Cell cell = null;

        Map<String, Object> resultMap = new HashMap<>();

        // 遍历Excel中所有的sheet(遍历所有页)
        for (int i = 0; i < work.getNumberOfSheets(); i++) {
            sheet = work.getSheetAt(i);
            if (sheet == null) {
                continue;
            }

            // 取第一行标题
            row = sheet.getRow(0);
            String title[] = null;
            if (row != null) {
                title = new String[row.getLastCellNum()];

                for (int y = row.getFirstCellNum(); y < row.getLastCellNum(); y++) {
                    cell = row.getCell(y);
                    title[y] = (String) getCellValue(cell);
                }

            } else {
                continue;
            }

            List<Map<String, Object>> tmpList = new ArrayList<Map<String, Object>>();
            // 遍历当前sheet中的所有行
            for (int j = 1; j < sheet.getLastRowNum() + 1; j++) {
                row = sheet.getRow(j);
                Map<String, Object> m = new HashMap<String, Object>();
                // 遍历所有的列 (只去前两列数据)
                for (int y = row.getFirstCellNum(); y < 2; y++) {
                    cell = row.getCell(y);
                    String key = title[y];
                    m.put(mapping.get(key), getCellValue(cell));
                }
                tmpList.add(m);
            }
            for (Map<String, Object> map : tmpList) {
                resultMap.put((String) map.get("key"), map.get("value"));
            }
        }
        work.close();
        return resultMap;
    }


    /**
     * 描述：根据文件后缀，自适应上传文件的版本
     * @param inStr
     * @param fileName
     * @return
     * @throws Exception
     */
    public static Workbook getWorkbook(InputStream inStr, String fileName) throws Exception {
        Workbook wb = null;
        String fileType = fileName.substring(fileName.lastIndexOf("."));
        if (excel2003L.equals(fileType)) {
            wb = new HSSFWorkbook(inStr); // 2003-
        } else if (excel2007U.equals(fileType)) {
            wb = new XSSFWorkbook(inStr); // 2007+
        } else {
            throw new Exception("解析的文件格式有误！");
        }
        return wb;
    }

    /**
     * 描述：对表格中数值进行格式化
     *
     * @param cell
     * @return
     */
    public static Object getCellValue(Cell cell) {
        Object value = null;
        DecimalFormat df = new DecimalFormat("0"); // 格式化number String字符
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd"); // 日期格式化
        DecimalFormat df2 = new DecimalFormat("0"); // 格式化数字

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                value = cell.getRichStringCellValue().getString();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if ("General".equals(cell.getCellStyle().getDataFormatString())) {
                    value = df.format(cell.getNumericCellValue());
                } else if ("m/d/yy".equals(cell.getCellStyle().getDataFormatString())) {
                    value = sdf.format(cell.getDateCellValue());
                } else {
                    value = df2.format(cell.getNumericCellValue());
                }
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case Cell.CELL_TYPE_BLANK:
                value = "";
                break;
            default:
                break;
        }
        return value;
    }

    public static Map<String, String> toMap(List<Map<String, Object>> list) {
        Map<String, String> result = new HashMap<>();

        return result;
    }

    public static void main(String[] args) throws Exception {
//        File file = new File("D:\\wordspace\\Study\\excel-read-test\\src\\main\\resources\\SystemConfig.xlsx");
        File file = new File("D:\\wordspace\\Study\\excel-read-test\\src\\main\\resources\\test2.xlsx");
        FileInputStream fis = new FileInputStream(file);
        Map<String, String> m = new HashMap<String, String>();
        m.put("参数名", "key");
        m.put("参数值", "value");
        Map<String, Object> map = parseExcel(fis, file.getName(), m, 2);
    }
}