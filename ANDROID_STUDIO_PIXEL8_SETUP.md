# Cài đặt Android Studio / Pixel 8 cho GalleryApp

Kiểm tra trực tiếp ngày 17/10/2026. Chưa thay đổi hoặc xóa AVD nào.

## 1. Những gì project hiện dùng

| Thành phần | Giá trị đã đọc |
|---|---|
| Máy tính | RAM khoảng 8 GB; i5-7200U, 2 nhân / 4 luồng; Intel HD 620 |
| Pixel 8 | Android 14 / API 34, x86_64, **Google Play** |
| RAM Pixel 8 | **4096 MB** |
| CPU Pixel 8 | 2 cores |
| Màn hình Pixel 8 | 1080×2400, density 420 |
| Graphics | Auto; log những lần trước có SwiftShader |
| Gradle wrapper | 9.5.0 |
| Android Gradle Plugin | 9.3.3 |
| Gradle heap / workers | 1536 MB / 2 |
| Gradle daemon JVM criteria | Java 25 (`gradle/gradle-daemon-jvm.properties`) |
| Android Studio project JDK | jbr-25 trong `.idea/misc.xml` |
| JDK kèm Android Studio | 25.0.2, `C:\Program Files\Android\Android Studio\jbr` |
| JAVA_HOME của terminal | `C:\Users\Admin\Downloads\jdk-26.0.2` |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |

RAM máy ảo 4 GB cộng Studio, Windows và tiến trình build gây áp lực lớn trên máy 8 GB. Đây là yếu tố đáng xử lý trước; không phải kết luận mọi lỗi app đều do RAM.

**Pixel 6 chỉ được lưu trong Device Manager nhưng không chạy thì không tự dùng RAM/CPU.** Xóa nó giúp giảm dữ liệu trên đĩa; không thay thế việc giảm tải Pixel 8.

## 2. Xóa Pixel 6 bằng giao diện

Xóa AVD sẽ xóa ứng dụng, ảnh và dữ liệu cục bộ **trong Pixel 6**. Sao chép ảnh chưa sao lưu ra ngoài trước nếu cần giữ. Thao tác đúng dòng Pixel 6 không xóa project GalleryApp, Firebase hay dữ liệu của Pixel 8.

1. Mở Android Studio và chờ tác vụ build/sync đang chạy hoàn tất.
2. Chọn **Tools → Device Manager**. Nếu menu khác ở phiên bản đang dùng, chọn biểu tượng Device Manager bên phải rồi mở danh sách thiết bị ảo.
3. Xác định đúng dòng **Pixel 6 / Pixel_6**, không chọn Pixel 8.
4. Bấm dấu **⋮** ở dòng Pixel 6 → **Stop** nếu máy ảo đang chạy. Chờ nó dừng hẳn. Chỉ đóng tab Running Devices chưa chắc đã dừng tiến trình emulator.
5. Bấm **⋮ → Delete** ở dòng Pixel 6 → đọc tên thiết bị trong hộp thoại → xác nhận xóa.
6. Kiểm tra danh sách: Pixel 8 vẫn còn, Pixel 6 đã mất.
7. Ở thanh chọn thiết bị cạnh nút Run của project, chọn **Pixel 8**.

Không cần xóa Android SDK/API 34 trong SDK Manager. Không dùng Wipe Data trên Pixel 8 để xóa Pixel 6.

Nếu Delete báo thiết bị đang dùng: dừng emulator, đóng Android Studio, mở lại và thử Delete. Vị trí đã xác minh của Pixel 6 là `D:\Android_AVD\Pixel_6.avd` và `D:\Android_AVD\Pixel_6.ini`; ưu tiên Device Manager thay vì xóa thư mục bằng tay. Không xóa cả `D:\Android_AVD` vì Pixel 8 cũng nằm trong đó.

## 3. Chỉnh Pixel 8 trước khi chạy

1. Device Manager → **Pixel 8 → ⋮ → Stop**. Chỉnh khi thiết bị đã dừng.
2. Chọn biểu tượng bút chì hoặc **⋮ → Edit**, mở **Show Advanced Settings / Additional settings** nếu có.
3. Giữ system image **Android 14 / API 34 / x86_64** hiện tại. Không cần cài máy ảo API 37 chỉ vì project compileSdk 37.
4. RAM: thử **2048 MB** thay cho 4096 MB. Nếu emulator tự điều chỉnh lên **2560 MB**, không ép xuống thấp hơn mức image yêu cầu. Không tăng lên 4–6 GB trên máy tính này.
5. CPU: giữ **2 cores**.
6. Graphics: thử **Hardware / Host** nếu có tùy chọn. Log trước từng chọn SwiftShader, khiến CPU phải gánh thêm đồ họa. Nếu xuất hiện lỗi hình hoặc crash, chuyển lại Auto/Software; Hardware không bảo đảm tương thích với mọi driver.
7. Nếu có tùy chọn camera, có thể chọn None khi chưa kiểm tra chụp camera. App hiện nhập ảnh qua trình chọn file, không cần dựng cảnh camera ảo.
8. Finish/Save. Lần đầu sau thay đổi → **⋮ → Cold Boot Now**. Cold Boot không xóa dữ liệu như Wipe Data; lần mở này có thể chậm, hãy chờ Android vào màn hình chính ổn định.

### Nếu ô RAM/Graphics bị khóa trên image Google Play

Không phải lỗi Gradle. Một số tùy chọn phụ thuộc loại image/hardware profile. Cách dễ kiểm soát là tạo **AVD riêng** bằng image **Google APIs x86_64 / API 34**, thay vì xóa Pixel 8 hiện tại. Có thể đặt tên `Gallery_Light` để phân biệt. AVD mới có dữ liệu riêng, không tự chứa ảnh của Pixel 8.

Nếu cần giảm độ phân giải xuống 720×1280, tạo/copy một hardware profile riêng trong bước chọn thiết bị → Edit/Create Hardware Profile → đặt Resolution 720×1280, mật độ khoảng 320 dpi nếu có. Thu nhỏ cửa sổ hiển thị emulator không làm giảm số pixel Android phải render. Không đổi tên một profile mặc định rồi cho rằng độ phân giải đã đổi.

Nếu vẫn dùng Pixel 8 mặc định 1080×2400, ưu tiên giảm RAM vừa phải, dùng đồ họa GPU khi ổn định, chỉ mở một máy ảo và tránh chạy build nặng đồng thời lúc Android khởi động.

## 4. Gradle và JDK trong Android Studio

### Gradle JDK

1. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**.
2. Nếu có **Gradle JDK**, chọn **jbr-25 / Embedded JDK** ở `C:\Program Files\Android\Android Studio\jbr`.
3. Nếu phiên bản Studio hiện **Gradle Daemon JVM criteria** thay cho lựa chọn trên, giữ **Java 25**, đồng nhất với `gradle/gradle-daemon-jvm.properties` của project.
4. Nếu có Gradle distribution, chọn **Wrapper / gradle-wrapper.properties**, không tự thay bằng bản Gradle khác cài ngoài.
5. Apply → OK → Sync Project with Gradle Files.

Project đã có daemon criteria Java 25. Theo Gradle, criteria có ưu tiên hơn JAVA_HOME và org.gradle.java.home. Do đó JAVA_HOME=26 không tự chứng minh build đang chạy daemon 26 và cũng không chứng minh đây là nguyên nhân System UI treo. Giữ IDE/terminal đồng nhất giúp giảm nhầm lẫn và hạn chế những daemon không tương thích dùng thêm RAM.

Trong terminal PowerShell của project, nếu muốn dùng cùng JDK trong phiên terminal hiện tại:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --version
```

Lệnh trên không đổi biến môi trường toàn Windows. Không cần cài JDK 11 chỉ vì Java sourceCompatibility trong app là VERSION_11: đó là mức mã nguồn/bytecode, khác JDK chạy Gradle.

### gradle.properties

Project đang có sẵn, không cần thêm các dòng trùng lặp:

```properties
org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.parallel=false
org.gradle.configuration-cache=true
```

Không tăng Xmx lên 4096m/8192m trên máy 8 GB để thử chữa treo. Không cần thêm Kotlin daemon flags vì mã ứng dụng đang là Java. Heap 1536 MB là giới hạn heap Gradle, không phải tổng RAM của toàn bộ Studio/build.

### Bộ nhớ Android Studio

Tìm **Memory Settings** trong File → Settings hoặc Help → Change Memory Settings (tên/vị trí có thể khác theo phiên bản). Với project này và máy 8 GB, có thể bắt đầu IDE heap khoảng **1536 MB**; nếu đang 2048 MB và Studio vẫn ổn thì không cần tăng thêm. Không cấp riêng cả IDE lẫn Gradle mỗi bên 4 GB. Khởi động lại IDE nếu đổi IDE heap.

## 5. SDK của project không bị lệch với Pixel 8

- `minSdk=24`: Pixel 8 API 34 đáp ứng điều kiện chạy.
- `compileSdk=37`: SDK dùng để biên dịch, không phải yêu cầu thiết bị phải Android 37.
- `targetSdk=37`: mức hành vi app nhắm tới; không phải minSdk.
- AGP/Gradle hiện tại đã build được ở lần kiểm tra trước. Không đổi phiên bản thư viện/AGP/SDK hàng loạt để chữa System UI của máy ảo khi chưa có lỗi build tương ứng.

## 6. Thứ tự chạy để hạn chế tranh tài nguyên

1. Đóng các ứng dụng nặng không cần thiết.
2. Mở project → để Sync xong → **Build → Make/Assemble Project** khi emulator chưa chạy nếu máy dễ quá tải.
3. Mở duy nhất Pixel 8 → chờ màn hình chính phản hồi tốt, thử mở Settings.
4. Chọn cấu hình Run `app`, thiết bị Pixel 8 → Run.
5. Nếu Settings/màn hình chính cũng báo System UI/Digital Wellbeing không phản hồi trước khi mở GalleryApp thì ghi nhận lỗi môi trường riêng. Không tạo lại Firebase collections để xử lý triệu chứng này.

Các thiết lập này giảm tải, không phải cam kết tuyệt đối hết treo. Nếu vẫn thiếu tài nguyên, chạy trên điện thoại thật sẽ tách tải hệ điều hành Android khỏi máy tính 8 GB.

## Nguồn chính thức

- https://developer.android.com/studio/run/managing-avds
- https://developer.android.com/studio/intro/studio-config
- https://developer.android.com/build/jdks
- https://docs.gradle.org/current/userguide/gradle_daemon.html
