# Chẩn đoán System UI / máy ảo — 17/10/2026

Cập nhật sau khi người dùng xác nhận chạy Pixel 8: đã đọc riêng `D:\Android_AVD\Pixel_8.avd\config.ini`. Pixel 8 dùng **4096 MB RAM**, Google Play API 34, 1080×2400, 2 cores; khác Pixel 6 2048 MB mô tả bên dưới. Đây là cấu hình đáng giảm tải trên máy tính 8 GB. Xem hướng dẫn riêng [ANDROID_STUDIO_PIXEL8_SETUP.md](ANDROID_STUDIO_PIXEL8_SETUP.md) để xóa đúng Pixel 6 và kiểm tra Pixel 8/JDK/Gradle.

## Kết luận theo bằng chứng hiện có

Nguyên nhân nổi bật là **máy ảo thiếu tài nguyên xử lý trong lúc khởi động/build**, có thêm chi phí đồ họa phần mềm. Chưa có bằng chứng đủ để quy mọi ANR cho mã Java của GalleryApp, cũng chưa thể khẳng định app hoàn toàn không có vấn đề hiệu năng.

Hôm kiểm tra lại không có emulator đang kết nối. Phân tích ANR bên dưới dùng log đã thu ngày 16/10, không phải một lần tái hiện mới ngày 17/10.

## Máy tính và máy ảo

Thông tin máy tính đọc ngày 17/10:

- RAM dùng được khoảng 7,86 GiB, trống khoảng 2,43 GiB lúc chưa mở Studio/emulator.
- CPU Intel Core i5-7200U: 2 nhân vật lý / 4 luồng.
- GPU Intel HD Graphics 620.
- Ổ C còn khoảng 5,35 GiB; ổ D khoảng 30,90 GiB. Ổ C khá chật, nhưng chưa có bằng chứng nó trực tiếp gây lần ANR này.
- Pagefile đã dùng đỉnh khoảng 4.260 MB trong phiên Windows hiện tại; đây là dấu hiệu bổ sung, không chứng minh riêng lần treo app.
- `emulator -accel-check`: WHPX installed and usable. Không phải trường hợp chưa bật tăng tốc CPU máy ảo.

AVD Pixel_6 hiện tại: Android 14 Google APIs x86_64, RAM cấu hình 2048 MB, 2 vCPU, màn hình 1080×2400, Graphics `auto`, camera sau `virtualscene`, Quick Boot bật. Log một lần chạy trước cho thấy emulator tự tăng RAM lên 2560 MB và chọn SwiftShader.

## Log ANR chỉ ra điều gì?

`build/diagnostics/app-anr-traces.txt`:

- App bị timeout FocusEvent sau 5.782 ms tại SplashActivity.
- CPU toàn máy ảo: **98%**; system_server chiếm khoảng **62%**, app khoảng **14%** trong cửa sổ lấy mẫu. Đây không phải tỷ lệ tài nguyên của Windows.
- CPU pressure `some avg10=80.98`, `avg60=90.07`; có cả memory/I/O pressure.
- Stack main thread tại lúc thu trace ở `MessageQueue.nativePollOnce` / Looper. Stack này không chỉ ra dòng Java nào đang khóa UI; cũng không tự chứng minh app vô can vì trace có thể được lấy muộn.
- Những lần kiểm tra trước còn ghi nhận ANR ở System UI và nhiều dịch vụ Google, cùng kswapd dùng CPU cao.

`build/diagnostics/ui-emulator.log` ghi Graphics Adapter **Google SwiftShader**, trong khi WHPX vẫn hoạt động. Tăng tốc CPU và tăng tốc đồ họa là hai phần khác nhau: có WHPX không có nghĩa đồ họa đang dùng GPU Intel.

## Kiểm tra mã project

- `GalleryApplication`: làm nóng Firebase Auth trên thread `authentication-startup`.
- `BaseActivity`: không tạo GalleryRepository trên màn chào/login/register/OTP/Google.
- `GalleryRepository`: đọc/ghi cache bằng executor; Firestore dùng callback bất đồng bộ, không có `Tasks.await()` hay chờ Future trên main thread trong luồng này.
- `GalleryActivity`: sao chép file nhập trên executor.
- `EditPhotoActivity`: nén/lưu bản sao trên thread riêng. Một số thao tác bitmap như copy/xoay/cắt vẫn ở main thread; có thể gây khựng khi chỉnh ảnh, nhưng không giải thích System UI treo ngay tại màn chào.

Không sửa Firebase rules, targetSdk hoặc bố cục để che hộp thoại ANR: chưa có bằng chứng các phần đó gây lỗi đang xét.

## Thay đổi trong project

`gradle.properties`: heap Gradle từ 2048 xuống 1536 MB, tối đa 2 worker, tắt parallel build. Mục tiêu là giảm tranh chấp RAM/CPU khi build đồng thời chạy emulator. Heap là giới hạn tối đa, không phải toàn bộ RAM tiến trình Gradle; thiết lập này cũng không bảo đảm chữa hết ANR.

Đã kiểm tra `:app:assembleDebug` với cấu hình mới: **BUILD SUCCESSFUL**, 32 giây, các task biên dịch đang up-to-date. Điều này xác nhận Gradle nhận cấu hình và project vẫn assemble được; chưa phải phép đo full clean build hay kiểm tra hết treo trên máy ảo.

Không sửa cấu hình AVD, không xóa snapshot/dữ liệu và không bật/tắt tính năng Windows trong lần chẩn đoán này.

## Các bước nên làm theo thứ tự

1. **Chỉ mở một emulator.** Tránh khởi động emulator cùng lúc Gradle đang Sync/Build lần đầu. Chờ máy ảo vào màn hình chính và phản hồi bình thường rồi Run app.
2. Device Manager → sửa Pixel_6 → Additional/Advanced settings → Graphics: thử **Hardware / GPU máy tính**. Log trước cho thấy Auto chọn SwiftShader. Nếu Hardware xuất hiện lỗi hình hoặc crash, chuyển lại Auto/Software; GPU/driver cũ có thể không tương thích.
3. Để giảm tải vẽ, thử một AVD riêng độ phân giải khoảng **720×1280** với mật độ phù hợp (chẳng hạn 320 dpi), **2 cores**. Không chỉ thu nhỏ cửa sổ emulator vì việc đó không giảm độ phân giải Android đang render.
4. Với máy 8 GB RAM, không tăng RAM AVD lên 4–6 GB một cách máy móc. Giữ quanh 2–2,5 GB theo mức image chấp nhận, đóng ứng dụng nặng; nếu Android vẫn thiếu RAM thì ưu tiên điện thoại thật thay vì tiếp tục ép host dùng swap.
5. Device Manager → menu AVD → **Cold Boot Now** để kiểm tra khả năng snapshot cũ có vấn đề. Cold Boot không tương đương Wipe Data. Lần khởi động lạnh có thể chậm hơn; chờ ổn định trước khi chạy app.
6. Giải phóng thêm dung lượng ổ C bằng cách tự chọn dữ liệu không cần thiết; giữ pagefile Windows ở chế độ phù hợp. Không xóa thư mục project/ảnh của app để xử lý vấn đề này.
7. Nếu màn hình chính Android hoặc Settings cũng treo khi GalleryApp chưa mở: vấn đề môi trường đã tái hiện độc lập với app. Nếu điện thoại thật chỉ treo trong GalleryApp, thu log mới để tìm lỗi app riêng.

## Tài liệu đối chiếu

- https://developer.android.com/studio/run/emulator-acceleration
- https://developer.android.com/studio/run/emulator-troubleshooting
- https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs

Không coi build thành công hay test gọi trực tiếp callback UI là bằng chứng emulator đã hết treo. Cần một lần kiểm tra chạy/chạm thực tế trong môi trường ổn định.
