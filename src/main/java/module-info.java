module org.example.niiefa_energo {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;


    opens org.example.niiefa_energo to javafx.fxml;
    exports org.example.niiefa_energo;
}