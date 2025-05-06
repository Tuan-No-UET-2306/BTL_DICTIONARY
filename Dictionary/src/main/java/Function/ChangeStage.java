package Function;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ChangeStage {
    public static void changeStage(Button a, String nameOfStage,Class<?> clazz) {
        try {
            Parent nextStage = FXMLLoader.load(Objects.requireNonNull
                    (clazz.getResource(nameOfStage)));
            Scene nextScene = new Scene(nextStage);

            // Tạo Stage mới
            Stage newStage = new Stage();
            newStage.setScene(nextScene);
            newStage.setTitle("BaChuTeEnglish");
            Image icon = new Image(Objects.requireNonNull(ChangeStage.class.getResource("/picture/LoGo/icon.png")).toExternalForm());
            newStage.getIcons().add(icon);
            newStage.show();

            // Đóng Stage cũ
            Stage currentStage = (Stage) a.getScene().getWindow();
            currentStage.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
