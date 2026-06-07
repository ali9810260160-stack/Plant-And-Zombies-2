package controller;

import model.AppState;
import service.GreenhouseService;
import service.ShopService;
import view.ConsoleView;

/**
 * کنترلر منوی گلخانه و فروشگاه.
 * دستورات کاشت، برداشت، تسریع و خرید از فروشگاه.
 */
public class GreenhouseController {

    private final GreenhouseService greenhouseService;
    private final ShopService shopService;
    private final AppState appState;
    private final ConsoleView view;

    public GreenhouseController(GreenhouseService greenhouseService,
                                ShopService shopService,
                                AppState appState, ConsoleView view) {
        this.greenhouseService = greenhouseService;
        this.shopService = shopService;
        this.appState = appState;
        this.view = view;
    }

    /** دستور "show greenhouse" */
    public void showGreenhouse() { }

    /**
     * دستور "plant pot at (x,y)"
     * @param x ستون
     * @param y ردیف
     */
    public void plantPot(int x, int y) { }

    /**
     * دستور "collect (x,y)"
     * @param x ستون
     * @param y ردیف
     */
    public void collectPot(int x, int y) { }

    /**
     * دستور "grow (x,y)"
     * @param x ستون
     * @param y ردیف
     */
    public void accelerateGrowth(int x, int y) { }

    /** دستور "enter shop" - ورود به فروشگاه */
    public void enterShop() { }

    /** دستور "shop list" */
    public void showShopList() { }

    /** دستور "shop daily" */
    public void showDailyOffer() { }

    /**
     * دستور "shop buy -i <id> -n <count> [-t <plant>]"
     * @param itemId شناسه آیتم
     * @param count تعداد
     * @param plantType نوع گیاه (برای بسته بذر انتخابی)
     */
    public void buyShopItem(String itemId, int count, String plantType) { }
}
