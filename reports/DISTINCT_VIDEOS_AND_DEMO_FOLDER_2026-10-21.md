# Tách nội dung ảnh/video và sửa GalleryDemo — 21/10/2026

Đã sao lưu dữ liệu trước khi sửa tại build/test-backup/before-distinct-videos.tar.

GalleryDemo không mất tệp: có đủ 12 JPEG và 3 MP4 trong /sdcard/Download/GalleryDemo nhưng Android MediaStore chưa lập chỉ mục, nên DocumentsUI báo No items. Đã quét đủ 15 tệp; kiểm tra qua nút + → Downloads → GalleryDemo, cuộn danh sách và xác nhận đủ 15 tên tệp xuất hiện, có thể chọn.

Thay đúng ba clip dựng từ ảnh vừa thêm bằng ba cảnh quay thật độc lập với bộ ảnh: bướm cánh trong, ong trên hoa và thác nước. Video nguồn từ Wikimedia Commons; trích 8/7/6 giây, thu nhỏ và đổi sang MP4 không tiếng. Ghi công và giấy phép nằm tại demo-media/distinct-videos/ATTRIBUTION.md và sources.json. Tệp thác nước chuyển thể được phát hành theo CC BY-SA 4.0 như nguồn; video bướm theo CC BY 2.0; video ong theo CC0.

Thay nội dung tệp và metadata trên đúng ID hiện có; giữ yêu thích, ẩn, album. Sau lưu, tải lại từ repository/Firestore và đối chiếu tên, tệp và trạng thái. Giữ bốn ảnh AI hiện có và ba video demo ban đầu. Các clip dựng từ ảnh cũ được lưu tại demo-media/fresh/photo-motion-archive, và bỏ khỏi cấp chính Download/GalleryFresh.

Phát hiện Glide vẫn hiện thumbnail của clip cũ khi nội dung tệp đổi nhưng đường dẫn giữ nguyên. Đã bổ sung chữ ký cache dựa trên thời gian sửa/kích thước tệp cho lưới, album, màn chi tiết và hộp thông tin; build APK cập nhật, không gỡ app hay xóa dữ liệu.

Build debug và 6 unit test qua. Bài bảo trì kiểm tra video giải mã được ở hai thời điểm và các khung hình khác nhau, metadata hợp lệ, giữ ID/trạng thái sau reload; kiểm tra danh sách thật của DocumentsUI. Ảnh chụp kết quả tại build/previews/distinct-videos/.

Lượt kiểm tra đầu xác nhận đủ 15 mục trong GalleryDemo. Sau cập nhật thumbnail, lượt bảo trì thứ hai qua các kiểm tra tệp/metadata nhưng timeout ở thao tác điều hướng lại thư mục trong DocumentsUI; ảnh chụp video trên APK mới xác nhận thumbnail là các cảnh mới. Không coi lượt UI timeout này là một lượt kiểm tra thành công.

Kiểm tra mở lại trên APK cuối thành công: đủ 4 ảnh mới và 3 video đã thay, không thêm bản sao, tài khoản và các ID có sẵn được giữ. Ảnh chụp fresh-videos.png xác nhận cả sáu thumbnail video hiển thị đúng (ba mới + ba ban đầu).
