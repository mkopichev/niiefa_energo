package org.example.niiefa_energo;

import com.fazecast.jSerialComm.SerialPort;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private TextField alphaFilterField;

    @FXML
    private ComboBox<String> comPortChoice;

    @FXML
    private Text connectionStatus;

    @FXML
    private TextField currentSetField;

    @FXML
    private TextField duration;

    @FXML
    private TextField frequencyField;

    @FXML
    private TextField frequencySetField;

    @FXML
    private LineChart<?, ?> lineChartArea;

    @FXML
    private CheckBox plot1checkBox;

    @FXML
    private CheckBox plot2checkBox;

    @FXML
    private CheckBox plot3checkBox;

    @FXML
    private ToggleButton startButton;

    @FXML
    private TextField yMaxValueField;

    @FXML
    private TextField yMinValueField;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        comPortChoice.showingProperty().addListener((observable, wasShowing, isShowing) ->
        {
            if(isShowing) {
                comPortChoice.getItems().clear();
                SerialPort[] ports = SerialPort.getCommPorts();
                for (SerialPort port : ports
                ) {
                    comPortChoice.getItems().add(port.getDescriptivePortName());
                }
            }
        });
    }
}


