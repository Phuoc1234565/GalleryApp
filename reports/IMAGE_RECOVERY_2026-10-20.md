# Khôi phục ảnh sau kiểm thử — 20/10/2026

Nguyên nhân: lượt khôi phục trước chỉ dựng lại cache chế độ khách. Người dùng đăng nhập tài khoản cũ; Firestore trả lại metadata nhưng không có nội dung tệp và không chứa đường dẫn riêng trên thiết bị. Cache tài khoản có filePath=null trong khi 15 tệp demo vẫn tồn tại trên máy.

Đã sao lưu toàn bộ files/shared_prefs hiện tại trước khi sửa vào build/test-backup/before-image-repair.tar. Không đăng xuất, không gỡ app, không xóa/ghi lại yêu thích, ẩn hoặc quan hệ album trên cloud.

Khôi phục đường dẫn cho 15 mục demo theo ID trùng khớp, đồng thời khôi phục đường dẫn bìa album theo ID. Các album do người dùng tạo vẫn giữ nguyên. Ảnh bị ẩn vẫn giữ trạng thái ẩn; album Đồ ăn có 0 ảnh hiển thị vì ảnh của album đang được ẩn.

Tìm được download.jpeg gốc trong Downloads, trùng tên, kích thước 12916 byte và 194×259 pixel với metadata. Sao chép vào bộ nhớ app và nối lại đúng bản ghi, giữ yêu thích/album hiện tại. Tổng cộng 16 bản ghi đã có lại tệp gốc.

Ba bản ghi còn thiếu tệp: EDIT_1792332265113.jpg (đang ẩn), EDIT_1792513182811.jpg và IMG_001.jpg. Không có firebaseUrl và không tìm thấy bản gốc trong media/Downloads hoặc snapshot project. Giữ nguyên các bản ghi; không thay bằng ảnh khác hoặc xóa để che lỗi. Chưa thể khôi phục ba tệp này.

Sửa GalleryRepository để tìm lại tệp nhập/xuất theo ID hợp lệ khi filePath bị thiếu hoặc cũ; thao tác tải metadata tài khoản giữ lại đường dẫn tệp tìm được. Đường dẫn ID có ký tự thoát thư mục bị từ chối.

Đã build APK cập nhật và cài bằng cập nhật, giữ app/dữ liệu đăng nhập. Sáu unit test qua. Kiểm tra khôi phục trực tiếp trên tài khoản qua, đã chụp và xem lại thư viện/album tại build/previews/final-review/gallery-recovered.png và albums-recovered.png. Ảnh/video demo và ảnh tải xuống hiển thị lại; các mục không còn file vẫn hiện biểu tượng lỗi.
