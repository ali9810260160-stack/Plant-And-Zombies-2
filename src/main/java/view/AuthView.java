package view;

/**
 * کلاس View مخصوص منوی احراز هویت (Register / Login / Password Recovery).
 */
public class AuthView {

    public void printSecurityQuestions() {
        System.out.println(ConsoleView.BOLD + ConsoleView.CYAN
                + "\n🔒 Security Questions:" + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  1. What is the name of your first pet?" + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  2. What is your mother's maiden name?" + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  3. What was the name of your first school?" + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  4. What is your favorite book?" + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  5. What city were you born in?" + ConsoleView.RESET);
    }
}
