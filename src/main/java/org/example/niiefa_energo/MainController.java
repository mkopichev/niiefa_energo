package org.example.niiefa_energo;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import com.google.common.collect.EvictingQueue;

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
    private LineChart<Number, Number> lineChartArea;

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

    private Thread plotThread;

    float alpha = 0.0f;
    float freq = 0.0f;
    float current = 0.0f;
    float duration_time = 0.0f;

    Float[] currentQueue = new Float[1000];
    Float[] currentFilteredQueue = new Float[1000];
    Float[] currentSetpointQueue = new Float[1000];
    Float frequencyQueue = 0.0f;

    XYChart.Series<Number, Number> dataset1;
    XYChart.Series<Number, Number> dataset2;
    XYChart.Series<Number, Number> dataset3;
    XYChart.Series<Number, Number> dataset4;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        serialThreadOutput = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (outputStream != null) {
                            // 2 start bytes, alpha_filter_float, frequency_float, current_float, mode_byte
                            ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 4 + 4 + 1 + 1);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            buf.put((byte) 'a').put((byte) 'f').putFloat(alpha).putFloat(freq).putFloat(current).put((byte) 112).put((byte) 0xAA);
                            outputStream.write(buf.array());
                        }
                    } catch (SerialPortTimeoutException e) {
                        connectionStatus.setText("Ошибка COM-порта");
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
                int i = 0;
                while (true) {
                    try {
                        if(inputStream != null) {
                            byte[] buf = new byte[10];
                            if (readInputStreamWithTimeout(inputStream, buf, 10, 10) == 10) {
                                System.out.println(Arrays.toString(buf));
                                ByteBuffer bb = ByteBuffer.wrap(buf);
                                bb.order(ByteOrder.LITTLE_ENDIAN);
                                currentQueue[i] = bb.getFloat(2);
                                currentFilteredQueue[i] = bb.getFloat(6);
//                                currentSetpointQueue[i] = bb.getFloat(10);
//                                frequencyQueue = bb.getFloat(14);
//                                System.out.println(currentQueue[i].toString() + '\t' + currentFilteredQueue[i].toString()
//                                        + '\t' + currentSetpointQueue[i].toString() + '\t' + frequencyQueue[i].toString());
                                i++;
                            }
                            if(i >= 1000) {
                                dataset1 = generateSeries(currentQueue, "Current");
                                dataset2 = generateSeries(currentFilteredQueue, "Current Filtered");
//                                dataset3 = generateSeries(currentSetpointQueue, "Current Setpoint");

                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            lineChartArea.getData().remove(0, 2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        lineChartArea.getData().add(dataset1);
                                        lineChartArea.getData().add(dataset2);
//                                        lineChartArea.getData().add(dataset3);
                                    }
                                });
                                i = 0;
                            }
                        }
                    } catch (SerialPortTimeoutException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        inputStream = null;
                        i = 0;
                        connectionStatus.setText("Ошибка COM-порта");
                    }

                    if(Thread.interrupted()) {
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
        background.setOnMousePressed(event -> background.requestFocus());
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
        duration.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            currentSetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    duration_time = Float.parseFloat(duration.getText().strip().replaceAll(",", "."));
                    if(duration_time > 1.0f){
                        duration.setText(String.valueOf(1.0f));
                        duration_time = 1.0f;
                    }
                } catch (NumberFormatException e) {
                    duration.getStyleClass().add("invalid");
                }
            }
        });

        lineChartArea.setAnimated(false);


    }

    private XYChart.Series<Number, Number> generateSeries(Float[] y, String name){
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        List<XYChart.Data<Number, Number>> dataPoints = new ArrayList<>();
        for(int i = 0; i < 1000; i++){
            dataPoints.add(new XYChart.Data<>(i, y[i]));
        }
        series.getData().addAll(dataPoints);
        return series;
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

    public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis, int length)
            throws IOException {
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (b[0] != (byte)0xAA && b[1] != (byte)0xBB) {
            if (!readOneByteTimeout(is, b, 0, maxTimeMillis)) return -1;
            if (b[0] == (byte)0xAA) {
                if (!readOneByteTimeout(is, b, 1, maxTimeMillis)) return -1;
            }
        }
        int bufferOffset = 2;
        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
            int readLength = java.lang.Math.min(is.available(), length - bufferOffset);
            // can alternatively use bufferedReader, guarded by isReady():
            int readResult = is.read(b, bufferOffset, readLength);
            if (readResult == -1) break;
            bufferOffset += readResult;
        }
        return bufferOffset;
    }

    public static boolean readOneByteTimeout(InputStream is, byte[] buffer, int position, long endTime) throws IOException {
        while (System.currentTimeMillis() < endTime) {
            if (is.available() > 0) {
                is.read(buffer, position, 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void closeApp() {
        serialThreadOutput.interrupt();
        serialThreadInput.interrupt();
    }
}
