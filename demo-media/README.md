# Bộ demo: 10 ảnh + 3 video

- Ảnh: Lorem Picsum, kích thước 960 × 720, các ID cố định để dễ tải lại. Tác giả và liên kết ảnh gốc có trong `sources.json`.
- Video: Samplelib, ba đoạn MP4 H.264 360p dài khoảng 5, 10, 15 giây; dùng clip nhỏ để demo trên máy ảo nhẹ hơn.
- Tổng dung lượng tải khoảng 8,2 MiB. Các tệp thật được kiểm tra JPEG/MP4 khi tải; phía Android kiểm tra kích thước ảnh và giải mã khung hình video trước khi thêm vào thư viện.

Nguồn: https://picsum.photos/ và https://samplelib.com/sample-mp4.html. Thông tin tải, tác giả ảnh và SHA-256 từng tệp nằm trong `sources.json`.

Bản sao trên máy ảo nằm ở **Download/GalleryDemo**. Trong app nhấn **+ → ☰ → Downloads → GalleryDemo**, nhấn giữ rồi chọn các tệp để nhập vào một tài khoản khác nếu cần. Thư viện khách và từng tài khoản được lưu riêng.

`tools/fetch_demo_media.py` tải lại bộ tệp. `tools/push_demo_media.py` chép vào máy ảo debug đang kết nối; `PrepareDemoMediaTest` với tham số `prepareDemo=true` đăng ký các tệp vào thư viện của tài khoản hiện tại, giữ ảnh/album cũ và không nhân đôi cùng bộ demo. Bài chuẩn bị này không tự chạy trong các test thông thường.

Các tệp được nạp cục bộ. Muốn có bản trên cloud, dùng **Hồ sơ → Sao lưu và đồng bộ → Sao lưu ngay**, chờ thành công rồi kiểm tra Firebase Storage/Firestore.

Đã bổ sung `album_family.jpg` và `album_food.jpg` để bìa album đúng chủ đề; nguồn và hash nằm trong `album-sources.json` (Pexels/Unsplash). `PrepareDemoAlbumsTest` với `prepareDemo=true` chuẩn bị bốn album, giữ ảnh/album cũ và ghi metadata vào Firestore của tài khoản hiện tại. Bộ album có 2 ảnh Gia đình, 9 ảnh Du lịch, 1 ảnh Đồ ăn và 3 video ở thời điểm chuẩn bị.
