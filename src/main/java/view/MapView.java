package view;

import model.GameSession;

/**
 * رندر نقشه بازی در ترمینال.
 * هر خانه با نماد مخصوص نوع زمین، گیاه و زامبی نمایش داده می‌شود.
 */
public class MapView {

    /**
     * نقشه کامل بازی را در ترمینال چاپ می‌کند.
     * شامل: شماره موج، خورشید، غذای گیاه، ماشین چمن‌زنی و محتوای هر خانه.
     * @param session session جاری
     */
    public void renderMap(GameSession session) { }

    /**
     * هدر نقشه را چاپ می‌کند (موج، خورشید، plant food).
     * @param session session جاری
     */
    private void renderHeader(GameSession session) { }

    /**
     * یک خانه را به صورت رشته نماد برمی‌گرداند.
     * خانه‌های مختلف (آب، قبر، یخ، عادی) نماد متفاوت دارند.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     * @return نماد ترمینالی خانه
     */
    private String renderTile(GameSession session, int x, int y) { return null; }

    /**
     * وضعیت ماشین چمن‌زنی‌ها را در کنار هر ردیف نمایش می‌دهد.
     * @param session session جاری
     */
    private void renderLawnMowers(GameSession session) { }
}
