package org.example.niiefa_energo;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class MainController implements Initializable, Notification {

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
    private ToggleButton startButton11;

    @FXML
    private TextField yMaxValueField;

    @FXML
    private TextField yMinValueField;

    @FXML
    private Button  comPortConnectButton;

    private String serialPortName;

    private SerialPort serialPort;

    private InputStream inputStream;

    private OutputStream outputStream;

    private Thread serialThreadOutput;

    float alpha = 0.0f;
    float freq = 0.0f;
    float current = 0.0f;

    ExecutorService es = Executors.newSingleThreadExecutor();

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
                    }
                    catch (IOException e) {
                        connectionStatus.setText("Ошибка COM-порта");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        serialPort.closePort();
                        return;
                    }
                }
            }
        });
        serialThreadOutput.start();

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
                        if(serialPort.isOpen() && !serialPort.getSystemPortName().equals(serialPortName)) {
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

        frequencySetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            if(outOfFocus) {
                try {
                    freq = Float.parseFloat(frequencySetField.getText());
                } catch (NumberFormatException e) {
                    //TODO: Set red area for incorrect input
                }
            }
        });
        currentSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            if(outOfFocus) {
                try {
                    current = Float.parseFloat(currentSetField.getText());
                } catch (NumberFormatException e) {
                    //TODO: Set red area for incorrect input
                }
            }
        });
        alphaFilterField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            if(outOfFocus) {
                try {
                    alpha = Float.parseFloat(alphaFilterField.getText());
                } catch (NumberFormatException e) {
                    //TODO: Set red area for incorrect input
                }
            }
        });
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
