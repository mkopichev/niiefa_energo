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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class MainController implements Initializable, Notification {

    @FXML
    public TextField currentP2P;

    @FXML
    public TextField currentFilteredP2P;

    @FXML
    public Button resetYaxis;

    @FXML
    private GridPane background;

    @FXML
    private ToggleButton acsEnableButton;

    @FXML
    private Text connectionStatus;

    @FXML
    private ComboBox<String> comPortChoice;

    @FXML
    private ComboBox<String> comBaudrateChoice;

    @FXML
    private Button comPortConnectButton;

    @FXML
    private TextField alphaFilterSetField;

    @FXML
    private TextField currentSetField;

    @FXML
    private TextField voltageSetField;

    @FXML
    private TextField durationSetField;

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
    private CheckBox plot4checkBox;

    @FXML
    private ToggleButton startButton;

    @FXML
    private TextField yMaxValueField;

    @FXML
    private TextField yMinValueField;

    private String serialPortName;

    private SerialPort serialPort;

    private InputStream inputStream;

    private OutputStream outputStream;

    private Thread serialThreadOutput;

    private Thread serialThreadInput;

    float alpha = 0.01f;
    float freq = 400.0f;
    float current = 0.0f;
    float voltage = 0.0f;
    byte controlSystem = 0;
    byte enable = 0;

    float duration_time = 1.0f;

    Float[] currentQueue = new Float[1000];
    Float[] currentFilteredQueue = new Float[1000];
    Float[] currentSetpointQueue = new Float[1000];
    Float[] voltageQueue = new Float[1000];
    Float frequencyQueue = 0.0f;

    Float[] currentSaved = new Float[1000];
    Float[] currentFilteredSaved = new Float[1000];
    Float[] currentSetpointSaved = new Float[1000];
    Float[] voltageSaved = new Float[1000];

    XYChart.Series<Number, Number> seriesCurrent;
    XYChart.Series<Number, Number> seriesCurrentFiltered;
    XYChart.Series<Number, Number> seriesCurrentSetpoint;
    XYChart.Series<Number, Number> seriesVoltage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        serialThreadOutput = new Thread(() -> {
            while (true) {
                try {
                    if (outputStream != null) {
                        // 2 start bytes, alpha_filter_float, frequency_float, current_float, mode_byte
                        ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 4 + 4 + 4 + 1 + 1);
                        buf.order(ByteOrder.LITTLE_ENDIAN);
                        buf.put((byte) 'a').put((byte) 'f').putFloat(alpha).putFloat(freq).putFloat(current).putFloat(voltage).put(controlSystem).put(enable);
                        outputStream.write(buf.array());
                    }
                } catch (SerialPortTimeoutException e) {
                    connectionStatus.setText("Ошибка COM-порта");
                    comBaudrateChoice.disableProperty().setValue(Boolean.FALSE);

                } catch (IOException e) {
                    connectionStatus.setText("Ошибка COM-порта");
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (serialPort != null) {
                        serialPort.closePort();
                    }
                    return;
                }
            }
        });
        serialThreadOutput.start();

        serialThreadInput = new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    if (inputStream != null) {
                        byte[] buf = new byte[22];
                        if (readInputStreamWithTimeout(inputStream, buf, 10, 22) == 22) {
                            ByteBuffer bb = ByteBuffer.wrap(buf);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            currentQueue[i] = bb.getFloat(2);
                            currentFilteredQueue[i] = bb.getFloat(6);
                            currentSetpointQueue[i] = bb.getFloat(10);
                            frequencyQueue = bb.getFloat(14);
                            voltageQueue[i] = bb.getFloat(18);

                            i++;
                        }
                        if (i >= 1000) {

                            if (!pause) {
                                System.arraycopy(currentQueue, 0, currentSaved, 0, 1000);
                                System.arraycopy(currentFilteredQueue, 0, currentFilteredSaved, 0, 1000);
                                System.arraycopy(currentSetpointQueue, 0, currentSetpointSaved, 0, 1000);
                                System.arraycopy(voltageQueue, 0, voltageSaved, 0, 1000);

                                Platform.runLater(() -> {
                                    frequencyField.setText(frequencyQueue.toString());
                                    List<Float> currentTmp = Arrays.asList(currentSaved);
                                    float p2p = Collections.max(currentTmp) - Collections.min(currentTmp);
                                    currentP2P.setText(Float.toString(p2p));

                                    currentTmp = Arrays.asList(currentFilteredSaved);
                                    p2p = Collections.max(currentTmp) - Collections.min(currentTmp);
                                    currentFilteredP2P.setText(Float.toString(p2p));

                                    seriesCurrent.getData().clear();
                                    for (int i1 = 0; i1 < 1000; i1++) {
                                        seriesCurrent.getData().add(new XYChart.Data<>(i1 * 0.001, currentSaved[i1]));
                                    }
                                    seriesCurrentFiltered.getData().clear();
                                    for (int i1 = 0; i1 < 1000; i1++) {
                                        seriesCurrentFiltered.getData().add(new XYChart.Data<>(i1 * 0.001, currentFilteredSaved[i1]));
                                    }
                                    seriesCurrentSetpoint.getData().clear();
                                    for (int i1 = 0; i1 < 1000; i1++) {
                                        seriesCurrentSetpoint.getData().add(new XYChart.Data<>(i1 * 0.001, currentSetpointSaved[i1]));
                                    }
                                    seriesVoltage.getData().clear();
                                    for (int i1 = 0; i1 < 1000; i1++) {
                                        seriesVoltage.getData().add(new XYChart.Data<>(i1 * 0.001, voltageSaved[i1]));
                                    }
                                });
                            }
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

                if (Thread.interrupted()) {
                    if (serialPort != null)
                        serialPort.closePort();
                    return;
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
                            comBaudrateChoice.disableProperty().setValue(Boolean.FALSE);
                            outputStream = null;
                            inputStream = null;
                        }
                    }
                }
            }
        });

        comBaudrateChoice.getItems().removeAll(comBaudrateChoice.getItems());
        comBaudrateChoice.getItems().addAll("4800", "9600", "19200", "38400", "57600", "115200", "230400");
        comBaudrateChoice.getSelectionModel().select("115200");

        yMinValueField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            yMinValueField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                double min;
                try {
                    min = Double.parseDouble(yMinValueField.getText().strip().replaceAll(",", "."));
                    lineChartArea.getYAxis().setAutoRanging(false);
                    ((NumberAxis) lineChartArea.getYAxis()).setLowerBound(min);
                    yMinValueField.setText(String.valueOf(min));
                } catch (NumberFormatException e) {
                    yMinValueField.getStyleClass().add("invalid");
                }
            }
        });

        yMaxValueField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            yMaxValueField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                double max;
                try {
                    max = Double.parseDouble(yMaxValueField.getText().strip().replaceAll(",", "."));
                    lineChartArea.getYAxis().setAutoRanging(false);
                    ((NumberAxis) lineChartArea.getYAxis()).setUpperBound(max);
                    yMaxValueField.setText(String.valueOf(max));
                } catch (NumberFormatException e) {
                    yMaxValueField.getStyleClass().add("invalid");
                }
            }
        });

        yMinValueField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });
        yMaxValueField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });
        comPortConnectButton.pressedProperty().addListener((observable, released, pressed) -> {
            if (released) {
                if (serialPortName == null)
                    return;
                if (serialPort != null)
                    serialPort.closePort();
                int baudRate = Integer.parseInt(comBaudrateChoice.getValue());
                comBaudrateChoice.disableProperty().setValue(Boolean.TRUE);
                serialPort = SerialPort.getCommPort(serialPortName);
                serialPort.setComPortParameters(baudRate, 8, 1, SerialPort.NO_PARITY);
                serialPort.openPort();
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                connectionStatus.setText("Подключено");
            }
        });
        background.setOnMousePressed(event -> background.requestFocus());
        alphaFilterSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            alphaFilterSetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    alpha = Float.parseFloat(alphaFilterSetField.getText().strip().replaceAll(",", "."));
                    if (alpha > 1) {
                        alpha = 1;
                    } else if (alpha < 0) {
                        alpha = 0;
                    }
                    alphaFilterSetField.setText(String.valueOf(alpha));
                } catch (NumberFormatException e) {
                    alphaFilterSetField.getStyleClass().add("invalid");
                }
            }
        });
        alphaFilterSetField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });

        voltageSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            frequencySetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    voltage = Float.parseFloat(voltageSetField.getText().strip().replaceAll(",", "."));
                    if (voltage > 400) {
                        voltage = 400;
                    } else if (voltage <= 0) {
                        voltage = 0;
                    }
                    voltageSetField.setText(String.valueOf(voltage));
                } catch (NumberFormatException e) {
                    voltageSetField.getStyleClass().add("invalid");
                }
            }
        });
        voltageSetField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });

        frequencySetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            frequencySetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    freq = Float.parseFloat(frequencySetField.getText().strip().replaceAll(",", "."));
                    if (freq > 400) {
                        freq = 400;
                    } else if (freq < 100) {
                        freq = 100;
                    }
                    frequencySetField.setText(String.valueOf(freq));
                } catch (NumberFormatException e) {
                    frequencySetField.getStyleClass().add("invalid");
                }
            }
        });
        frequencySetField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });
        currentSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            currentSetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    current = Float.parseFloat(currentSetField.getText().strip().replaceAll(",", "."));
                    if (current > 300) {
                        current = 300;
                    } else if (current < 0) {
                        current = 0;
                    }
                    currentSetField.setText(String.valueOf(current));
                } catch (NumberFormatException e) {
                    currentSetField.getStyleClass().add("invalid");
                }
            }
        });
        currentSetField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });
        durationSetField.focusedProperty().addListener((observable, outOfFocus, inFocus) -> {
            currentSetField.getStyleClass().removeAll("invalid");
            if (outOfFocus) {
                try {
                    duration_time = Float.parseFloat(durationSetField.getText().strip().replaceAll(",", "."));
                    if (duration_time > 1.0f) {
                        duration_time = 1.0f;
                    } else if (duration_time <= 0) {
                        duration_time = 0.1f;
                    }
                    durationSetField.setText(String.valueOf(duration_time));
                    Platform.runLater(() -> ((NumberAxis) lineChartArea.getXAxis()).setUpperBound(duration_time));
                } catch (NumberFormatException e) {
                    durationSetField.getStyleClass().add("invalid");
                }
            }
        });
        durationSetField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                background.requestFocus();
            }
        });

        alphaFilterSetField.setText(String.valueOf(alpha));
        currentSetField.setText(String.valueOf(current));
        frequencySetField.setText(String.valueOf(freq));

        lineChartArea.setAnimated(false);
        seriesCurrent = new XYChart.Series<>();
        seriesCurrent.setName("Ток");
        seriesCurrentSetpoint = new XYChart.Series<>();
        seriesCurrentSetpoint.setName("Уставка по току");
        seriesCurrentFiltered = new XYChart.Series<>();
        seriesCurrentFiltered.setName("Ток с альфа-фльтром");
        seriesVoltage = new XYChart.Series<>();
        seriesVoltage.setName("Напряжение");
        lineChartArea.getData().add(seriesCurrent);
        lineChartArea.getData().add(seriesCurrentFiltered);
        lineChartArea.getData().add(seriesCurrentSetpoint);
        lineChartArea.getData().add(seriesVoltage);
        seriesCurrent.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: rgba(0, 255, 0, 1)");
        seriesCurrentSetpoint.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: rgba(255, 0, 0, 1)");
        seriesCurrentFiltered.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: rgba(0, 0, 255, 1)");
        seriesVoltage.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: rgba(255, 255, 0, 1)");
        ((NumberAxis) lineChartArea.getXAxis()).setUpperBound(1);
        ((NumberAxis) lineChartArea.getXAxis()).setLowerBound(0);
        ((NumberAxis) lineChartArea.getYAxis()).setForceZeroInRange(false);

        resetYaxis.pressedProperty().addListener((observable, released, pressed) -> {
            if (released) {
                lineChartArea.getYAxis().setAutoRanging(true);
                yMinValueField.setText("");
                yMaxValueField.setText("");
                yMinValueField.getStyleClass().removeAll("invalid");
                yMaxValueField.getStyleClass().removeAll("invalid");
            }
        });
    }

    boolean pause = false;

    @FXML
    void onPauseButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Пауза");
            pause = false;
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Запуск");
            pause = true;
        }
    }

    @FXML
    void onAcsEnableButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            try {
                freq = Float.parseFloat(frequencyField.getText());
            } catch (NumberFormatException e) {
                freq = 400.0f;
            }
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Включить САУ");
            frequencySetField.setText(String.valueOf(freq));
            controlSystem = 0;
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Выключить САУ");
            controlSystem = 1;
        }
    }

    @FXML
    void onStartButtonPress(ActionEvent event) {
        if (((ToggleButton) event.getSource()).getStyleClass().contains("stop")) {
            ((ToggleButton) event.getSource()).getStyleClass().remove("stop");
            ((ToggleButton) event.getSource()).setText("Старт");
            enable = 0;
        } else {
            ((ToggleButton) event.getSource()).getStyleClass().add("stop");
            ((ToggleButton) event.getSource()).setText("Стоп");
            enable = 1;
        }
    }

    public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis, int length)
            throws IOException {
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (b[0] != (byte) 0xAA && b[1] != (byte) 0xBB) {
            if (readOneByteTimeout(is, b, 0, maxTimeMillis)) return -1;
            if (b[0] == (byte) 0xAA) {
                if (readOneByteTimeout(is, b, 1, maxTimeMillis)) return -1;
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
                return false;
            }
        }
        return true;
    }

    @FXML
    void checkBoxEvent(ActionEvent event) {
        CheckBox checkBox = (CheckBox) event.getSource();
        if (checkBox == plot1checkBox) {
            seriesCurrentSetpoint.getNode().setVisible(plot1checkBox.isSelected());
        }
        if (checkBox == plot2checkBox) {
            seriesCurrent.getNode().setVisible(plot2checkBox.isSelected());
        }
        if (checkBox == plot3checkBox) {
            seriesCurrentFiltered.getNode().setVisible(plot3checkBox.isSelected());
        }
    }

    @Override
    public void closeApp() {
        freq = 400;
        current = 0;
        controlSystem = 0;
        enable = 0;
        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            serialThreadOutput.interrupt();
            serialThreadInput.interrupt();
        });
        stopper.start();
    }
}
