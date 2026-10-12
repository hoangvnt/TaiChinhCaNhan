# 💰 Tài Chính Cá Nhân

Ứng dụng Android quản lý và hoạch định tài chính cá nhân. Ứng dụng được viết bằng Kotlin và Jetpack Compose (Material 3). Dữ liệu đồng bộ đám mây qua Firebase (Google Sign-In + Firestore) và vẫn dùng được khi offline.

## Tính năng

| Nhóm | Chi tiết |
|---|---|
| **Thu / chi** | Ghi khoản thu, chi và chuyển tiền giữa các ví. Lọc theo tháng, loại, ví. Tìm theo ghi chú hoặc danh mục. |
| **Danh mục** | Có sẵn 18 danh mục tiếng Việt. Thêm, sửa, xóa danh mục, chọn emoji và màu. |
| **Ví / tài khoản** | Tiền mặt, ngân hàng, thẻ tín dụng (số dư âm), ví điện tử, tiết kiệm. Số dư tự tính từ giao dịch. |
| **Ngân sách tháng** | Đặt hạn mức cho từng danh mục chi. Thanh tiến độ, gợi ý số tiền "có thể chi mỗi ngày", thông báo khi đạt 80% hoặc 100%. |
| **Mục tiêu tiết kiệm** | Số tiền mục tiêu, hạn chót, nạp/rút tiền. Tự tính số cần để dành mỗi tháng. |
| **Báo cáo** | Biểu đồ vành khuyên theo danh mục, biểu đồ cột thu/chi 6 tháng, tỷ lệ tiết kiệm, tổng kết năm. |
| **Giao dịch định kỳ** | Hằng ngày/tuần/tháng/năm. Có hai chế độ: *Tự động ghi* hoặc *Chỉ nhắc* (bấm "Ghi nhận" hoặc "Bỏ qua kỳ này"). |
| **Nợ / khoản vay** | Đi vay và cho vay, lãi suất đơn %/năm, hạn trả, lịch sử trả. Có thể ghi tiền vào ví. Nhắc trước hạn 3 ngày. |
| **Sao lưu** | Xuất/nhập JSON toàn bộ dữ liệu, xuất CSV mở được bằng Excel. |
| **Bảo mật** | Khóa ứng dụng bằng vân tay, khuôn mặt hoặc PIN của máy. Firestore Rules chỉ cho mỗi người đọc dữ liệu của mình. |

Giao diện tiếng Việt, đơn vị VNĐ, hỗ trợ Android 8.0+ (minSdk 26) và chế độ sáng/tối (màu động trên Android 12+).

## Kiến trúc

```
app/src/main/java/com/hoangvnt/taichinh/
├── TaiChinhApp.kt, AppContainer.kt   # Application + DI thủ công
├── data/
│   ├── model/Models.kt               # Wallet, Category, Transaction, Budget, Goal, Debt, RecurringRule
│   ├── FinanceRepository.kt          # Firestore (users/{uid}/...), cache offline
│   ├── AuthRepository.kt             # Google Sign-In (Credential Manager) + Firebase Auth
│   ├── SettingsRepository.kt         # DataStore: khóa ứng dụng, thông báo
│   └── BackupManager.kt              # JSON / CSV
├── domain/Calculations.kt            # Logic thuần Kotlin (số dư, ngân sách, lãi, kỳ định kỳ) – có unit test
├── work/                             # WorkManager: ghi giao dịch định kỳ + nhắc nhở
└── ui/                               # Compose: MainActivity, điều hướng, các màn hình
```

Cấu trúc dữ liệu Firestore: `users/{uid}/{wallets|categories|transactions|budgets|goals|debts|recurring}/{id}`.

## Cài đặt Firebase (bắt buộc)

1. Vào <https://console.firebase.google.com> và tạo project mới.
2. **Thêm ứng dụng Android**:
   - Package name: `com.hoangvnt.taichinh`.
   - Thêm **SHA-1** của khóa ký. Với bản debug, chạy lệnh sau rồi lấy dòng `SHA1` của biến thể `debug`:
     ```bash
     ./gradlew signingReport
     ```
3. **Authentication**: vào *Sign-in method*, bật **Google**.
4. **Firestore Database**: tạo database ở chế độ *production*, chọn vùng `asia-southeast1` cho gần Việt Nam. Vào tab *Rules*, dán nội dung file [`firestore.rules`](firestore.rules) rồi bấm *Publish*.
5. Tải **`google-services.json`** về (tải sau bước 3 để file có OAuth client) và đặt vào `app/google-services.json`.

> File `google-services.json` đã nằm trong `.gitignore`, sẽ không bị đưa lên Git.

## Build & chạy

Yêu cầu: Android Studio (Ladybug trở lên) hoặc JDK 17 + Android SDK 35.

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # chạy unit test
```

Có thể mở thư mục dự án bằng Android Studio rồi bấm **Run**.

### Build tự động bằng GitHub Actions

Workflow `.github/workflows/android.yml` tự build và chạy test mỗi lần push.

Để CI tạo được APK dùng thật:
1. Vào *Settings → Secrets and variables → Actions*.
2. Tạo secret `GOOGLE_SERVICES_JSON` với nội dung là toàn bộ file `google-services.json`.

Nếu chưa có secret này, CI dùng cấu hình giả: build vẫn chạy và test vẫn chạy, nhưng APK tạo ra không đăng nhập được.

Lưu ý: APK debug do CI build được ký bằng debug keystore của máy CI. SHA-1 của keystore này khác với máy bạn, nên cần thêm SHA-1 đó vào Firebase nếu muốn đăng nhập Google bằng APK tải từ CI. Cách đơn giản nhất là tự build trên máy của bạn.

## Ghi chú sử dụng

- **Lần đăng nhập đầu tiên**: ứng dụng tự tạo danh mục mặc định và ví "Tiền mặt". Bước này cần có mạng.
- **Chuyển tiền**: ở màn *Ví & tài khoản*, bấm biểu tượng ⇄, hoặc chọn loại "Chuyển tiền" khi thêm giao dịch.
- **Thẻ tín dụng**: bật "Số dư âm" cho dư nợ ban đầu. Chi tiêu bằng thẻ sẽ làm số dư âm thêm. Trả thẻ bằng cách chuyển tiền từ ví ngân hàng sang thẻ.
- **Giao dịch định kỳ**: được xử lý khi mở ứng dụng và định kỳ khoảng 6 giờ/lần ở chế độ nền.
