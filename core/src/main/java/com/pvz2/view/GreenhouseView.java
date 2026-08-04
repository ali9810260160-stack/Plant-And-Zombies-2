package com.pvz2.view;

import java.util.List;

/**
 * کلاس View مخصوص منوی گلخانه و فروشگاه (GREENHOUSE, SHOP).
 */
public class GreenhouseView {

    /**
     * چاپ یک ردیف از جدول گلخانه.
     *
     * @param rowNumber  شماره ردیف (1 تا 4)
     * @param cellDisplays لیست رشته‌های فرمت‌شده هر گلدان در آن ردیف
     */
    public void printGreenhouseRow(int rowNumber, List<String> cellDisplays) {
        StringBuilder sb = new StringBuilder();
        sb.append(ConsoleView.CYAN)
          .append(" ").append(rowNumber).append(" ")
          .append(ConsoleView.RESET);
        for (String cell : cellDisplays) {
            sb.append(cell).append(" ");
        }
        System.out.println(sb.toString());
    }
}
