package org.example.niiefa_energo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class MainController {

    @FXML
    private LineChart<?, ?> lineChartArea;

    @FXML
    private CheckBox plot1checkBox;

    @FXML
    private CheckBox plot2checkBox;

    @FXML
    private CheckBox plot3checkBox;

    @FXML
    private Button resetMinMaxValues;

    @FXML
    private TextField xMaxValueField;

    @FXML
    private TextField xMinValueField;

    @FXML
    private TextField yMaxValueField;

    @FXML
    private TextField yMinValueField;

    @FXML
    void checkBoxEvent(ActionEvent event) {

    }

    @FXML
    void onChangeRange(ActionEvent event) {

    }

    @FXML
    void onResetRange(ActionEvent event) {

    }

}
