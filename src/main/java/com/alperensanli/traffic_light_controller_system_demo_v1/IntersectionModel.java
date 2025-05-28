package com.alperensanli.traffic_light_controller_system_demo_v1;

import java.util.*;

/**
 * Trafik simülasyonundaki veri modelini yöneten sınıftır.
 * Araç sayıları, yeşil ışık süreleri ve araç kuyrukları gibi bilgileri saklar ve hesaplar.
 */
public class IntersectionModel {
    private final Map<Direction, Integer> vehicleCounts;
    private final Map<Direction, Integer> greenDurations;
    private final Map<Direction, java.util.Queue<Car>> carQueues;


    public IntersectionModel() {
        vehicleCounts = new HashMap<>();
        greenDurations = new HashMap<>();
        carQueues = new HashMap<>();
    }

    /**
     * Belirtilen yöndeki araç sayısını ayarlar.
     */
    public void setVehicleCount(String direction, int count) {
        vehicleCounts.put(Direction.valueOf(direction.toUpperCase(Locale.ROOT)), count);
    }

    /**
     * Tüm yönlerdeki araç sayılarına göre yeşil ışık sürelerini hesaplar.
     * Süreler 10 ile 60 saniye arasında sınırlıdır.
     */
    public void calculateGreenDurations() {
        int totalVehicles = vehicleCounts.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<Direction, Integer> entry : vehicleCounts.entrySet()) {
            Direction direction = entry.getKey();
            int count = entry.getValue();
            int duration = (int) ((count / (double) totalVehicles) * 120);
            greenDurations.put(direction, Math.max(10, Math.min(60, duration)));
        }
    }

    /**
     * Hesaplanmış yeşil ışık sürelerini döndürür.
     */
    public Map<Direction, Integer> getGreenDurations() {
        return greenDurations;
    }

    /**
     * Araç yoğunluğuna göre sıralanmış yön listesini döndürür.
     */
    public List<Direction> getDirectionsSortedByDensity() {
        return vehicleCounts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Her yön için boş araç kuyrukları başlatır.
     */
    public void prepareCarQueues() {
        carQueues.clear();
        for (Direction direction : vehicleCounts.keySet()) {
            carQueues.put(direction, new java.util.LinkedList<>());
        }
    }

    /**
     * Belirtilen yöne yeni bir araç oluşturur ve kuyruğa ekler.
     */
    public void addCarToQueue(String direction) {
        try {
            Direction dir = Direction.valueOf(direction.toUpperCase(Locale.ROOT));
            Car car = new Car(dir);
            carQueues.get(dir).add(car);
        } catch (IllegalArgumentException e) {
            System.err.println("‼ Hatalı yön adı: " + direction);
        }
    }

    /**
     * Belirtilen yönün araç kuyruğunu döndürür.
     */
    public Queue<Car> getCarQueue(Direction direction) {
        return carQueues.get(direction);
    }

    /**
     * Belirtilen yöndeki araç sayısını döndürür.
     */
    public int getVehicleCount(Direction direction) {
        return vehicleCounts.getOrDefault(direction, 0);
    }


    /**
     * Tüm verileri temizler ve yön başına boş kuyrukları yeniden başlatır.
     */
    public void clear() {
        vehicleCounts.clear();
        greenDurations.clear();
        carQueues.clear();


        for (Direction d : Direction.values()) {
            carQueues.put(d, new LinkedList<>());
        }
    }
}