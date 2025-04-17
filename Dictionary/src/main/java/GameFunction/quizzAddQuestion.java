package GameFunction;

import java.util.ArrayList;

public class quizzAddQuestion {
    public static ArrayList<quizz> questions = new ArrayList<>();
    static ArrayList<String> option = new ArrayList<>();
    public static void main(String [] args) {
        option.add("A. Biên dịch");

        questions.add(new quizz("Java là ngôn ngữ lập trình gì?\n", option, "Anh"  ));
        System.out.println(questions.get(0).getQuestion());
        ArrayList<String> a = questions.get(0).getOptions();
        System.out.println(a.get(0));
        System.out.println(questions.get(0).getCorrectAns());



    }

}
