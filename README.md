# 🚦 Araç Yoğunluğuna Dayalı Trafik Işığı Kontrol Sistemi

Bu proje, gerçek zamanlı araç yoğunluğuna göre yeşil ışık sürelerini ayarlayan akıllı bir trafik ışığı sistemini simüle eder. Dört yönlü bir kavşakta (Kuzey, Güney, Doğu, Batı) araç hareketi, animasyonlu geçişler ve dinamik trafik sinyali geçişleri içerir.

---

## 🧱 Proje Yapısı

Proje, Java ve JavaFX kullanılarak **Model-View-Controller (MVC)** mimarisi ile geliştirilmiştir.

### 📁 Ana Bileşenler:

- `Main.java`: JavaFX uygulamasını başlatır ve FXML arayüzünü yükler.
- `IntersectionView.fxml`: Grafik kullanıcı arayüzünü tanımlar; giriş panelleri, ışık göstergeleri ve kontrol butonlarını içerir.
- `IntersectionController.java`: Trafik ışıklarının fazlarını, araç animasyonlarını ve arayüz güncellemelerini kontrol eder.
- `IntersectionModel.java`: Araç sayılarını saklar ve yeşil ışık sürelerini hesaplar.
- `Car.java`: Bireysel araçları temsil eder ve animasyonlarını yönetir.
- `Direction.java`: Dört yönü tanımlayan enum: `NORTH`, `SOUTH`, `EAST`, `WEST`.
- `LightPhase.java`: Trafik ışığı fazlarını tanımlar: `GREEN`, `YELLOW_BEFORE_GREEN`, `RED`, `YELLOW_AFTER_GREEN`.

---

## 🎯 Özellikler

- Dört yönlü kavşakta araçların geçişi simüle edilir.
- Araç yoğunluğu:
  - Kullanıcı tarafından manuel girilebilir.
  - Rastgele oluşturulabilir.
- Araç yoğunluğuna göre dinamik yeşil ışık süreleri hesaplanır.
- Gerçek zamanlı geri sayım içeren animasyonlu trafik ışıkları.
- Araçlar:
  - Kırmızı ışıkta durur.
  - Yeşil ışıkta geçer.
  - Kuyruğa girer ve çarpışmadan hareket eder.
  - Kavşaktan geçtikten sonra sahneden kaldırılır.

---

## ⏱ Sinyal Zamanlama Mantığı

- **Toplam Döngü Süresi:** 120 saniye (sabit)
- **Sarı Işık Süresi:** 3 saniye (sabit)
- **Yeşil Işık Aralığı:** 10 ila 60 saniye (araç yoğunluğuna göre)

### 🧮 Hesaplama Örneği:

| Yön    | Araç Sayısı | Yeşil Işık Süresi |
|--------|-------------|-------------------|
| Kuzey  | 40          | 60 saniye         |
| Güney  | 20          | 30 saniye         |
| Doğu   | 10          | 15 saniye         |
| Batı   | 10          | 15 saniye         |

---

## 🖥 Arayüz Özeti

- Merkezi kavşak görünümü, animasyonlu araçlar.
- Her yön için araç sayısı girişi.
- Başlat, Duraklat, Sıfırla, Rastgele butonları.
- Her trafik ışığı için geri sayım göstergesi.
- Işıklar ve araç yolları yön ve renge göre senkronize çalışır.

---

## 🛠 Kullanılan Teknolojiler

- Java SE
- JavaFX (arayüz ve animasyonlar)
- Java Collections (Queue, Map, List)
- MVC yazılım mimarisi

---

## 📦 Nasıl Çalıştırılır?

1. Proje dosyalarını klonlayın veya çıkarın.
2. IntelliJ IDEA ile açın.
3. JavaFX kütüphanesini doğru şekilde bağlayın.
4. `Main.java` dosyasını çalıştırın.

---

## 📋 Notlar

- Üçüncü parti kütüphane kullanılmamıştır.
- Tüm trafik mantığı JavaFX zamanlayıcılarıyla kontrol edilir.
- Simülasyon döngüsü boyunca toplam araç sayısı sabittir; yeni araç eklenmez.

---

## 📁 Teslimat İçeriği

- `.java` kaynak dosyaları
- `IntersectionView.fxml`
- Bu `README.md`
  

---

## 🙏 Teşekkür

Bu proje BZ214 Görsel Programlama dersi kapsamında geliştirilmiştir.  
Katkılarından dolayı Gökhan Azizoğlu’na teşekkür ederiz.
