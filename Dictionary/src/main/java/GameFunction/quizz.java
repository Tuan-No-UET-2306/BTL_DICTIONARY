package GameFunction;

import java.util.ArrayList;

public class quizz {
    private String question;
    private ArrayList<String> options;
    private String correctAns;
    quizz(String question,ArrayList<String> options, String correctAns) {
        this.question = question;
        this.options = options;
        this.correctAns = correctAns;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public ArrayList<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options) {
        this.options = options;
    }

    public String getCorrectAns() {
        return correctAns;
    }

    public void setCorrectAns(String correctAns) {
        this.correctAns = correctAns;
    }
}
