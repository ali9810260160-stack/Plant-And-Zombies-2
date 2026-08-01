import view.CommandDispatcher;
import util.FileUtil;

/**
 * نقطه ورود اصلی برنامه.
 * برنامه‌نویسی پیشرفته - دانشگاه صنعتی شریف
 * پروژه: Plants vs. Zombies 2
 */
public class Main {

    public static void main(String[] args) {
        FileUtil.initDataDirectories();
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.run();
    }
}
