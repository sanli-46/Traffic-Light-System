package com.alperensanli.traffic_light_controller_system_demo_v1;

import javafx.animation.KeyFrame;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.*;

public class IntersectionController {

    private final Map<Direction, Integer> carsCreatedCount = new HashMap<>();
    private final Map<Direction, Integer> carsPassedCount = new HashMap<>();
    private final List<Timer> activeTimers = new ArrayList<>();
    private static final double CAR_SPACING = 30;
    private final IntersectionModel model = new IntersectionModel();


    private final List<PathTransition> activeTransitions = new ArrayList<>();


    private LightPhase phase = LightPhase.RED;
    private final Random random = new Random();
    private Map<Direction, Integer> greenDurations;
    private Timeline timeline;
    private int currentIndex = 0;
    private Direction[] order;
    private int remainingSeconds = 0;
    private Direction activeDirection;
    private boolean initial = true;
    private final Map<String, Path> directionPaths = new HashMap<>();

    @FXML
    private javafx.scene.control.Label northTimer;
    @FXML
    private javafx.scene.control.Label southTimer;
    @FXML
    private javafx.scene.control.Label eastTimer;
    @FXML
    private javafx.scene.control.Label westTimer;
    @FXML
    private TextField northInput, southInput, eastInput, westInput;
    @FXML
    private Circle northRed, northYellow, northGreen;
    @FXML
    private Circle southRed, southYellow, southGreen;
    @FXML
    private Circle eastRed, eastYellow, eastGreen;
    @FXML
    private Circle westRed, westYellow, westGreen;
    @FXML
    private Pane roadPane;
    @FXML
    private VBox northSignal, southSignal, eastSignal, westSignal;


    /**
     * Her bir yön için rastgele araç sayısı üretir ve ilgili giriş alanlarına yerleştirir.
     */
    @FXML
    public void onRandomClicked() {
        northInput.setText(String.valueOf(random.nextInt(100)));
        southInput.setText(String.valueOf(random.nextInt(100)));
        eastInput.setText(String.valueOf(random.nextInt(100)));
        westInput.setText(String.valueOf(random.nextInt(100)));
    }


    /**
     * Kullanıcı tarafından başlatıldığında simülasyonu başlatır.
     * Araç sayısını alır, yolları oluşturur, araçları sıraya koyar ve zamanlamayı başlatır.
     */
    @FXML
    public void onStartClicked() {
        updateLightsToRed();
        int northCount = Integer.parseInt(northInput.getText());
        int southCount = Integer.parseInt(southInput.getText());
        int eastCount = Integer.parseInt(eastInput.getText());
        int westCount = Integer.parseInt(westInput.getText());

        model.setVehicleCount("NORTH", northCount);
        model.setVehicleCount("SOUTH", southCount);
        model.setVehicleCount("EAST", eastCount);
        model.setVehicleCount("WEST", westCount);


        order = model.getDirectionsSortedByDensity().toArray(new Direction[0]);


        if (order.length == 0) {
            System.err.println("Araç yoğunluğu sıfır! Lütfen değer girin.");
            return;
        }

        model.calculateGreenDurations();
        greenDurations = model.getGreenDurations();
        activeDirection = order[0];
        currentIndex = 1;


        model.prepareCarQueues();
        for (Direction d : Direction.values()) {
            carsCreatedCount.put(d, 0);
            carsPassedCount.put(d, 0);
        }



        for (Direction dir : Direction.values()) {
            Timeline spawner = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int created = carsCreatedCount.getOrDefault(dir, 0);
                int target = model.getVehicleCount(dir);

                if ((created < target)) {
                    model.addCarToQueue(dir.name());
                    carsCreatedCount.put(dir, created + 1);
                    Queue<Car> carQueue = model.getCarQueue(dir);
                    Car[] carArray = carQueue.toArray(new Car[0]);
                    Car car = carArray.length > 0 ? carArray[carArray.length - 1] : null;

                    if (car != null) {
                        Rectangle shape = car.getShape();
                        shape.setTranslateX(getStartX(dir.name()));
                        shape.setTranslateY(getStartY(dir.name()));

                        if (!roadPane.getChildren().contains(shape)) {
                            roadPane.getChildren().add(shape);
                        }

                        if (phase == LightPhase.GREEN && dir == activeDirection &&
                            model.getCarQueue(dir).stream().noneMatch(c -> !c.hasMoved())) {
                            String key = dir.name() + "_" + car.getExit().name();
                            Path path = directionPaths.get(key);
                            if (path != null) {
                                car.moveFromCurrentPosition(path, Duration.seconds(5), () -> {
                                    car.markMoved();
                                    roadPane.getChildren().remove(car.getShape());
                                    model.getCarQueue(dir).remove(car);
                                    carsPassedCount.put(dir, carsPassedCount.getOrDefault(dir, 0) + 1);
                                });
                            }
                        } else {
                            long position = model.getCarQueue(dir).stream().filter(c -> !c.hasMoved()).count();
                            double offset = position * CAR_SPACING;
                            double stopX = getStopX(dir.name(), offset);
                            double stopY = getStopY(dir.name(), offset);

                            Path spawnPath = new Path();
                            spawnPath.getElements().add(new MoveTo(shape.getTranslateX(), shape.getTranslateY()));
                            spawnPath.getElements().add(new LineTo(stopX, stopY));

                            PathTransition pt = new PathTransition();
                            pt.setNode(shape);
                            pt.setPath(spawnPath);
                            pt.setDuration(Duration.seconds(1));
                            pt.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
                            pt.setOnFinished(event -> {
                                shape.setTranslateX(stopX);
                                shape.setTranslateY(stopY);
                                shape.toFront();
                            });
                            // Track this transition for pausing
                            activeTransitions.add(pt);
                            pt.play();

                            if (dir.name().equals("NORTH")) shape.setRotate(270);
                            else if (dir.name().equals("SOUTH")) shape.setRotate(90);
                            else if (dir.name().equals("EAST")) shape.setRotate(0);
                            else if (dir.name().equals("WEST")) shape.setRotate(180);
                        }
                    }
                }
            }));
            spawner.setCycleCount(model.getVehicleCount(dir));
            spawner.play();
        }

        initial = true;
        phase = LightPhase.RED;
        remainingSeconds = 0;

        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> runCycle()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        startCountdownUpdater();
    }


    private void runCycle() {
        if (remainingSeconds <= 0) {
            if (phase == LightPhase.GREEN) {
                for (Timer t : activeTimers) {
                    t.cancel();
                }
                activeTimers.clear();
                phase = LightPhase.YELLOW_AFTER_GREEN;
                remainingSeconds = 3;
                System.out.println("SARI " + activeDirection + " (3 Saniye)");
                updateLightsToYellow(activeDirection);

                Queue<Car> waitList = model.getCarQueue(activeDirection);
                if (waitList != null) {
                    List<Car> waitListArr = new ArrayList<>(waitList);
                    for (int i = 0; i < waitListArr.size(); i++) {
                        Car car = waitListArr.get(i);
                        Rectangle shape = car.getShape();
                        double offset = (i + 1) * CAR_SPACING;
                        double stopX = getStopX(activeDirection.name(), offset);
                        double stopY = getStopY(activeDirection.name(), offset);

                        if (activeDirection.name().equals("NORTH")) stopX -= 12;
                        else if (activeDirection.name().equals("SOUTH")) stopX += 2;
                        else if (activeDirection.name().equals("EAST")) stopY -= 8;
                        else if (activeDirection.name().equals("WEST")) stopY -= 2;

                        shape.setTranslateX(stopX);
                        shape.setTranslateY(stopY);

                        if (activeDirection.name().equals("NORTH")) shape.setRotate(270);
                        else if (activeDirection.name().equals("SOUTH")) shape.setRotate(90);
                        else if (activeDirection.name().equals("EAST")) shape.setRotate(0);
                        else if (activeDirection.name().equals("WEST")) shape.setRotate(180);

                        if (!roadPane.getChildren().contains(shape)) {
                            roadPane.getChildren().add(shape);
                        }
                    }
                }

            } else if (phase == LightPhase.YELLOW_AFTER_GREEN) {
                phase = LightPhase.RED;
                remainingSeconds = 1;
                updateLightsToRed();
            } else if (phase == LightPhase.RED) {
                if (initial) {
                    initial = false;
                } else {
                    if (currentIndex >= order.length) currentIndex = 0;
                    activeDirection = order[currentIndex];
                    currentIndex++;
                }
                phase = LightPhase.YELLOW_BEFORE_GREEN;
                remainingSeconds = 3;
                System.out.println("SARI " + activeDirection + " (3 Saniye)");
                updateLightsToYellow(activeDirection);
            } else if (phase == LightPhase.YELLOW_BEFORE_GREEN) {
                phase = LightPhase.GREEN;
                Queue<Car> queue = model.getCarQueue(Direction.valueOf(activeDirection.name()));
                System.out.println("Kuyruk kontrol: " + activeDirection + " yönünde " + queue.size() + " araç var.");
                if (queue != null && !queue.isEmpty()) {
                    Queue<Car> waitingList = model.getCarQueue(activeDirection);
                    List<Car> waitingArr = new ArrayList<>(waitingList);
                    for (int i = 0; i < waitingArr.size(); i++) {
                        Car car = waitingArr.get(i);
                        Rectangle shape = car.getShape();
                        double offset = (i + 1) * CAR_SPACING;
                        double stopX = getStopX(activeDirection.name(), offset);
                        double stopY = getStopY(activeDirection.name(), offset);

                        if (activeDirection.name().equals("NORTH")) stopX -= 12;
                        else if (activeDirection.name().equals("SOUTH")) stopX += 2;
                        else if (activeDirection.name().equals("EAST")) stopY -= 8;
                        else if (activeDirection.name().equals("WEST")) stopY -= 2;

                        // Only move cars that have not moved and are not currently moving
                        if (!car.hasMoved() && !car.isMoving()) {
                            if (Math.abs(shape.getTranslateX() - stopX) > 1 || Math.abs(shape.getTranslateY() - stopY) > 1) {
                                shape.setTranslateX(stopX);
                                shape.setTranslateY(stopY);
                            }
                        }

                        if (activeDirection.name().equals("NORTH")) shape.setRotate(270);
                        else if (activeDirection.name().equals("SOUTH")) shape.setRotate(90);
                        else if (activeDirection.name().equals("EAST")) shape.setRotate(0);
                        else if (activeDirection.name().equals("WEST")) shape.setRotate(180);

                        if (!roadPane.getChildren().contains(shape)) {
                            roadPane.getChildren().add(shape);
                        }
                    }

                    System.out.println("Total in queue for " + activeDirection + ": " + queue.size());
                    int duration = greenDurations.getOrDefault(activeDirection, 10);
                    // Dynamic Timer-based dispatcher for green phase
                    Timer greenDispatcher = new Timer();
                    activeTimers.add(greenDispatcher);
                    greenDispatcher.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                Queue<Car> queue = model.getCarQueue(activeDirection);
                                for (Car car : queue) {
                                    if (!car.hasMoved() && !car.isMoving()) {
                                        String key = activeDirection.name() + "_" + car.getExit().name();
                                        Path path = directionPaths.get(key);
                                        if (path != null) {
                                            car.setMoving(true);
                                            car.moveFromCurrentPosition(path, Duration.seconds(5), () -> {
                                                car.setMoving(false);
                                                car.markMoved();
                                                roadPane.getChildren().remove(car.getShape());
                                                queue.remove(car);
                                                carsPassedCount.put(activeDirection, carsPassedCount.getOrDefault(activeDirection, 0) + 1);
                                            });
                                        }
                                        break; // only one car per tick
                                    }
                                }
                            });
                        }
                    }, 0, 1000);
                }
                int duration = greenDurations.getOrDefault(activeDirection, 10);
                remainingSeconds = duration;
                updateLightsToGreen(activeDirection);
                updateCountdownLabel(activeDirection, remainingSeconds);
                System.out.println("YESIL " + activeDirection + " (" + remainingSeconds + " Saniye)");
            }
        } else {
            remainingSeconds--;
            updateCountdownLabel(activeDirection, remainingSeconds);
        }
    }

    private void updateCountdownLabel(Direction dir, int seconds) {
        if (dir == null) return;

        javafx.scene.control.Label label = switch (dir) {
            case NORTH -> northTimer;
            case SOUTH -> southTimer;
            case EAST -> eastTimer;
            case WEST -> westTimer;
        };
        label.setText(seconds + "s");

        if (dir != Direction.NORTH) northTimer.setText("");
        if (dir != Direction.SOUTH) southTimer.setText("");
        if (dir != Direction.EAST) eastTimer.setText("");
        if (dir != Direction.WEST) westTimer.setText("");
    }


    private double getStopX(String dir, double offset) {
        double cx = roadPane.getWidth() / 2;
        return switch (dir) {
            case "NORTH" -> cx - 16;
            case "SOUTH" -> cx + 10;
            case "EAST" -> cx + offset + 70;
            case "WEST" -> cx - offset - 70;
            default -> cx;
        };
    }


    private double getStopY(String dir, double offset) {
        double cy = roadPane.getHeight() / 2;
        return switch (dir) {
            case "NORTH" -> cy - offset - 70;
            case "SOUTH" -> cy + offset + 70;
            case "EAST" -> cy - 16;
            case "WEST" -> cy + 4;
            default -> cy;
        };
    }

    private void updateLightsToRed() {
        northRed.setFill(Color.RED);
        northYellow.setFill(Color.GRAY);
        northGreen.setFill(Color.GRAY);
        southRed.setFill(Color.RED);
        southYellow.setFill(Color.GRAY);
        southGreen.setFill(Color.GRAY);
        eastRed.setFill(Color.RED);
        eastYellow.setFill(Color.GRAY);
        eastGreen.setFill(Color.GRAY);
        westRed.setFill(Color.RED);
        westYellow.setFill(Color.GRAY);
        westGreen.setFill(Color.GRAY);
    }

    private void updateLightsToYellow(Direction active) {
        updateLightsToRed();

        switch (active) {
            case NORTH -> {
                northRed.setFill(Color.GRAY);
                northYellow.setFill(Color.GOLD);
            }
            case SOUTH -> {
                southRed.setFill(Color.GRAY);
                southYellow.setFill(Color.GOLD);
            }
            case EAST -> {
                eastRed.setFill(Color.GRAY);
                eastYellow.setFill(Color.GOLD);
            }
            case WEST -> {
                westRed.setFill(Color.GRAY);
                westYellow.setFill(Color.GOLD);
            }
        }

    }

    private void updateLightsToGreen(Direction active) {
        updateLightsToRed();

        switch (active) {
            case NORTH -> {
                northRed.setFill(Color.GRAY);
                northGreen.setFill(Color.LIMEGREEN);
            }
            case SOUTH -> {
                southRed.setFill(Color.GRAY);
                southGreen.setFill(Color.LIMEGREEN);
            }
            case EAST -> {
                eastRed.setFill(Color.GRAY);
                eastGreen.setFill(Color.LIMEGREEN);
            }
            case WEST -> {
                westRed.setFill(Color.GRAY);
                westGreen.setFill(Color.LIMEGREEN);
            }
        }

    }


    /**
     * Simülasyonu duraklatır.
     * Tüm aktif zamanlayıcıları ve araç animasyonlarını geçici olarak durdurur.
     */
    @FXML
    public void onPauseClicked() {
        if (timeline != null) {
            timeline.pause();
            System.out.println("Simülasyon duraklatıldı.");
        }
        for (Timer timer : activeTimers) {
            timer.cancel();
        }
        activeTimers.clear();
        // Pause all active PathTransitions
        for (PathTransition pt : activeTransitions) {
            pt.pause();
        }
    }

    /**
     * Uygulama başlatıldığında kullanıcı arayüzünü, yol görsellerini ve trafik ışıklarını hazırlar.
     */
    public void initialize() {

        // Trafik ışıklarının yol kenarına hizalanması
        northSignal.layoutXProperty().bind(roadPane.widthProperty().divide(2).subtract(northSignal.widthProperty().divide(2)).add(-45));
        northSignal.layoutYProperty().bind(
                Bindings.subtract(
                        roadPane.heightProperty().divide(2),
                        Bindings.add(60, northSignal.heightProperty())
                )
        );

        southSignal.layoutXProperty().bind(roadPane.widthProperty().divide(2).subtract(southSignal.widthProperty().divide(2)).add(45));
        southSignal.layoutYProperty().bind(roadPane.heightProperty().divide(2).add(60));

        eastSignal.layoutXProperty().bind(roadPane.widthProperty().divide(2).add(60));
        eastSignal.layoutYProperty().bind(roadPane.heightProperty().divide(2).subtract(eastSignal.heightProperty().divide(2)).add(-45));

        westSignal.layoutXProperty().bind(
                Bindings.subtract(
                        roadPane.widthProperty().divide(2),
                        Bindings.add(60, westSignal.widthProperty())
                )
        );
        westSignal.layoutYProperty().bind(roadPane.heightProperty().divide(2).subtract(westSignal.heightProperty().divide(2)).add(45));

        // Trafik ışıklarının boyutlandırılması
        northRed.setRadius(5);
        northYellow.setRadius(5);
        northGreen.setRadius(5);

        southRed.setRadius(5);
        southYellow.setRadius(5);
        southGreen.setRadius(5);

        eastRed.setRadius(5);
        eastYellow.setRadius(5);
        eastGreen.setRadius(5);

        westRed.setRadius(5);
        westYellow.setRadius(5);
        westGreen.setRadius(5);

        // Dikey ve yatay yol şekillerinin tanımlanması
        Rectangle verticalRoad = new Rectangle();
        verticalRoad.setFill(Color.GRAY);
        verticalRoad.setY(0);
        verticalRoad.setWidth(60);
        verticalRoad.heightProperty().bind(roadPane.heightProperty());
        verticalRoad.xProperty().bind(roadPane.widthProperty().divide(2).subtract(30));

        Rectangle horizontalRoad = new Rectangle();
        horizontalRoad.setFill(Color.GRAY);
        horizontalRoad.setX(0);
        horizontalRoad.setHeight(60);
        horizontalRoad.widthProperty().bind(roadPane.widthProperty());
        horizontalRoad.yProperty().bind(roadPane.heightProperty().divide(2).subtract(30));

        roadPane.getChildren().addAll(verticalRoad, horizontalRoad);

        // Yollardaki şerit çizgileri (kesikli çizgiler)
        Line vertLineTop = new Line();
        vertLineTop.startXProperty().bind(roadPane.widthProperty().divide(2).subtract(0));
        vertLineTop.startYProperty().set(0);
        vertLineTop.endXProperty().bind(roadPane.widthProperty().divide(2).subtract(0));
        vertLineTop.endYProperty().bind(roadPane.heightProperty().divide(2).subtract(10));
        vertLineTop.getStrokeDashArray().addAll(4.0, 6.0);

        Line vertLineBottom = new Line();
        vertLineBottom.startXProperty().bind(roadPane.widthProperty().divide(2).subtract(0));
        vertLineBottom.startYProperty().bind(roadPane.heightProperty().divide(2).add(10));
        vertLineBottom.endXProperty().bind(roadPane.widthProperty().divide(2).subtract(0));
        vertLineBottom.endYProperty().bind(roadPane.heightProperty());
        vertLineBottom.getStrokeDashArray().addAll(4.0, 6.0);

        Line horzLeft = new Line();
        horzLeft.startXProperty().set(0);
        horzLeft.endXProperty().bind(roadPane.widthProperty().divide(2).subtract(10));
        horzLeft.startYProperty().bind(roadPane.heightProperty().divide(2).subtract(0));
        horzLeft.endYProperty().bind(roadPane.heightProperty().divide(2).subtract(0));
        horzLeft.getStrokeDashArray().addAll(4.0, 6.0);
        horzLeft.setStroke(Color.WHITE);
        horzLeft.setStrokeWidth(2);
        roadPane.getChildren().add(horzLeft);

        Line horzRight = new Line();
        horzRight.startXProperty().bind(roadPane.widthProperty().divide(2).add(10));
        horzRight.endXProperty().bind(roadPane.widthProperty());
        horzRight.startYProperty().bind(roadPane.heightProperty().divide(2).subtract(0));
        horzRight.endYProperty().bind(roadPane.heightProperty().divide(2).subtract(0));
        horzRight.getStrokeDashArray().addAll(4.0, 6.0);
        horzRight.setStroke(Color.WHITE);
        horzRight.setStrokeWidth(2);
        roadPane.getChildren().add(horzRight);

        roadPane.getChildren().addAll(vertLineTop, vertLineBottom);

        // Ortadaki yönlere göre çizgiler
        for (int i = -2; i <= 2; i++) {

            Line n = new Line();
            n.startXProperty().bind(roadPane.widthProperty().divide(2).add(i * 6));
            n.startYProperty().bind(roadPane.heightProperty().divide(2).subtract(30));
            n.endXProperty().bind(roadPane.widthProperty().divide(2).add(i * 6));
            n.endYProperty().bind(roadPane.heightProperty().divide(2).subtract(20));
            n.setStroke(Color.WHITE);
            n.setStrokeWidth(2);
            roadPane.getChildren().add(n);

            Line s = new Line();
            s.startXProperty().bind(roadPane.widthProperty().divide(2).add(i * 6));
            s.startYProperty().bind(roadPane.heightProperty().divide(2).add(20));
            s.endXProperty().bind(roadPane.widthProperty().divide(2).add(i * 6));
            s.endYProperty().bind(roadPane.heightProperty().divide(2).add(30));
            s.setStroke(Color.WHITE);
            s.setStrokeWidth(2);
            roadPane.getChildren().add(s);

            Line e = new Line();
            e.startXProperty().bind(roadPane.widthProperty().divide(2).add(20));
            e.startYProperty().bind(roadPane.heightProperty().divide(2).add(i * 6));
            e.endXProperty().bind(roadPane.widthProperty().divide(2).add(30));
            e.endYProperty().bind(roadPane.heightProperty().divide(2).add(i * 6));
            e.setStroke(Color.WHITE);
            e.setStrokeWidth(2);
            roadPane.getChildren().add(e);

            Line w = new Line();
            w.startXProperty().bind(roadPane.widthProperty().divide(2).subtract(30));
            w.startYProperty().bind(roadPane.heightProperty().divide(2).add(i * 6));
            w.endXProperty().bind(roadPane.widthProperty().divide(2).subtract(20));
            w.endYProperty().bind(roadPane.heightProperty().divide(2).add(i * 6));
            w.setStroke(Color.WHITE);
            w.setStrokeWidth(2);
            roadPane.getChildren().add(w);
        }

        northSignal.toFront();
        southSignal.toFront();
        eastSignal.toFront();
        westSignal.toFront();

        // Ekran boyutu değiştiğinde yol yönlerini güncelleyen dinleyici
        ChangeListener<Number> routeUpdater = (obs, oldVal, newVal) -> {

            roadPane.getChildren().removeIf(node ->
                    (node instanceof Rectangle && ((Rectangle) node).getFill().equals(Color.BLUE)) ||
                            (node instanceof Path && (((Path) node).getStroke().equals(Color.GRAY) || ((Path) node).getStroke().equals(Color.BLACK))));


            double cx = roadPane.getWidth() / 2;
            double cy = roadPane.getHeight() / 2;



            Path northToSouthPathNew = new Path();
            MoveTo nsMove = new MoveTo(cx - 10, cy - 400);
            LineTo nsLine = new LineTo(cx - 10, cy + 400);
            northToSouthPathNew.getElements().addAll(nsMove, nsLine);
            northToSouthPathNew.setStroke(Color.GRAY);
            northToSouthPathNew.setStrokeWidth(2);
            northToSouthPathNew.setFill(null);
            roadPane.getChildren().add(northToSouthPathNew);


            Path southToNorthPathNew = new Path();
            MoveTo snMove = new MoveTo(cx + 10, cy + 400);
            LineTo snLine = new LineTo(cx + 10, cy - 400);
            southToNorthPathNew.getElements().addAll(snMove, snLine);
            southToNorthPathNew.setStroke(Color.GRAY);
            southToNorthPathNew.setStrokeWidth(2);
            southToNorthPathNew.setFill(null);
            roadPane.getChildren().add(southToNorthPathNew);


            Path eastToWestPathNew = new Path();
            MoveTo ewMove = new MoveTo(cx + 400, cy - 10);
            LineTo ewLine = new LineTo(cx - 400, cy - 10);
            eastToWestPathNew.getElements().addAll(ewMove, ewLine);
            eastToWestPathNew.setStroke(Color.GRAY);
            eastToWestPathNew.setStrokeWidth(2);
            eastToWestPathNew.setFill(null);
            roadPane.getChildren().add(eastToWestPathNew);


            Path westToEastPathNew = new Path();
            MoveTo weMove = new MoveTo(cx - 400, cy + 10);
            LineTo weLine = new LineTo(cx + 400, cy + 10);
            westToEastPathNew.getElements().addAll(weMove, weLine);
            westToEastPathNew.setStroke(Color.GRAY);
            westToEastPathNew.setStrokeWidth(2);
            westToEastPathNew.setFill(null);
            roadPane.getChildren().add(westToEastPathNew);


            Path northRightNewPath = new Path();
            MoveTo nwMove = new MoveTo(cx - 10, cy - 400);
            LineTo nwLine = new LineTo(cx - 10, cy - 50);
            ArcTo nwArc = new ArcTo();
            nwArc.setX(cx - 50);
            nwArc.setY(cy - 10);
            nwArc.setRadiusX(50);
            nwArc.setRadiusY(50);
            nwArc.setSweepFlag(true);
            nwArc.setLargeArcFlag(false);
            LineTo nwLine2 = new LineTo(cx - 400, cy - 10);
            northRightNewPath.getElements().setAll(nwMove, nwLine, nwArc, nwLine2);
            northRightNewPath.setStroke(Color.GRAY);
            northRightNewPath.setStrokeWidth(2);
            northRightNewPath.setFill(null);
            roadPane.getChildren().add(northRightNewPath);


            Path southRightNewPath = new Path();
            MoveTo seMove = new MoveTo(cx + 10, cy + 400);
            LineTo seLine = new LineTo(cx + 10, cy + 50);
            ArcTo seArc = new ArcTo();
            seArc.setX(cx + 50);
            seArc.setY(cy + 10);
            seArc.setRadiusX(50);
            seArc.setRadiusY(50);
            seArc.setSweepFlag(true);
            seArc.setLargeArcFlag(false);
            LineTo seLine2 = new LineTo(cx + 400, cy + 10);
            southRightNewPath.getElements().setAll(seMove, seLine, seArc, seLine2);
            southRightNewPath.setStroke(Color.GRAY);
            southRightNewPath.setStrokeWidth(2);
            southRightNewPath.setFill(null);
            roadPane.getChildren().add(southRightNewPath);


            Path eastRightNewPath = new Path();
            MoveTo enMove = new MoveTo(cx + 400, cy - 10);
            LineTo enLine = new LineTo(cx + 50, cy - 10);
            ArcTo enArc = new ArcTo();
            enArc.setX(cx + 10);
            enArc.setY(cy - 50);
            enArc.setRadiusX(50);
            enArc.setRadiusY(50);
            enArc.setSweepFlag(true);
            enArc.setLargeArcFlag(false);
            LineTo enLine2 = new LineTo(cx + 10, cy - 400);
            eastRightNewPath.getElements().setAll(enMove, enLine, enArc, enLine2);
            eastRightNewPath.setStroke(Color.GRAY);
            eastRightNewPath.setStrokeWidth(2);
            eastRightNewPath.setFill(null);
            roadPane.getChildren().add(eastRightNewPath);


            Path westToSouthPath = new Path();
            MoveTo wsMove = new MoveTo(cx - 400, cy + 10);
            LineTo wsLine = new LineTo(cx - 50, cy + 10);
            ArcTo wsArc = new ArcTo();
            wsArc.setX(cx - 10);
            wsArc.setY(cy + 50);
            wsArc.setRadiusX(50);
            wsArc.setRadiusY(50);
            wsArc.setSweepFlag(true);
            wsArc.setLargeArcFlag(false);
            LineTo wsLine2 = new LineTo(cx - 10, cy + 400);
            westToSouthPath.getElements().setAll(wsMove, wsLine, wsArc, wsLine2);
            westToSouthPath.setStroke(Color.GRAY);
            westToSouthPath.setStrokeWidth(2);
            westToSouthPath.setFill(null);
            roadPane.getChildren().add(westToSouthPath);

            Path westToNorthPath = new Path();
            MoveTo wnMove = new MoveTo(cx - 400, cy + 10);
            LineTo wnLine1 = new LineTo(cx - 20, cy + 10);
            ArcTo wnArc = new ArcTo();
            wnArc.setX(cx + 10);
            wnArc.setY(cy - 30);
            wnArc.setRadiusX(40);
            wnArc.setRadiusY(40);
            wnArc.setSweepFlag(false);
            wnArc.setLargeArcFlag(false);
            LineTo wnLine2 = new LineTo(cx + 10, cy - 400);
            westToNorthPath.getElements().addAll(wnMove, wnLine1, wnArc, wnLine2);
            westToNorthPath.setStroke(Color.GRAY);
            westToNorthPath.setStrokeWidth(2);
            westToNorthPath.setFill(null);
            roadPane.getChildren().add(westToNorthPath);

            Path northToEastPath = new Path();
            MoveTo neMove = new MoveTo(cx - 10, cy - 400);
            LineTo neLine1 = new LineTo(cx - 10, cy - 30);

            ArcTo neArc = new ArcTo();
            neArc.setX(cx + 40);
            neArc.setY(cy + 20);
            neArc.setRadiusX(80);
            neArc.setRadiusY(80);
            neArc.setSweepFlag(false);
            neArc.setLargeArcFlag(false);

            LineTo neLine2 = new LineTo(cx + 400, cy + 20);

            northToEastPath.getElements().addAll(neMove, neLine1, neArc, neLine2);
            northToEastPath.setStroke(Color.GRAY);
            northToEastPath.setStrokeWidth(2);
            northToEastPath.setFill(null);
            roadPane.getChildren().add(northToEastPath);

            Path aeastToSouthPath = new Path();


            MoveTo esMove = new MoveTo(cx + 400, cy - 10);


            LineTo esLine1 = new LineTo(cx + 30, cy - 10);


            ArcTo esArc = new ArcTo();
            esArc.setX(cx - 10);
            esArc.setY(cy + 30);
            esArc.setRadiusX(60);
            esArc.setRadiusY(60);
            esArc.setSweepFlag(false);
            esArc.setLargeArcFlag(false);

            LineTo esLine2 = new LineTo(cx - 10, cy + 400);
            aeastToSouthPath.getElements().addAll(esMove, esLine1, esArc, esLine2);
            aeastToSouthPath.setStroke(Color.GRAY);
            aeastToSouthPath.setStrokeWidth(2);
            aeastToSouthPath.setFill(null);

            roadPane.getChildren().add(aeastToSouthPath);


            Path southToWestPath = new Path();
            MoveTo swMove = new MoveTo(cx + 10, cy + 500);
            LineTo swLine = new LineTo(cx + 10, cy + 50);
            ArcTo swArc = new ArcTo();
            swArc.setX(cx - 50);
            swArc.setY(cy - 10);
            swArc.setRadiusX(60);
            swArc.setRadiusY(60);
            swArc.setSweepFlag(false);
            swArc.setLargeArcFlag(false);
            LineTo swLine2 = new LineTo(cx - 500, cy - 10);
            southToWestPath.getElements().setAll(swMove, swLine, swArc, swLine2);
            southToWestPath.setStroke(Color.GRAY);
            southToWestPath.setStrokeWidth(2);
            southToWestPath.setFill(null);
            roadPane.getChildren().add(southToWestPath);



            directionPaths.put("NORTH_SOUTH", northToSouthPathNew);
            directionPaths.put("SOUTH_NORTH", southToNorthPathNew);
            directionPaths.put("EAST_WEST", eastToWestPathNew);
            directionPaths.put("WEST_EAST", westToEastPathNew);

            directionPaths.put("NORTH_EAST", northToEastPath);
            directionPaths.put("SOUTH_WEST", southToWestPath);
            directionPaths.put("EAST_SOUTH", aeastToSouthPath);
            directionPaths.put("WEST_NORTH", westToNorthPath);

            directionPaths.put("WEST_SOUTH", westToSouthPath);
            directionPaths.put("EAST_NORTH", eastRightNewPath);
            directionPaths.put("SOUTH_EAST", southRightNewPath);
            directionPaths.put("NORTH_WEST", northRightNewPath);
        };

        roadPane.widthProperty().addListener(routeUpdater);
        roadPane.heightProperty().addListener(routeUpdater);

        // CSS ve yol düzen hesaplamalarının uygulanması
        Platform.runLater(() -> {
            roadPane.applyCss();
            roadPane.layout();
            routeUpdater.changed(null, null, null);
        });

        // Geri sayım etiketlerinin görünümü
        northTimer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        southTimer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        eastTimer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        westTimer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
    }

    /**
     * Simülasyonu sıfırlar.
     * Tüm verileri, araçları, zamanlayıcıları temizler ve simülasyonu başlangıç durumuna getirir.
     */
    @FXML
    public void onResetClicked() {

        northInput.clear();
        southInput.clear();
        eastInput.clear();
        westInput.clear();


        if (timeline != null) {
            timeline.stop();
        }
        for (Timer timer : activeTimers) {
            timer.cancel();
        }
        activeTimers.clear();

        for (PathTransition pt : activeTransitions) {
            pt.stop();
        }
        activeTransitions.clear();


        carsCreatedCount.clear();
        carsPassedCount.clear();


        model.clear();


        roadPane.getChildren().removeIf(node ->
            node.getStyleClass().contains("car") ||
            (node instanceof Path && !((Path) node).getStroke().equals(Color.WHITE) && !((Path) node).getStroke().equals(Color.GRAY))
        );


        updateLightsToRed();
        northTimer.setText("");
        southTimer.setText("");
        eastTimer.setText("");
        westTimer.setText("");


        phase = LightPhase.RED;
        remainingSeconds = 0;
        currentIndex = 0;
        activeDirection = null;
        initial = true;

        System.out.println("Reset complete.");
    }


    /**
     * Simülasyon süresini görsel olarak göstermek için geri sayım etiketlerini günceller.
     */
    private void startCountdownUpdater() {
        Timeline countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Platform.runLater(() -> updateCountdownLabel(activeDirection, remainingSeconds));
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }


    private double getStartX(String dir) {
        double cx = roadPane.getWidth() / 2;
        return switch (dir) {
            case "NORTH" -> cx - 14;
            case "SOUTH" -> cx + 10;
            case "EAST" -> roadPane.getWidth() + 100;
            case "WEST" -> -100;
            default -> cx;
        };
    }

    private double getStartY(String dir) {
        double cy = roadPane.getHeight() / 2;
        return switch (dir) {
            case "NORTH" -> -100;
            case "SOUTH" -> roadPane.getHeight() + 100;
            case "EAST" -> cy - 14;
            case "WEST" -> cy + 10;
            default -> cy;
        };
    }
}