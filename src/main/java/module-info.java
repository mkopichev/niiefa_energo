module org.example.niiefa_energo {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.niiefa_energo to javafx.fxml;
    exports org.example.niiefa_energo;
}