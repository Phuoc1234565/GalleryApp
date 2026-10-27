# Kết quả kiểm tra

- `:app:assembleDebug :app:lintDebug`: **BUILD SUCCESSFUL** sau các thay đổi cuối.
- Tất cả XML trong `app/src/main/res` parse hợp lệ.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Lint không có lỗi chặn build; vẫn còn cảnh báo, chủ yếu chuỗi hardcode, tài nguyên và API cũ.
- Đã thêm `GalleryFlowTest` để kiểm tra inflate các layout, lưu ảnh cục bộ, tạo album và yêu thích.
- Đã chạy trực tiếp `GalleryFlowTest` bằng AndroidJUnitRunner: **OK (3 tests)**, 74.491 giây. Kiểm tra inflate các layout, form/OTP, cache ảnh, tạo album và yêu thích. Đây là kiểm tra logic/lifecycle; các thao tác form trong test được gọi trên UI thread, không thay thế kiểm tra chạm trên máy thật.
- Trình Gradle connected-test trước đó thất bại do emulator bị ngắt kết nối; kết quả trực tiếp ở trên được lấy từ lần chạy riêng sau đó.
- Chưa kiểm thử đăng nhập Google, SMS, email và sao lưu với Firebase thật. Chủ project cần bật provider, thêm SHA, tạo database/bucket và Publish rules theo `FIREBASE_SETUP.md`.
- Đã xem ảnh chụp màn chào và đăng nhập sau khi thiết kế lại. Ảnh chụp lần đầu bị che bởi hộp thoại **System UI isn't responding** của máy ảo. Không dùng các ảnh đó làm bằng chứng UI đã được xác minh hoàn toàn.

## Thay đổi theo phản hồi giao diện

- Thay nền splash xanh đặc bằng nền sáng, logo hoa tám cánh, minh họa vector nhiều lớp và nút rõ chữ. Thay cả biểu tượng Android mặc định của launcher/native splash.
- Thiết kế lại đăng nhập/đăng ký, sáu ô OTP, thư viện và tìm kiếm, menu danh sách, album, cài đặt, chi tiết ảnh, editor và sao lưu.
- Chữ nội dung chủ yếu 15–16sp, tiêu đề 22–30sp, xanh đậm trên nền sáng; ảnh chi tiết/editor dùng nền tối và thanh thao tác ngang.
- Loại bỏ khởi tạo repository ở màn chào/auth; làm nóng Firebase Auth bằng luồng riêng; bỏ Analytics không sử dụng.
- Thống nhất edge-to-edge và insets để tránh cộng khoảng cách thanh hệ thống hai lần trên Android 14; kiểm tra build/lint thành công sau thay đổi này.

## Dữ liệu chẩn đoán ANR

Log máy ảo người dùng ghi nhận timeout FocusEvent tại SplashActivity. Tại thời điểm lấy stack, main thread đang ở `MessageQueue.nativePollOnce`; không có stack Java của app đang khóa/chạy một vòng lặp dài được ghi nhận. Máy ảo có load khoảng 33–37, kswapd dùng CPU cao, nhiều dịch vụ Google và System UI cùng ANR. Bằng chứng này cho thấy áp lực hệ thống đáng kể, nhưng không đủ để khẳng định mọi nguyên nhân ANR đã được giải quyết.

Nếu còn **Process system / System UI isn't responding**, hãy chỉ mở một emulator, chờ khởi động xong trước khi Run, thử Cold Boot trong Device Manager hoặc kiểm tra trên điện thoại thật. Không xóa dữ liệu/gỡ app khi còn ảnh chưa sao lưu. Chỉ điều chỉnh RAM emulator trong khả năng RAM của máy tính; RAM quá thấp hoặc mở nhiều emulator đều có thể làm máy ảo thiếu tài nguyên.

Build thành công không thay thế việc kiểm thử các luồng trên máy thật. Các giới hạn chức năng được ghi trong README.
# Kiểm tra bổ sung: tìm kiếm, điều hướng và hồ sơ — 17/10/2026

## Giao diện Sáng/Tối toàn app và bốn album mẫu

- ThemeManager lưu một lựa chọn chung, khởi tạo trong Application. Bỏ localNightMode riêng từng Activity để các màn đã mở trong back stack cùng cập nhật. Theme.Fullscreen của màn xem ảnh kế thừa bộ màu chung; xem ảnh/chỉnh sửa, ô nhập, thanh điều hướng, menu và dialog dùng màu theo chế độ.
- Bộ màu semantic ở values/theme_colors.xml và values-night/colors.xml; theme_resources.py áp dụng sau khi tái tạo layout để tránh hồi quy. Màu chữ chính/phụ có kiểm tra tương phản ít nhất 4.5:1 trên nền tương ứng.
- PrepareDemoAlbumsTest: OK (1 test), 1.723 giây. Log xác nhận máy chủ Firestore đã nhận bốn album và metadata thành viên. Ảnh gốc/video vẫn cục bộ đến khi chạy sao lưu.
- ThemeSwitchTest và AlbumDemoUiTest chạy chung: OK (2 tests), 40.718 giây, trước tinh chỉnh kích thước thẻ/xếp album. Kiểm tra 29 layout ở cả hai mode, dialog/bottom sheet, Activity mở trước khi đổi mode, giữ từ khóa và bộ lọc; bốn album có thành viên và menu mở được.
- Giữ album có sẵn của người dùng. Các album demo Gia đình/Du lịch/Đồ ăn/Video dùng ID ổn định và chuẩn bị lại không tạo bản trùng.
- Bản cuối sau tinh chỉnh: build/lint thành công; PrepareDemoAlbumsTest OK (1 test), 1.691 giây; ThemeSwitchTest + AlbumDemoUiTest OK (2 tests), 39.574 giây. Test bìa yêu cầu bốn thumbnail đầu đã giải mã thành BitmapDrawable. Bốn album mới xếp đầu bằng thời gian tạo, thẻ nhỏ hơn để thấy trọn tên/số lượng; đã xem ảnh chụp thực tế sáng/tối, nút + hiển thị rõ.

## Bố cục Cài đặt và bộ media demo

- Cài đặt dùng các dòng icon/tiêu đề/mô tả/mũi tên, nhóm Khác và Đăng xuất màu đỏ. Dung lượng/số mục sao lưu lấy từ thư viện hiện tại; không hiển thị hạn mức giả 15 GB.
- Đã tải và xác minh 10 JPEG 960×720, 3 MP4 360p (5/10/15 giây), 8,2 MiB. Kiểm tra SHA-256 sau khi chuyển vào máy ảo. Nguồn và hash trong demo-media/sources.json.
- PrepareDemoMediaTest chạy với prepareDemo=true: OK (1 test), 1.648 giây. Kiểm tra đủ 13 tệp, kích thước ảnh, metadata và giải mã khung hình cả ba video; lưu vào cache của tài khoản hiện tại và giữ thư viện cũ. Có bản sao Download/GalleryDemo.
- GalleryNavigationTest sau thay bố cục: OK (1 test), 21.7 giây. Đã xem ảnh chụp thực tế Cài đặt và lưới ảnh có media thật ở build/previews/demo-ready/. Máy ảo có ứng dụng Google Photos nhận ACTION_VIEW video/mp4.
- Bộ demo hiện nạp cục bộ, chưa xác nhận đã tải lên Firebase Storage. Sao lưu cloud dùng luồng Sao lưu ngay trong ứng dụng.

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`: BUILD SUCCESSFUL sau các thay đổi cuối.
- Chạy riêng `GalleryNavigationTest` trên máy ảo API 30 đang kết nối: `OK (1 test)`, 18.201 giây. Kiểm tra bốn tab hiện trong màn hình, vùng chạm ít nhất 48dp, chuyển đúng Activity và khôi phục tab được chọn khi quay về; ô tìm kiếm đủ chiều cao dòng và xử lý chữ tiếng Việt.
- Kiểm tra Hồ sơ → Tài khoản mở AccountActivity; họ tên rỗng bị chặn trước khi ghi. Bài test không đăng xuất hoặc lưu sửa dữ liệu tài khoản thật.
- Đã xem ảnh chụp thực tế trang Ảnh, Tìm kiếm và Tài khoản: ô tìm kiếm không cắt chữ, bốn tab hiện và hồ sơ có tên/email cùng form riêng. Ảnh lưu trong `build/previews/navigation-fix/`.
- AccountActivity dùng listener đọc `users/{uid}`, ghi merge khi người dùng nhấn Lưu và đồng bộ tên Auth sau khi Firestore xác nhận. Chưa kiểm thử thao tác ghi hồ sơ thật lên cloud trong lần kiểm tra này.
- Không xác nhận toàn bộ 21 trạng thái giống từng pixel hoặc mọi chức năng cloud chỉ từ bài kiểm tra này. Xem SCREEN_MAP.md và DEMO_GUIDE.md để đối chiếu chức năng và thiết lập demo.


## Cập nhật editor và phạm vi sao lưu — 18/10/2026

- Sao lưu cloud đã bỏ theo yêu cầu người dùng; BackupActivity giữ giao diện minh họa và không tải file lên Storage. Ghi chú kiểm thử sao lưu cũ phía trên không mô tả bản hiện tại.
- EditorInteractionTest + ThemeSwitchTest trên Gallery_Light API 30: **OK (3 tests), 66.169 giây**. Đã kiểm tra crop/rotate biến đổi tọa độ nét vẽ, filter giữ màu mực, undo/redo/reset; kéo góc cắt, vẽ đỏ, giữ chỉnh sửa sau recreate, xuất JPEG đúng kích thước, giữ album và SHA-256 ảnh gốc không đổi.
- Đã xem ảnh chụp thực tế khung cắt và palette vẽ tại build/previews/editor/.
- Editor giới hạn ảnh xử lý 1600×1600 và tối đa bốn trạng thái hoàn tác để giảm bộ nhớ. Bản sao được lưu cục bộ; chưa có chỉnh sửa video.

- HiddenMediaTest trên bản cài cuối: **OK (1 test), 33.633 giây**. Dùng thao tác nhấn giữ và menu để ẩn một ảnh + một video, kiểm tra biến mất khỏi thư viện/album, vẫn có trong màn đã ẩn; bỏ ẩn và mở lại thư viện để xác nhận trở về. Test khôi phục cờ ẩn của hai mục về trạng thái ban đầu.
- ThemeSwitchTest cũng qua kiểm tra sau khi thêm màn đã ẩn. Lần chạy chung đầu có lỗi đồng bộ chọn cửa sổ trong test ẩn; đã thêm chờ đúng hộp thoại và chạy lại HiddenMediaTest thành công.
- Build app + AndroidTest và lint thành công; lint còn cảnh báo của project. Bản APK mới đã cài bằng cập nhật, không gỡ app hoặc xóa dữ liệu.


## Rà soát cuối — 20/10/2026

- APK debug build thành công; 6/6 unit test và 9/9 bài thiết bị được chọn qua trên Gallery_Light API 30. Lượt thiết bị cuối mất 1 phút 42 giây.
- Đã kiểm tra tìm kiếm không dấu, phạm vi lọc/thứ tự, giữ adapter, trạng thái rỗng, video không mở editor, giữ ảnh hiện tại qua recreate; nhập 3 lượt × 12 ảnh kèm recreate và bảo toàn mục có sẵn; điều hướng, sáng/tối, editor và ẩn/bỏ ẩn.
- Lint: 0 lỗi, còn 425 cảnh báo kế thừa. Xem reports/FINAL_REVIEW_2026-10-20.md để biết phạm vi và giới hạn.
- Lưu ý dữ liệu máy ảo: runner mặc định đã tự gỡ app sau các lượt đầu. Đã khôi phục 15 tệp và 4 album demo từ project; các chỉnh sửa cục bộ khác trước đó không được bảo toàn. Đã bật giữ APK sau connected tests và xác nhận app/dữ liệu còn sau lượt cuối; có snapshot bộ demo trước lượt cuối tại build/test-backup/gallery-demo-before-final.tar.
- Không kiểm thử xác thực bằng tài khoản thật hay cloud. Sao lưu vẫn là giao diện minh họa. Chưa chạy thực tế trên API khác/điện thoại thật.


## Sửa lỗi ảnh trong tài khoản — 20/10/2026

Xem reports/IMAGE_RECOVERY_2026-10-20.md. Kiểm thử trước chỉ xác nhận bộ demo trong chế độ khách, chưa xác nhận cache tài khoản cũ. Đã nối lại 15 tệp demo + download.jpeg trong tài khoản đang đăng nhập; giữ nguyên yêu thích/ẩn/album. Còn 3 bản ghi chưa có tệp gốc để khôi phục. Đã cập nhật cách tìm tệp cục bộ theo ID và kiểm tra lại trực tiếp tài khoản.
