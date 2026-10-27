> Bản demo hiện tại đã bỏ sao lưu cloud theo yêu cầu. Màn Sao lưu chỉ minh họa; không cần kích hoạt Storage để dùng cắt/vẽ, album hoặc hồ sơ. Các mục Storage bên dưới là tài liệu tùy chọn cho phát triển sau này.

# Hướng dẫn Firebase cho GalleryApp

## 1. Chọn đúng Firebase project

Repository bài tập có kèm `app/google-services.json` cho package **com.example.appanh**. Bạn có thể dùng cấu hình Firebase hiện có; nếu chuyển sang Firebase project khác, tạo hoặc chọn project của bạn → Add app → Android → nhập đúng package này → tải `google-services.json` thay file trong thư mục `app/`.

Không đổi `applicationId` riêng lẻ: package trong file cấu hình phải trùng với `app/build.gradle.kts`.

## 2. Bật đăng nhập

Firebase Console → Authentication → Get started → Sign-in method:

1. **Email/Password** → Enable → Save.
2. **Google** → Enable → chọn support email → Save.
3. Nếu dùng OTP SMS: **Phone** → Enable, kiểm tra chính sách vùng SMS. Khi thử trên emulator, thêm số điện thoại thử và mã 6 số trong phần Phone numbers for testing. App không hardcode mã OTP.
4. Android Studio → Gradle → Tasks → android → signingReport (hoặc terminal `./gradlew signingReport`). Sao chép **SHA-1 và SHA-256** debug vào Firebase → Project settings → Your apps → Android app → Add fingerprint.
5. Sau khi bật Google và thêm SHA, **tải lại** `google-services.json` và đặt vào `app/`, Sync rồi Run lại. File mới cần có OAuth web client để sinh resource `default_web_client_id`.

Email xác thực dùng **liên kết trong email**, không phải mã 6 số. Sau đăng ký: mở email, nhấn xác minh, trở lại app → Tôi đã xác minh email. SMS OTP chỉ dành cho đăng nhập số điện thoại (`+849…`, bỏ số 0 đầu của số Việt Nam). Đây là cơ chế Firebase chính thức: [email](https://firebase.google.com/docs/auth/android/manage-users), [phone](https://firebase.google.com/docs/auth/android/phone-auth).

Không cần tự tạo user trong Firestore để đăng nhập: Authentication quản lý mật khẩu, không bao giờ lưu mật khẩu vào Firestore.

## 3. Tạo Cloud Firestore

Firebase Console → Build → Firestore Database → Create database → chọn **Standard / Native mode**, database `(default)` → chọn khu vực phù hợp → dùng Production mode. Vào tab **Rules**, thay nội dung bằng file `firestore.rules` trong project → **Publish**.

Rules chỉ cho phép tài khoản đọc/ghi dữ liệu bên dưới UID của chính nó. Không dùng `allow read, write: if true`.

## 4. Tạo các collection bằng tay (nếu muốn nhìn cấu trúc trước)

App tự tạo collection khi có dữ liệu; bạn **không bắt buộc** nhập document mẫu. Nếu muốn tạo bằng Console, thực hiện đúng cấp bên dưới.

### Collection `users`

Firestore → Data → Start collection → Collection ID: `users`. Document ID: **UID thật** sao chép từ Authentication → Users → chọn user → User UID, không phải email.

| Field | Type | Ví dụ |
|---|---|---|
| email | string | ban@example.com |
| displayName | string | Nguyễn Văn An |
| lastLoginAt | timestamp | thời gian hiện tại |

App cập nhật các trường này sau đăng nhập bằng merge nên không xóa subcollection. Tên đã chỉnh trong hồ sơ được giữ qua các lần đăng nhập.

Mở **Hồ sơ → Tài khoản** để xem/sửa document `users/{UID hiện đang đăng nhập}`. Màn hình đọc cập nhật Firestore và cho sửa `displayName` (1–80 ký tự), `bio` (tối đa 240 ký tự). Nút **Lưu thay đổi** ghi hai trường này cùng `email`, `phoneNumber`, `emailVerified`, `updatedAt` (server timestamp) bằng merge. Email/số điện thoại lấy từ Firebase Auth và không sửa như văn bản hồ sơ. App chỉ báo đã lưu trên Firestore sau khi máy chủ xác nhận; khi mất mạng có thể hiện trạng thái chờ đồng bộ.

Để demo: sửa họ tên/giới thiệu → Lưu → đợi xác nhận → Firebase Console → Firestore → `users` → document trùng UID trong Authentication. Kiểm tra `displayName` và `bio`, sau đó quay lại app/mở lại màn hồ sơ. Không cần tạo collection tài khoản mới, không dùng email làm document ID. Nếu tài khoản chưa có document thì lần lưu sẽ tạo `users/{UID}`. Rules phải cho phép chủ tài khoản đọc/ghi document của chính mình như file firestore.rules.

### Subcollection `photos`

Mở document `users/{UID}` → Start collection → ID `photos`. Mỗi ảnh là một document riêng, ID UUID do app tạo. **Nên để app nhập và sao lưu ảnh đầu tiên để tạo document đúng**, vì URL phải trỏ tới file thực đã upload. Ví dụ các trường:

| Field | Type | Ý nghĩa / ví dụ |
|---|---|---|
| userId | string | cùng UID với document cha |
| fileName | string | IMG_20261016.jpg |
| firebaseUrl | string | URL tải file sau upload Storage |
| fileSize | number (integer) | 5242880 (byte) |
| width | number (integer) | 3024 |
| height | number (integer) | 4032 |
| mimeType | string | image/jpeg hoặc video/mp4 |
| video | boolean | false |
| favorite | boolean | false |
| deleted | boolean | false |
| hidden | boolean | false (ẩn khỏi lưới mặc định) |
| deletedAt | null / timestamp | null khi chưa xóa |
| albumIds | array of string | [] hoặc [ID album] |
| dateTaken | timestamp | thời gian nhập ảnh |
| createdAt | timestamp | thời gian tạo |
| updatedAt | timestamp | server timestamp khi app ghi |

Đừng tạo field `isDeleted` hay `isFavorite`: Java getter `isDeleted()` ánh xạ thành field **deleted**, tương tự **favorite**, **video**.

`filePath` chỉ lưu cache trên thiết bị, không ghi Firestore vì đường dẫn máy này không dùng được trên máy khác. `dateTaken` hiện là thời gian nhập, chưa đọc EXIF ngày chụp. ID là document ID, không cần field `id`.

**Thùng rác không phải collection riêng**: document ảnh có `deleted=true`, `deletedAt=timestamp`; khôi phục đưa về `false/null`. Khi mở thùng rác, app dọn mục quá 30 ngày. Không cấu hình TTL Firestore riêng lẻ cho ảnh: TTL metadata không tự xóa file Storage tương ứng.

### Subcollection `albums`

Trong `users/{UID}` → Start collection → `albums`. Document ID UUID do app tạo.

| Field | Type | Ví dụ |
|---|---|---|
| userId | string | UID thật |
| name | string | Du lịch hè (1–80 ký tự) |
| description | string | rỗng |
| createdAt | timestamp | thời gian tạo |
| updatedAt | timestamp / null | thời gian cập nhật |
| photoIds | array | [] (field model cũ; không dùng để tính ảnh) |
| photoCount | number | 0 (field model cũ) |
| videoCount | number | 0 (field model cũ) |
| coverPhotoUrl | string / null | tùy chọn |
| coverPhotoPath | string / null | tùy chọn |

App xác định thành viên bằng **photos/{photoId}.albumIds** và tính số lượng trực tiếp; không sửa photoCount để thêm ảnh. Chọn ảnh trong app sẽ cập nhật albumIds. Xóa album chỉ xóa document album, giữ nguyên ảnh; ID album cũ trên ảnh được bỏ qua khi không còn document album.

### Subcollection `notifications`

Trong `users/{UID}` → Start collection → `notifications`. Document ID Auto-ID.

| Field | Type | Ví dụ |
|---|---|---|
| message | string | Đã sao lưu 3 ảnh/video lên Firebase Storage |
| createdAt | timestamp | thời gian tạo |
| read | boolean | false |

Không cần tạo composite index cho các query hiện tại; app tải collection theo tài khoản rồi lọc/sắp xếp ảnh tại máy. Thông báo chỉ orderBy createdAt (single-field index mặc định).

## 5. Tạo Storage để lưu file ảnh/video

Firebase Console → Build → Storage → Get started → chọn vị trí bucket. Cloud Storage for Firebase hiện yêu cầu project dùng gói **Blaze**, theo [hướng dẫn chính thức](https://firebase.google.com/docs/storage/android/start). Bạn cần tự bật billing trong Firebase/Google Cloud nếu chưa có; mã app không thể tạo bucket hoặc bật billing thay bạn.

Storage → Rules → dán file **storage.rules** → Publish. File lưu tại:

```text
users/<UID>/media/<photoId>
```

Tên object không cần đuôi mở rộng; Content-Type lấy từ file nhập. Rules mẫu chấp nhận image/* hoặc video/* và file nhỏ hơn 100 MiB. Nếu cần file lớn hơn, điều chỉnh giới hạn trong rules có chủ đích. Firestore chỉ lưu URL và metadata, không lưu base64 ảnh.

Trình tự sao lưu: upload file Storage → lấy download URL → ghi `users/{uid}/photos/{photoId}`. Khi lỗi, app hiện thông báo; file đã có URL được bỏ qua ở các lần sao lưu sau. Các metadata yêu thích/album/thùng rác có thể có trước khi upload; ảnh chưa có URL không xem được trên thiết bị khác.

## 6. Kiểm tra sau cấu hình

1. Chạy app, đăng ký email/mật khẩu và xác minh email.
2. Nhập một ảnh thử vào tab Ảnh.
3. Cài đặt → Sao lưu và đồng bộ → kết nối Wi-Fi → Sao lưu ngay.
4. Storage → Files: phải có `users/UID/media/photoId`.
5. Firestore → Data: phải có `users/UID/photos/photoId` với `firebaseUrl` không rỗng.
6. Tạo album và thêm ảnh: kiểm tra `albums/{albumId}` và mảng `albumIds` trong document ảnh.
7. Đăng nhập cùng tài khoản trên máy khác để kiểm tra tải bản sao lưu.

## Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| PERMISSION_DENIED Firestore | Publish firestore.rules, UID đúng document cha, userId của ảnh/album trùng UID |
| Storage unauthorized | Publish storage.rules; đăng nhập đúng tài khoản; đường dẫn phải users/UID/media/ID |
| Bucket not found / lỗi Storage | Bật Storage, tạo bucket, kiểm tra storage_bucket trong google-services.json, billing Blaze |
| Google lỗi 10 / không có default_web_client_id | Thêm SHA-1/SHA-256, bật Google provider, tải lại JSON, Sync và cài lại app |
| Email chưa gửi / too many requests | Kiểm tra provider, Spam, chờ trước khi gửi lại; không spam nút |
| SMS không đến | Kiểm tra Phone provider, SHA, vùng SMS, quota/billing; dùng test number của Firebase khi thử |
| Ảnh khách biến mất sau đăng nhập | Dữ liệu khách tách riêng theo thiết kế; đăng xuất để vào khách hoặc nhập lại ảnh gốc vào tài khoản |
| Ảnh trên máy khác không mở | File chưa sao lưu thành công hoặc firebaseUrl không hợp lệ |

Các file rules trong repo chỉ có hiệu lực sau khi bạn Publish trên Console (hoặc deploy bằng Firebase CLI với đúng project). Không có thao tác deploy nào được tự động thực hiện bởi project này.
