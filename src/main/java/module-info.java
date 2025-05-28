module com.alperensanli.traffic_light_controller_system_demo_v1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.alperensanli.traffic_light_controller_system_demo_v1 to javafx.fxml;
    exports com.alperensanli.traffic_light_controller_system_demo_v1;
}