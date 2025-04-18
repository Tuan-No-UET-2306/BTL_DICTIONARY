module com.example.dictionary {
    requires javafx.controls;
    requires javafx.fxml;
    // Bạn có chắc cần module này không? Thường không cần cho JavaFX thuần túy.
    requires org.json;
    requires java.net.http;


    // Thêm dòng này để sử dụng các lớp trong javafx.scene.media


    opens com.example.dictionary to javafx.fxml;
    exports com.example.dictionary;
}