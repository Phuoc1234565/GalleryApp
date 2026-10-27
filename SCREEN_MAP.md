# Đối chiếu 21 màn hình/trạng thái với ảnh mẫu

Giao diện nằm trong `app/src/main/res/layout`, xử lý nằm trong các Activity Java. Các mục menu/bộ lọc là trạng thái của màn hình, không cần giả lập thành Activity riêng. Ảnh thật trong lưới đến từ tệp người dùng nhập hoặc bản sao lưu của tài khoản.

| Mục trong mẫu | Layout chính | Cách mở và chức năng |
|---|---|---|
| 1. Khởi động | activity_splash.xml | Bắt đầu hoặc khám phá trên thiết bị |
| 2. Đăng nhập | activity_login.xml | Email/mật khẩu; số điện thoại có mã quốc gia; đặt lại mật khẩu |
| 3. Đăng ký | activity_register.xml | Tạo tài khoản Firebase, cập nhật họ tên, gửi xác minh email |
| 4. Xác thực | activity_otp.xml | Sáu ô mã cho SMS; với email dùng liên kết xác minh thật |
| 5. Trang Ảnh | activity_main.xml | Lưới ba cột, nhập tệp bằng +, tìm theo tên, lọc ảnh/video/yêu thích |
| 6. Bộ lọc | activity_search.xml, sheet_filter.xml | Sắp xếp mới/cũ, kích thước, tên |
| 7. Options menu | sheet_options.xml | Chọn nhiều, thêm album, ẩn các mục đã chọn, mở Ảnh và video đã ẩn, cài đặt, trợ giúp |
| 8. Context menu | sheet_photo_context.xml | Nhấn giữ ảnh: thông tin, album, yêu thích, ẩn, sửa, chia sẻ, thùng rác |
| 9. Xem ảnh | activity_photo_detail.xml | Vuốt qua ảnh, yêu thích, sửa, chia sẻ, xóa; chạm video để mở trình phát |
| 10. Chỉnh sửa | activity_edit_photo.xml | Kéo khung cắt tự do/1:1/4:3/16:9, xoay, năm bộ lọc, sáng/tương phản, vẽ sáu màu và độ dày, hoàn tác/làm lại, lưu bản sao |
| 11. Thông tin | dialog_photo_info.xml | Tên, kích thước, độ phân giải, thời gian nhập, thiết bị và loại file |
| 12. Album | activity_album.xml, item_album.xml | Hai cột với bìa thật, số ảnh/video, nút ba chấm; tạo mới, thêm ảnh, đổi tên/xóa |
| 13. Tạo album (mẫu đánh số 12 lần hai) | activity_create_album.xml | Nhập tên, chọn ảnh rồi tạo |
| 14. Chi tiết album | activity_album_detail.xml, sheet_album_options.xml | Lưới ảnh, nút + và ba chấm trên toolbar; chọn nhiều/bỏ khỏi album, thêm ảnh, đổi tên/xóa; xóa album giữ ảnh |
| 15. Thùng rác | activity_trash.xml | Khôi phục/xóa vĩnh viễn; dọn tất cả; xử lý mục quá 30 ngày khi mở thùng rác |
| 16. Cài đặt | activity_settings.xml | Tab Hồ sơ: các dòng icon + tiêu đề + thông tin phụ + mũi tên, nhóm Khác; tài khoản, sao lưu, dung lượng thực, thông báo, giao diện, trợ giúp, đăng xuất |
| 17. Google | activity_google_sign_in.xml | Mở trình chọn tài khoản Google thật của hệ thống; cần provider/SHA/client ID |
| 18. Sao lưu | activity_backup.xml | Giao diện minh họa, công tắc lưu lựa chọn; chưa tải file hoặc tự sao lưu |
| 19. Giao diện tối | values-night, ThemeManager.java | Hồ sơ → Giao diện; đổi chung toàn app, cả các màn đã mở, menu, hộp thoại và màn xem/chỉnh sửa ảnh. Giữ lựa chọn khi mở lại app |
| 20. Thông báo | activity_notifications.xml | Lịch sử hoạt động do app ghi, đánh dấu đã đọc; không phải thông báo đẩy FCM |
| 21. Chia sẻ | sheet_share.xml | Chia sẻ tệp qua ứng dụng đã cài; chia sẻ liên kết sau khi sao lưu |

Thanh điều hướng chung có bốn tab: **Ảnh – Album – Tìm kiếm – Hồ sơ**. Khi quay lại Activity đã mở, tab được chọn phải khớp màn hình. Root xử lý khoảng trống thanh hệ thống một lần để các tab không bị che trên Android 11.

Mục Tài khoản mở `activity_account.xml` / `AccountActivity.java`: đọc document `users/{uid}` từ Firestore, sửa họ tên/giới thiệu, lưu bằng merge, hiển thị trạng thái đồng bộ và thông tin đăng nhập từ Firebase Auth. Chế độ khách có nút đăng nhập riêng.

## Những phần cần thiết lập ngoài mã nguồn

- Auth, Google/SMS và Firestore/Storage cần cấu hình theo FIREBASE_SETUP.md. Chưa thể coi việc đăng nhập hoặc sao lưu cloud là đã kiểm thử chỉ từ kết quả build.
- Firestore lưu metadata, Storage lưu file. App tạo document/collection khi ghi dữ liệu; nhập ảnh vào chế độ khách chỉ lưu trong thiết bị.
- Trình chọn tệp, trình chọn tài khoản Google và bảng chia sẻ do Android cung cấp nên hình thức thay đổi theo phiên bản hệ điều hành và ứng dụng đã cài.
- Chỉnh sửa hiện là bộ công cụ cơ bản như mô tả trong bảng; ngôn ngữ hiện hỗ trợ tiếng Việt. Các tùy chọn này không được giới thiệu là công cụ biên tập chuyên nghiệp hay đã có đa ngôn ngữ.

Xem DEMO_GUIDE.md để chuẩn bị ảnh và trình bày các luồng hoạt động.

Mục **Hồ sơ → Ảnh và video đã ẩn** mở HiddenMediaActivity, có tìm kiếm/lọc loại và bỏ ẩn một hoặc nhiều mục. Các mục đã ẩn được loại khỏi thư viện thường, tìm kiếm, nội dung/bìa album và chuỗi vuốt ảnh thường. Đây là bộ lọc trong ứng dụng, chưa có khóa PIN hoặc mã hóa.

Bản hiện tại đã bỏ sao lưu cloud theo yêu cầu; các ghi chú Storage trong tài liệu thiết lập là tùy chọn cho phát triển sau này.
