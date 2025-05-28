package com.alperensanli.traffic_light_controller_system_demo_v1;

import javafx.animation.PathTransition;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;



/**
 * Trafik simülasyonunda bir aracı temsil eden sınıftır.
 * Giriş ve çıkış yönleri, görsel temsili, hareket durumu ve animasyon özelliklerini içerir.
 */
public class Car {
    private String id;
    private Direction entry;
    private Direction exit;
    private Rectangle shape;
    private boolean moved = false;

    /**
     * Belirtilen ID, giriş ve çıkış yönleriyle araç oluşturur.
     */
    public Car(String id, Direction entry, Direction exit) {
        this.id = id;
        this.entry = entry;
        this.exit = exit;
        this.shape = createColoredRectangle();
    }

    /**
     * Sadece giriş yönü verilen aracı oluşturur, çıkışı rastgele belirlenir.
     */
    public Car(Direction entry) {
        this("CAR-" + System.currentTimeMillis(), entry, generateRandomExit(entry));
    }


    /**
     * Aracın kavşağı geçip geçmediğini kontrol eder.
     */
    public boolean hasMoved() {
        return moved;
    }

    /**
     * Aracın kavşağı geçtiğini işaretler.
     */
    public void markMoved() {
        this.moved = true;
    }


    private boolean moving = false;

    /**
     * Aracın şu anda hareket halinde olup olmadığını belirten bayrak.
     */
    public boolean isMoving() {
        return moving;
    }

    /**
     * Aracın şu anda hareket halinde olup olmadığını belirten bayrak.
     */
    public void setMoving(boolean moving) {
        this.moving = moving;
    }
    /**
     * Verilen giriş yönü dışındaki yönlerden rastgele bir çıkış yönü seçer.
     */
    private static Direction generateRandomExit(Direction entry) {
        List<Direction> possibleExits = new ArrayList<>(Arrays.asList(Direction.values()));
        possibleExits.remove(entry);
        Collections.shuffle(possibleExits);
        return possibleExits.get(0);
    }

    /**
     * Aracın görsel temsilini oluşturan renkli dikdörtgeni oluşturur.
     */
    private Rectangle createColoredRectangle() {
        Rectangle rect = new Rectangle(18, 9);
        rect.setFill(getRandomColor());
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        rect.getStyleClass().add("car");
        return rect;
    }

    /**
     * Rastgele bir renk döndürür.
     */
    private Color getRandomColor() {
        Random rand = new Random();
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    /**
     * Aracın JavaFX üzerindeki dikdörtgen şeklini döndürür.
     */
    public Rectangle getShape() {
        return shape;
    }


    /**
     * Aracın çıkış yönünü döndürür.
     */
    public Direction getExit() {
        return exit;
    }


    /**
     * Aracı belirtilen yol boyunca verilen süre ile hareket ettirir.
     * Hareket sonunda verilen işlev çalıştırılır.
     */
    public void moveFromCurrentPosition(Path fullPath, Duration duration, Runnable onFinished) {
        double x = shape.getTranslateX();
        double y = shape.getTranslateY();

        ObservableList<PathElement> original = fullPath.getElements();
        Path pathWithMove = new Path();
        pathWithMove.getElements().add(new MoveTo(x, y));

        for (PathElement element : original) {
            if (!(element instanceof MoveTo)) {
                pathWithMove.getElements().add(element);
            }
        }

        PathTransition transition = new PathTransition();
        transition.setNode(shape);
        transition.setPath(pathWithMove);
        transition.setDuration(duration);
        transition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        transition.setOnFinished(e -> onFinished.run());
        transition.play();
    }
}
