package com.alperensanli.traffic_light_controller_system_demo_v1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/alperensanli/traffic_light_controller_system_demo_v1/IntersectionView.fxml"));
        Scene scene = new Scene(fxmlLoader. load(), 1100, 1100);
        stage.setScene(scene);
        stage.setTitle("Traffic Light System");
        stage.show();


    }
    public static void main(String[] args) {launch();}
}
