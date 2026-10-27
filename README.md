# Ảnh — GalleryApp (Java + XML)

Mở thư mục này bằng Android Studio. Giao diện nằm trong `app/src/main/res/layout`, xử lý nằm trong `app/src/main/java/com/example/appanh`.

Xem [kịch bản demo 5–7 phút](DEMO_GUIDE.md) và [đối chiếu 21 màn hình/trạng thái](SCREEN_MAP.md). Mục **Hồ sơ → Tài khoản** mở màn chỉnh sửa thông tin, đọc/ghi document `users/{uid}` trên Firestore thay cho hộp thoại UID/email.

Bản demo trên Gallery_Light đã có bốn album **Gia đình, Du lịch, Đồ ăn, Video** với ảnh bìa/nội dung thật và menu ba chấm. Chế độ **Sáng/Tối** dùng chung toàn ứng dụng qua ThemeManager, gồm màn xem/chỉnh sửa ảnh, menu, ô nhập và các Activity đã mở trước đó.

## Chạy app

Repository bài tập có kèm `app/google-services.json`, bộ media demo và báo cáo. Có thể dùng cấu hình Firebase hiện có hoặc thay bằng cấu hình project Firebase của bạn theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md). `local.properties`, bản build, bộ nhớ đệm và dữ liệu máy ảo không được đưa lên repository.

Nếu dùng Pixel 8 và gặp System UI không phản hồi, xem [hướng dẫn Android Studio / Pixel 8](ANDROID_STUDIO_PIXEL8_SETUP.md), gồm cách xóa riêng Pixel 6, RAM máy ảo, JDK và Gradle đã đối chiếu với cấu hình thật của project.

1. Android Studio → Open → chọn `GalleryApp`, chờ Gradle Sync.
2. Chọn **Gradle JDK = Embedded JDK** của Android Studio. Project hiện dùng Gradle 9.5 / Android Gradle Plugin 9.3.3 và compile SDK 37 theo cấu hình ban đầu; cài SDK tương ứng khi Studio yêu cầu.
3. File `local.properties` hiện trỏ tới `D:\AndroidSDK`. Nếu chuyển máy, để Studio tạo lại đường dẫn này.
4. Chọn emulator hoặc điện thoại Android 7.0 (API 24) trở lên → Run `app`.
5. Có thể chọn **Khám phá trên thiết bị** để dùng thư viện cục bộ trước khi cấu hình Firebase. Thêm ảnh/video bằng trình chọn file của Android. Không cần cấp quyền đọc toàn bộ thư viện.

Ảnh nhập được **sao chép vào bộ nhớ riêng của app**; xóa trong app không xóa ảnh gốc trong thư viện hệ thống. Gỡ app sẽ xóa các bản sao riêng. Dữ liệu khách và dữ liệu từng tài khoản được tách riêng; đăng nhập không tự chuyển ảnh khách sang tài khoản. Sao lưu cloud đã được bỏ theo yêu cầu; màn Sao lưu chỉ minh họa giao diện.

## 21 màn hình / trạng thái theo ảnh mẫu

Ảnh mẫu đánh số 12 hai lần; bảng dưới dùng số thứ tự 1–21 liên tục. Các menu và bottom sheet là layout XML riêng, không phải 21 Activity độc lập.

| STT | Màn hình | Layout XML | Java xử lý |
|---|---|---|---|
| 1 | Khởi động | activity_splash | SplashActivity |
| 2 | Đăng nhập | activity_login | LoginActivity / AuthActivity |
| 3 | Đăng ký | activity_register | RegisterActivity / AuthActivity |
| 4 | Xác thực | activity_otp | OtpActivity |
| 5 | Thư viện ảnh | activity_main | MainActivity / GalleryActivity |
| 6 | Tìm kiếm và bộ lọc | activity_search, sheet_filter | SearchActivity / GalleryActivity |
| 7 | Menu tùy chọn | sheet_options | GalleryActivity |
| 8 | Menu nhấn giữ ảnh | sheet_photo_context | GalleryActivity |
| 9 | Xem ảnh chi tiết | activity_photo_detail | PhotoDetailActivity |
| 10 | Chỉnh ảnh | activity_edit_photo | EditPhotoActivity |
| 11 | Thông tin ảnh | dialog_photo_info | PhotoDetailActivity.info |
| 12 | Danh sách album | activity_album | AlbumActivity |
| 13 | Tạo album | activity_create_album | CreateAlbumActivity |
| 14 | Album chi tiết | activity_album_detail | AlbumDetailActivity |
| 15 | Thùng rác | activity_trash | TrashActivity |
| 16 | Cài đặt | activity_settings | SettingsActivity |
| 17 | Google | activity_google_sign_in | GoogleSignInActivity |
| 18 | Sao lưu | activity_backup | BackupActivity |
| 19 | Giao diện tối | activity_dark_gallery | MainActivity / BaseActivity |
| 20 | Thông báo | activity_notifications | NotificationsActivity |
| 21 | Chia sẻ | sheet_share | ShareActivity |

Có thêm layout phụ trợ bộ lọc và ô ảnh. Giao diện sử dụng màu xanh, Material, lưới ảnh bo góc, thanh điều hướng dưới và chế độ tối. Ảnh thật được lấy từ thiết bị, không phải ảnh mẫu hardcode.

Giao diện đã được thiết kế lại theo phản hồi: màn chào nền sáng với minh họa phong cảnh, logo hoa nhiều cánh, form rõ chữ, album có ảnh bìa và tên bên dưới, menu/cài đặt dạng dòng có icon, thùng rác dạng danh sách. Màn xem và chỉnh ảnh dùng nền tối cùng thanh thao tác ngang. Các file XML là giao diện app thực tế; `tools/polish_screens.py` giữ nguồn tạo giao diện đồng bộ, chạy lại sẽ ghi đè các layout nó quản lý.

Xem [VERIFICATION.md](VERIFICATION.md) để phân biệt kết quả build, kiểm thử và vấn đề máy ảo. Chỉ mở một emulator khi máy thiếu tài nguyên; lỗi **System UI / Process system isn't responding** cần kiểm tra cả cấu hình máy ảo, không chỉ mã app.

## Hoàn thiện ngày 20/10/2026

- Tìm tên ảnh/video có hoặc không dấu tiếng Việt; trạng thái không có kết quả hướng dẫn đổi từ khóa/bộ lọc.
- Giữ adapter và vị trí cuộn khi chọn ảnh, tìm kiếm hoặc làm mới; số cột thích ứng với màn hình rộng.
- Màn xem ảnh vuốt theo đúng album, bộ lọc và thứ tự đã chọn; giữ ảnh hiện tại khi xoay máy; vô hiệu hóa chỉnh sửa video.
- Nhập nhiều tệp chạy trong ViewModel, tiếp tục qua việc tạo lại màn hình và lưu gộp với thư viện trên đĩa; báo số mục nhập thành công/thất bại. Tác vụ này chưa phải dịch vụ nền và không bảo đảm tiếp tục nếu hệ điều hành kết thúc tiến trình.
- Tăng vùng chạm các chip, ghi rõ Yêu thích, nhấn Back để thoát chọn nhiều; nhập sai tên album không làm đóng hộp thoại.
- Bìa album loại ảnh đã ẩn/đã xóa; chia sẻ giải thích đúng phạm vi cloud đang tắt. Khung cắt editor tái sử dụng đối tượng khi vẽ.

Chi tiết kiểm tra bản cuối: [reports/FINAL_REVIEW_2026-10-20.md](reports/FINAL_REVIEW_2026-10-20.md).

## Tính năng

- Email/mật khẩu, đăng ký, đặt lại mật khẩu, liên kết xác minh email; điện thoại + SMS OTP; đăng nhập Google qua cửa sổ chọn tài khoản Google thật.
- Nhập nhiều ảnh/video bằng Android document picker, sao chép nền, hiển thị thumbnail; mở video bằng ứng dụng phát của máy.
- Lọc ảnh/video/yêu thích, tìm tên không phân biệt hoa thường, sắp xếp ngày/tên/kích thước; chọn nhiều để thêm vào album.
- Nhấn giữ ảnh/video để ẩn; Hồ sơ hoặc menu trang Ảnh → Ảnh và video đã ẩn để tìm/lọc và bỏ ẩn một hoặc nhiều mục. Trạng thái lưu ngay trên thiết bị, loại khỏi thư viện/tìm kiếm thường, bìa/nội dung album và chuỗi vuốt ảnh thường. Đây là bộ lọc hiển thị, chưa có khóa PIN hay mã hóa riêng.
- Tạo, đổi tên, xóa album (giữ ảnh); thêm ảnh vào album; xem số lượng thực tế.
- Xem ảnh, vuốt chuyển ảnh, yêu thích, thông tin file, chia sẻ bằng Android Sharesheet và liên kết sau sao lưu.
- Editor: kéo góc/di chuyển khung cắt tự do hoặc 1:1, 4:3, 16:9; xoay 90°; năm bộ lọc; độ sáng/tương phản; vẽ sáu màu, chỉnh độ dày, xóa nét, hoàn tác/làm lại và khôi phục gốc. Xuất JPEG thành bản sao cục bộ trong cùng album; ảnh gốc giữ nguyên. Giải mã tối đa 1600×1600 để giảm bộ nhớ; các thao tác biến đổi/xuất chạy nền. Chưa có editor video.
- Thùng rác: khôi phục, xóa vĩnh viễn có xác nhận, dọn nhiều mục. Dọn mục quá 30 ngày khi mở thùng rác; không phải lịch xóa trên server khi app đóng.
- Sao lưu và đồng bộ: giữ giao diện và các công tắc minh họa; không tải file lên cloud và không tự sao lưu. Nút Sao lưu ngay giải thích rõ trạng thái demo.
- Thông báo hoạt động trong app và Firestore; chưa phải push notification FCM.
- Chế độ tối lưu qua lần mở app. Phiên bản này dùng tiếng Việt; mục ngôn ngữ hiển thị rõ trạng thái hỗ trợ.

## Cấu hình Firebase và tạo collection

Xem hướng dẫn từng bước, kiểu dữ liệu và cách kiểm tra ở [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các đường dẫn **đang được app sử dụng**:

```text
Cloud Firestore
users/{uid}                         hồ sơ
users/{uid}/photos/{photoId}        metadata ảnh/video, cả mục trong thùng rác
users/{uid}/albums/{albumId}        album
users/{uid}/notifications/{id}      thông báo hoạt động

Firebase Storage (chưa kích hoạt; không được màn Sao lưu gọi)
users/{uid}/media/{photoId}         đường dẫn dự kiến trong mã dữ liệu
```

`GalleryRepository.java` là lớp dữ liệu đang dùng. Helper Firebase cũ trong `helpers` được giữ từ project ban đầu nhưng không được các Activity mới gọi. Không tạo collection `photos`, `albums`, `trash` ở cấp gốc theo helper cũ.

Collection xuất hiện tự động khi app ghi document đầu tiên. Firestore chứa metadata; nội dung ảnh/video demo hiện nằm trên thiết bị. Không dùng Firestore để chứa file nhị phân.

## Kiểm tra thủ công

1. Chế độ khách → nhập 2 ảnh và 1 video → đóng/mở lại: mục vẫn còn.
2. Tìm tên file, đổi chip lọc; tab tìm kiếm → sắp xếp mới/cũ/kích thước/tên.
3. Tạo album, chọn ảnh, mở album, đổi tên và xóa album: ảnh vẫn còn trong thư viện.
4. Mở ảnh → yêu thích → chỉnh ảnh → lưu: có bản sao mới, ảnh gốc giữ nguyên.
5. Nhấn giữ → thùng rác → khôi phục; thử lại rồi xóa vĩnh viễn với file thử nghiệm.
6. Cài đặt → giao diện tối → đóng/mở lại.
7. Sau cấu hình Firebase: mở Hồ sơ → Tài khoản → sửa thông tin → lưu → đối chiếu document users/{uid} trên Firestore.
8. Hồ sơ → Sao lưu: xác nhận hiển thị trạng thái minh họa; không khẳng định đã đồng bộ file sang thiết bị khác.
9. Tắt mạng → kiểm tra cache. Đăng xuất/đăng nhập tài khoản khác: không lẫn dữ liệu.

Chỉ thao tác với ảnh thử khi kiểm tra xóa vĩnh viễn. Rules mẫu không mở dữ liệu giữa các tài khoản.


### Cập nhật nội dung mới — 21/10/2026

Bốn ảnh mới và ba video có nội dung riêng đã có trong app; ba video là cảnh bướm, ong và thác nước, thay cho clip dựng từ ảnh trước đó. GalleryDemo trong Downloads được lập chỉ mục để hiện đủ 15 tệp ban đầu. Xem reports/DISTINCT_VIDEOS_AND_DEMO_FOLDER_2026-10-21.md và demo-media/distinct-videos/ATTRIBUTION.md.
