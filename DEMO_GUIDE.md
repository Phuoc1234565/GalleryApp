# Kịch bản demo ứng dụng Ảnh (5–7 phút)

## Bản đã chuẩn bị sẵn trên Gallery_Light

Tài khoản demo hiện có bốn album **Gia đình, Du lịch, Đồ ăn, Video**, với ảnh bìa và ảnh/video thật. Metadata album và danh sách thành viên đã được máy chủ Firestore xác nhận khi chuẩn bị. Nội dung file nằm trên máy ảo; màn Sao lưu chỉ minh họa giao diện theo yêu cầu.

Bạn có thể trình bày ngay theo thứ tự sau:

1. **Tab Ảnh:** “Đây là thư viện ảnh/video. Có tìm theo tên, lọc loại, đánh dấu yêu thích và nhập thêm bằng nút +.” Gõ `demo`, sau đó xóa từ khóa.
2. **Tab Album:** chỉ bốn thẻ Gia đình, Du lịch, Đồ ăn, Video. Số lượng dưới tên được đếm từ các tệp thật, không phải số cố định trong giao diện.
3. **Mở Du lịch:** xem lưới ảnh; nhấn + để thêm ảnh; nhấn menu ba chấm để chọn nhiều, đổi tên hoặc xóa album. Xóa album giữ nguyên ảnh trong thư viện.
4. **Menu ba chấm của thẻ album:** trình bày các lựa chọn. Có thể đổi tên album rồi đổi lại để chứng minh thao tác có lưu dữ liệu.
5. **Chạm một ảnh:** xem thông tin, yêu thích, chỉnh sửa và lưu bản sao. Nhấn giữ một ảnh trong thư viện để mở menu ngữ cảnh.
6. **Album Video:** có ba video mẫu. Mở một video rồi chạm ảnh xem trước để phát bằng trình phát đã cài trên máy ảo.
7. **Hồ sơ → Giao diện → Sáng/Tối:** chuyển chế độ, quay về Album/Ảnh để chỉ ra toàn app đổi màu. Màn xem ảnh, chỉnh sửa, menu và các hộp thoại cũng dùng chế độ đã chọn. Lựa chọn được giữ khi mở lại app.
8. **Hồ sơ → Tài khoản:** thông tin đọc từ `users/{uid}`. Có thể chỉnh giới thiệu rồi lưu để chứng minh kết nối Firestore.
9. **Firebase Console:** mở `users/{uid}/albums` xem bốn album; mở `photos` xem `albumIds` để giải thích một ảnh có thể thuộc nhiều album. Firestore lưu metadata; file demo đang được lưu cục bộ.

Hãy giữ nguyên tài khoản đang dùng để thấy bộ demo đã chuẩn bị. Nếu đăng nhập tài khoản khác, dữ liệu được tách riêng.

## Chuẩn bị trước buổi demo

Bộ mẫu đã được tải sẵn tại `demo-media/`: **10 ảnh JPG và 3 video MP4**. Trên máy ảo đang dùng có thêm bản sao trong **Download/GalleryDemo**. Sau khi nạp vào tài khoản demo, có thể mở tab Ảnh để trình bày ngay; nếu chuyển tài khoản hoặc cài app trên thiết bị khác, dùng dấu + để nhập lại từ thư mục này.

1. Chạy riêng máy ảo Gallery_Light. Chờ vào Home ổn định rồi Run app.
2. Trên máy tính, chuẩn bị 9–12 ảnh JPG/PNG thật và một video MP4 ngắn. Dùng các tên dễ tìm: `bien_01.jpg`, `bien_02.jpg`, `gia_dinh.jpg`, `hoa.jpg`…
3. Kéo các tệp từ File Explorer thả vào màn hình máy ảo. Chờ chép xong; các tệp thường nằm trong Download.
4. Trong app, chọn **Khám phá trên thiết bị → dấu +**. Trình chọn tệp Android mở ra: nhấn **☰ → Downloads/Download**. Nhấn giữ một tệp rồi chọn thêm nhiều tệp, nhấn **Open/Mở** để nhập.
5. Nếu Recent báo No items, vào Download bằng menu ☰. Nếu đang bật chip Videos thì bỏ bộ lọc đó để nhìn thấy cả ảnh. Đây là màn chọn tệp của Android, không phải thư viện Firestore.
6. Ảnh cần được nhập vào app để xuất hiện trong lưới. App giữ bản sao riêng; ảnh trên máy tính và tệp gốc không bị chỉnh sửa.

Không dùng ảnh lỗi hoặc chỉ đổi đuôi tệp thành .jpg. Một ô có biểu tượng ảnh thay cho hình thật có thể là tệp không còn truy cập được hoặc giải mã thất bại; hãy nhập lại một ảnh JPG/PNG hợp lệ.

## Trình bày và thao tác

| Thời lượng | Thao tác | Nội dung có thể nói |
|---|---|---|
| 30 giây | Mở màn chào, chọn Khám phá trên thiết bị | “Ứng dụng quản lý ảnh và video, giao diện XML trong res/layout, xử lý bằng Java. Có chế độ khách lưu trên thiết bị.” |
| 1 phút | Dấu +, nhập các ảnh đã chuẩn bị | “Người dùng tự chọn tệp; ảnh được sao chép vào bộ nhớ riêng của ứng dụng.” |
| 45 giây | Gõ `bien`, xóa từ khóa, chọn Ảnh/Video, mở bộ lọc | “Có tìm theo tên, lọc loại và sắp xếp theo thời gian hoặc kích thước.” |
| 1 phút | Chạm ảnh để xem; nhấn giữ ảnh để mở menu | “Có xem thông tin, đánh dấu yêu thích, chia sẻ và chuyển vào thùng rác.” |
| 1 phút | Tab Album → Tạo album → nhập tên → chọn ảnh → Tạo | “Album nhóm các ảnh trong thư viện; xóa album vẫn giữ ảnh.” |
| 1 phút | Mở ảnh → Chỉnh sửa → Cắt → kéo góc/di chuyển khung → Áp dụng cắt → Vẽ → chọn màu và độ dày → vẽ lên ảnh → Hoàn tác/Làm lại → Lưu bản sao | “Khung cắt có nhiều tỷ lệ, nét vẽ tùy màu và độ dày. Bản chỉnh sửa lưu thành ảnh mới trong cùng album, giữ nguyên ảnh gốc.” |
| 45 giây | Chuyển một ảnh vào thùng rác → Hồ sơ/Cài đặt → Thùng rác → Khôi phục | “Có thể phục hồi ảnh đã xóa. Chỉ xóa vĩnh viễn khi người dùng xác nhận.” |

Nếu bàn phím đang mở, nhấn Back một lần để đóng trước khi chuyển tab. Khi đổi từ khóa, kết quả được cập nhật ngay.

## Demo Firebase (thêm 2 phút, chuẩn bị trước)

Chỉ cần cấu hình Auth và Firestore để trình bày hồ sơ/album. Bản này không kích hoạt Storage hay sao lưu cloud.

1. Đăng ký/đăng nhập tài khoản đã xác thực. Dữ liệu khách và tài khoản được lưu riêng: nhập lại vài ảnh vào tài khoản demo.
   Mở **Hồ sơ → Tài khoản**, sửa họ tên và giới thiệu, nhấn **Lưu thay đổi**, đợi xác nhận. Mở document `users/{uid}` trong Console để đối chiếu hai trường `displayName`/`bio`. Quay lại app và mở lại hồ sơ để chứng minh dữ liệu được đọc từ Firestore.
2. Mở Firebase Console → Firestore → `users/{uid}/photos` và `users/{uid}/albums` để chỉ ra metadata của tài khoản. Collection/document được app tạo khi ghi dữ liệu; không cần gõ sẵn toàn bộ ảnh trong Console.
3. Nếu mở Sao lưu và đồng bộ, giải thích đây là giao diện minh họa, chưa tải ảnh/video lên cloud. Các công tắc chỉ lưu lựa chọn giao diện.

Chia sẻ tệp mở bảng chia sẻ hệ thống; các ứng dụng nhận phụ thuộc vào những gì đã cài trên máy ảo. Không giới thiệu liên kết cloud hoặc sao lưu là chức năng đang hoạt động.

## Để giao diện giống mẫu khi trình bày

- Nhập đủ ảnh thật để lưới ba cột có nhiều hàng như ảnh tham khảo.
- Chuẩn bị album Du lịch, Gia đình, Đồ ăn với ảnh bìa khác nhau.
- Chọn giao diện sáng trước khi demo, rồi đổi sang tối trong phần Cài đặt để trình bày tùy chọn giao diện.
- Chỉ chạy một máy ảo, chờ build hoàn tất trước khi thao tác; tránh nhập video dung lượng lớn trong buổi demo.

## Demo ẩn ảnh/video (30 giây)

Nhấn giữ ảnh/video trong tab Ảnh → **Ẩn ảnh/video**. Mục đó biến mất khỏi thư viện thường và album. Mở **Hồ sơ → Ảnh và video đã ẩn**, hoặc menu ba chấm trang Ảnh → mục cùng tên. Có tìm theo tên, lọc ảnh/video; nhấn giữ → **Bỏ ẩn** để đưa về thư viện. Chọn nhiều rồi mở menu để ẩn/bỏ ẩn nhiều mục. Thao tác chỉ thay đổi hiển thị trong app, không xóa tệp.
