package org.example.niiefa_energo;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController implements Initializable, Notification {

    @FXML
    private GridPane background;

    @FXML
    private ToggleButton acsEnableButton;

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

    @FXML
    private Button comPortConnectButton;

    private String serialPortName;

    private SerialPort serialPort;

    private InputStream inputStream;

    private OutputStream outputStream;

    private Thread serialThreadOutput;

    private Thread serialThreadInput;

    float alpha = 0.0f;
    float freq = 0.0f;
    float current = 0.0f;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        serialThreadOutput = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (outputStream != null) {
                            // 2 start bytes, alpha_filter_float, frequency_float, current_float, mode_byte
                            ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 4 + 4 + 1);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            buf.put((byte) 'a').put((byte) 'f').putFloat(alpha).putFloat(freq).putFloat(current).put((byte) 1);
                            outputStream.write(buf.array());
                        }
                    } catch (SerialPortTimeoutException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        connectionStatus.setText("Ошибка COM-порта");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        if(serialPort != null)
                            serialPort.closePort();
                        return;
                    }
                }
            }
        });
        serialThreadOutput.start();

        serialThreadInput = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if(inputStream != null) {
                            if(inputStream.available() > )
                        }
                    } catch (SerialPortTimeoutException e) {

                    } catch (IOException e) {

                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        if (serialPort != null)
                            serialPort.closePort();
                        return;
                    }
                }
            }
        });
        serialThreadInput.start();

        comPortChoice.showingProperty().addListener((observable, wasShowing, isShowing) ->
        {
            if (isShowing) {
                comPortChoice.getItems().clear();
                SerialPort[] ports = SerialPort.getCommPorts();
                for (SerialPort port : ports
                ) {
                    comPortChoice.getItems().add(port.getSystemPortName());
                }
            }
            if (wasShowing) {
                if (comPortChoice.getValue() == null) {
                    comPortChoice.setValue(serialPortName);
                } else {
                    serialPortName = comPortChoice.getValue();
                    if (serialPort != null) {
                        if (serialPort.isOpen() && !serialPort.getSystemPortName().equals(serialPortName)) {
                            serialPort.closePort();
                            connectionStatus.setText("Не подключено");
                            outputStream = null;
                            inputStream = null;
                        }
                    }
                }
            }
        });

        comPortConnectButton.pressedProperty().addListener((observable, released, pressed) -> {
            if (released) {
                if (serialPortName == null)
                    return;
                if (serialPort != null)
                    serialPort.closePort();
                serialPort = SerialPort.getCommPort(serialPortName);
                serialPort.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY);
                serialPort.openPort();
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                connectionStatus.setText("Подключено");
            }
        });
        background.setOnMousePressed(event -> {
            if (!alphaFilterField.equals(event.getSource()))
                alphaFilterField.getParent().requestFocus();
        });
        alphaFilterField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            alphaFilterField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    alpha = Float.parseFloat(alphaFilterField.getText().strip().replaceAll(",", "."));
                } catch (NumberFormatException e) {
                    alphaFilterField.getStyleClass().add("invalid");
                }
            }
        });
        frequencySetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            frequencySetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    freq = Float.parseFloat(frequencySetField.getText().strip().replaceAll(",", "."));
                } catch (NumberFormatException e) {
                    frequencySetField.getStyleClass().add("invalid");
                }
            }
        });
        currentSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            currentSetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    current = Float.parseFloat(currentSetField.getText().strip().replaceAll(",", "."));
                } catch (NumberFormatException e) {
                    currentSetField.getStyleClass().add("invalid");
                }
            }
        });
    }

    @FXML
    void onPauseButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Пауза");
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Запуск");
        }
    }

    @FXML
    void onAcsEnableButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Включить САУ");
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Выключить САУ");
        }
    }

    @FXML
    void onStartButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Старт");
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Стоп");
        }
    }

    @Override
    public void closeApp() {
        serialThreadOutput.interrupt();
    }
}
