package model.enums;

/**
 * سوالات امنیتی از پیش تعریف‌شده برای بازیابی رمز عبور.
 * هنگام ثبت‌نام، بازیکن یکی را انتخاب می‌کند.
 */
public enum SecurityQuestion {
    Q1("What is the name of your first pet?"),
    Q2("What is your mother's maiden name?"),
    Q3("What was the name of your first school?"),
    Q4("What is your favorite book?"),
    Q5("What city were you born in?");

    private final String questionText;

    SecurityQuestion(String questionText) {
        this.questionText = questionText;
    }

    /** متن سوال را برمی‌گرداند */
    public String getQuestionText() {
        return questionText;
    }
}
