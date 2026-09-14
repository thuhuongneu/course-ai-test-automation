---
name: skills-user-guide-writer
description: Skill sinh Hướng dẫn sử dụng (User Guide / User Manual) cho phần mềm từ tài liệu requirements và evidence đã khảo sát — viết theo việc người dùng cần làm, bám cấu trúc ISO/IEC/IEEE 26514 và 82079-1, mọi bước truy được về bằng chứng thật.
---

# User Guide Writer

Purpose: Biến thứ QA đã biết rất rõ về hệ thống (requirements, evidence, luồng thao tác đã chạy tay hàng chục lần) thành **tài liệu người dùng cuối đọc và làm theo được** — không phải bản mô tả hệ thống viết lại bằng giọng khác.

---

## When to Use

- Bàn giao sản phẩm cho khách hàng, cần tài liệu hướng dẫn kèm theo hợp đồng
- Đào tạo người dùng mới (nhân viên vận hành, khách hàng cuối)
- Sau khi module đã recon xong và có `docs/requirements/<module>/` đầy đủ
- Hệ thống đã có tài liệu nhưng viết theo màn hình, người dùng đọc không biết bắt đầu từ đâu

## When NOT to Use

| Tình huống | Dùng cái khác |
|---|---|
| Cần mô tả *hệ thống phải làm gì* cho QA/dev | `skills-requirements-analyzer` |
| Cần kịch bản kiểm thử | `skills-rbt-manual-testing` |
| Module **chưa recon**, chưa có evidence | Chạy `/discover-system` → `/generate-requirements-from-website` trước. **Không** viết hướng dẫn cho màn hình chưa từng nhìn thấy |
| Cần tài liệu kỹ thuật cho lập trình viên (API docs, kiến trúc) | Ngoài phạm vi skill này |

---

## Ranh giới — ba tài liệu hay bị viết lẫn vào nhau

| | Requirements | Test Case | **User Guide** |
|---|---|---|---|
| Trả lời câu hỏi | Hệ thống **phải làm gì** | Làm sao **biết nó đúng** | Tôi **làm thế nào** để xong việc |
| Người đọc | QA · Dev · BA | Tester | **Người dùng cuối** — không biết và không cần biết thuật ngữ hệ thống |
| Tổ chức theo | Chức năng / màn hình | REQ ID | ⭐ **Việc người dùng muốn làm** |
| Có nói về lỗi không | Có — mọi ràng buộc validation | Có — mọi case âm | Chỉ lỗi người dùng **hay gặp** và **cách tự xử lý** |
| Giọng văn | Mô tả (*"Hệ thống hiển thị…"*) | Mô tả + kỳ vọng | **Mệnh lệnh** (*"Nhấn **Lưu**."*) |

> 🚫 **Lỗi số một:** chép `requirements_<module>.md` rồi đổi giọng. Ra một tài liệu tổ chức theo màn hình, liệt kê đủ mọi ràng buộc validation — đúng về nội dung nhưng người dùng không dùng được, vì họ mở tài liệu ra với **một việc cần làm**, không phải với một màn hình cần hiểu.

---

## Chuẩn tham chiếu

| Chuẩn | Dùng cho |
|---|---|
| **ISO/IEC/IEEE 26514** — Design and development of information for users | Cấu trúc và nội dung tài liệu người dùng — khung mục của skill này |
| **ISO/IEC/IEEE 82079-1:2019** — Preparation of information for use | Nguyên tắc chung · phân cấp cảnh báo an toàn · nguyên tắc tối giản |
| **ISO/IEC/IEEE 26513:2017** — Requirements for testers and reviewers of information for users | Cơ sở cho Quality Gate ở cuối skill này |

> ⚠️ Ghi **tên chuẩn**, **KHÔNG ghi số điều khoản** — số đổi giữa các bản phát hành, agent không có bản chuẩn trong tay để tra.
>
> ⚠️ **KHÔNG tuyên bố "tuân thủ ISO/IEC/IEEE 26514"** — tuân thủ là kết luận của đánh giá viên. Câu đúng: *"biên soạn theo cấu trúc ISO/IEC/IEEE 26514"*.

---

## Quy Tắc Chống Bịa (BẮT BUỘC — đọc trước khi viết dòng đầu tiên)

Hướng dẫn sử dụng sai **nguy hiểm hơn** requirements sai: người dùng làm theo từng chữ, và khi làm không được thì họ kết luận **phần mềm hỏng**, không kết luận tài liệu sai.

### 1. Mọi bước phải truy được về một trong ba nguồn

| Nguồn | Ghi chú |
|---|---|
| Một tấm ảnh trong `docs/requirements/<module>/evidence/` | Mở ảnh ra xem thật, không đọc tên file rồi đoán |
| Một dòng REQ trong `requirements_<module>.md` | Dẫn được REQ ID |
| Một lần thao tác thật qua Playwright MCP trong phiên này | Mode FULL |

Không truy được về nguồn nào → **không viết bước đó**. Gắn `⚠️ Chưa xác minh` và liệt kê ở mục *Vùng chưa xác minh* cuối tài liệu.

### 2. Nhãn nút, tên menu, thông báo — chép NGUYÊN VĂN

Người dùng dò theo chữ trên màn hình. Viết *"nhấn nút Lưu lại"* trong khi nút ghi *"Cập nhật"* là làm người dùng đứng im.

| ❌ Sai | ✅ Đúng |
|---|---|
| Nhấn nút lưu | Nhấn **Cập nhật** |
| Vào phần quản lý khách hàng | Vào **Khách hàng → Danh sách khách hàng** |
| Hệ thống báo thành công | Hệ thống hiện thông báo **"Cập nhật thành công"** |

### 3. Không viết hướng dẫn cho tính năng chưa nhìn thấy

Requirements ghi có nút Export nhưng không ảnh nào có nút đó → **không** viết mục xuất file. Ghi vào *Vùng chưa xác minh*.

### 4. Bước không có kết quả quan sát được là bước chưa viết xong

Mỗi bước quan trọng phải nói **người dùng thấy gì sau khi làm** — đó là cách họ biết mình đang đi đúng.

### 5. Không độn bảng cho "đủ" — mọi mục có kích thước tự nhiên

Cấu trúc tài liệu quy định **mỗi thủ tục có bảng "Nếu không được"**. Điều đó **không** có nghĩa mọi bảng phải nhiều dòng.

| Số dòng có bằng chứng thật | Viết thế nào |
|---|---|
| Nhiều (form có nhiều ràng buộc) | Xếp theo **tần suất người dùng gặp**, không theo thứ tự trong requirements |
| Đúng 1 dòng | ✅ **Hợp lệ.** Giữ nguyên 1 dòng |
| Không có dòng nào | ✅ **Bỏ hẳn bảng.** Thủ tục đơn giản, không có gì hỏng được thì đừng nghĩ ra tình huống hỏng |

🚫 **CẤM thêm dòng chỉ để bảng trông đầy đặn.** Đây là cách bịa dễ lọt nhất trong toàn bộ tài liệu: một dòng lỗi nghe rất hợp lý (*"Không tìm thấy nút Logout trong menu"*) mà không tấm ảnh nào cho thấy tình huống đó xảy ra. Người dùng đọc phải sẽ đi tìm một vấn đề không tồn tại, và mất niềm tin vào những dòng còn lại — kể cả dòng đúng.

**Phép thử:** với mỗi dòng, chỉ ra được **ảnh nào / REQ nào / lần chạy nào** cho thấy người dùng thật sự gặp tình huống đó. Không chỉ ra được thì xoá dòng.

> Cùng nguyên tắc áp dụng cho mục **Câu hỏi thường gặp** (câu hỏi phải là câu người dùng thật sự hỏi, không phải câu tự nghĩ ra để có mục FAQ) và mục **Bảng thuật ngữ** (tài liệu ngắn thì bỏ hẳn mục này).

---

## Nguyên Tắc Viết

### Tối giản (minimalism) — 82079-1

Người dùng mở tài liệu ra khi **đang kẹt**, không đọc từ đầu. Nên:

- Vào thẳng việc. **Không** có mục "Giới thiệu tổng quan về hệ thống" dài 2 trang trước bước đầu tiên
- Mỗi thủ tục ≤ **10 bước**. Dài hơn → tách thành nhiều việc con
- **Không** giải thích cơ chế bên trong (*"hệ thống sẽ gọi API lưu vào cơ sở dữ liệu"*) — người dùng không cần và không muốn biết
- Một việc = một mục đọc độc lập được, không cần đọc mục trước

### Câu lệnh — mỗi bước một hành động

```
✅ 1. Nhấn **Thêm mới**.
   2. Nhập tên khách hàng vào ô **Tên khách hàng**.
   3. Chọn **Nhóm khách hàng** trong danh sách thả xuống.
   4. Nhấn **Lưu**. Hệ thống hiện thông báo **"Thêm mới thành công"** và quay về danh sách.

❌ 1. Người dùng sẽ tiến hành nhấn vào nút Thêm mới, sau đó điền các thông tin
      bắt buộc bao gồm tên, nhóm, số điện thoại rồi nhấn lưu để hệ thống xử lý.
```

- Ngôi thứ hai, giọng mệnh lệnh: *"Nhấn"*, không phải *"Người dùng nhấn"* hay *"Bạn sẽ cần nhấn"*
- Thì hiện tại. Không *"sẽ"*, không *"tiến hành"*, không *"thực hiện việc"*
- Tên phần tử giao diện in **đậm**, chép nguyên văn
- Một bước = một hành động = một số thứ tự

### Thuật ngữ nhất quán

Một khái niệm — **một** từ, xuyên suốt tài liệu. Chọn từ **người dùng dùng**, không phải từ hệ thống dùng. Lập bảng thuật ngữ ở cuối khi tài liệu dài.

> Hệ thống gọi là `Customer`, giao diện hiển thị *"Khách hàng"*, người dùng gọi là *"khách"* → tài liệu dùng **"khách hàng"** (từ trên giao diện), mọi chỗ như nhau.

---

## Cảnh Báo & An Toàn (theo 82079-1)

Đặt cảnh báo **ngay trước** bước gây hậu quả, không gom vào cuối mục.

| Mức | Dùng khi | Ví dụ |
|---|---|---|
| 🛑 **NGUY HIỂM** | Gây thiệt hại người/tài sản thật (thiết bị y tế, điều khiển công nghiệp, chuyển tiền) | 🛑 Lệnh chuyển tiền không thể huỷ sau khi xác nhận. |
| ⚠️ **CẢNH BÁO** | Mất dữ liệu, thao tác không hồi lại được | ⚠️ Xoá khách hàng sẽ xoá toàn bộ hợp đồng liên quan. Không khôi phục được. |
| ❗ **THẬN TRỌNG** | Kết quả sai, phải làm lại | ❗ Kiểm tra kỹ mã số thuế trước khi lưu — sau khi lưu không sửa được. |
| 💡 **Lưu ý** | Thông tin hữu ích, không có rủi ro | 💡 Có thể lọc nhanh bằng cách gõ vào ô tìm kiếm. |

> 🚫 Không dùng 🛑/⚠️ cho mẹo vặt. Lạm dụng cảnh báo → người dùng bỏ qua **mọi** cảnh báo, kể cả cảnh báo thật.

---

## Ảnh Minh Hoạ

| Quy tắc | Lý do |
|---|---|
| Chụp **đúng vùng cần chỉ**, không full-page cả trang dữ liệu | Ảnh full-page kéo theo dữ liệu nghiệp vụ không liên quan tới bước đang hướng dẫn |
| 🔒 **Dữ liệu trong ảnh phải là dữ liệu mẫu** — không tên, email, số điện thoại, số tiền của khách hàng thật | Hướng dẫn sử dụng được **phát cho người ngoài** và thường được commit lên repo. Ràng buộc này nghiêm hơn cả với evidence của requirements |
| Ảnh đặt **ngay dưới** bước nó minh hoạ | Ảnh gom cuối mục thì người dùng không biết nó ứng với bước nào |
| Không cần ảnh cho mọi bước — chỉ nơi **khó tìm** (menu lồng nhau, nút nhỏ, trạng thái đặc biệt) | Ảnh cho bước hiển nhiên làm loãng tài liệu và tăng chi phí bảo trì khi UI đổi |
| Ảnh lưu tại `docs/user-guides/<module>/images/`, tên theo việc: `them-khach-hang-buoc-2.png` | Tên theo việc thì đổi UI biết ngay phải chụp lại tấm nào |
| Dùng lại ảnh từ `requirements/<module>/evidence/` **chỉ khi** ảnh đó đúng phạm vi và không chứa dữ liệu thật | Ảnh recon chụp để chứng minh REQ, mục đích khác — thường thừa và thường có dữ liệu thật |

---

## Cấu Trúc Tài Liệu

```markdown
# Hướng dẫn sử dụng — <Tên hệ thống> · Module <Tên module>

| | |
|---|---|
| Phiên bản tài liệu | v1.0 |
| Áp dụng cho phiên bản phần mềm | v2.4 |
| Đối tượng đọc | Nhân viên kinh doanh (role: Sales) |
| Ngày cập nhật | 2026-09-09 |
| Cấu trúc tài liệu | Biên soạn theo cấu trúc ISO/IEC/IEEE 26514 |

## 1. Trước khi bắt đầu
- Bạn cần có: tài khoản với quyền <role>, trình duyệt <tên + phiên bản>
- Đăng nhập tại: <đường dẫn hệ thống>
- 💡 Nếu chưa có tài khoản: liên hệ <bộ phận>

## 2. Bạn muốn làm gì?          ← MỤC LỤC THEO VIỆC, không theo màn hình
| Tôi muốn… | Xem mục |
|---|---|
| Thêm một khách hàng mới | 3.1 |
| Tìm khách hàng đã có | 3.2 |
| Sửa thông tin khách hàng | 3.3 |
| Xuất danh sách ra Excel | 3.4 |

## 3. Các thao tác

### 3.1 Thêm một khách hàng mới
**Khi nào dùng:** khi có khách hàng mới cần đưa vào hệ thống theo dõi.

1. Vào **Khách hàng → Danh sách khách hàng**.
2. Nhấn **Thêm mới** ở góc trên bên phải.
   ![Nút Thêm mới](images/them-khach-hang-buoc-2.png)
3. Nhập **Tên khách hàng**. Đây là trường bắt buộc.
4. …
5. Nhấn **Lưu**. Hệ thống hiện thông báo **"Thêm mới thành công"** và đưa bạn về danh sách, khách hàng vừa tạo nằm ở đầu danh sách.

⚠️ **CẢNH BÁO:** Mã khách hàng không sửa được sau khi lưu. Kiểm tra kỹ trước khi nhấn **Lưu**.

**Nếu không được:**
| Bạn thấy | Nghĩa là | Làm gì |
|---|---|---|
| Thông báo **"Email đã tồn tại"** | Đã có khách hàng dùng email này | Tìm theo email ở mục 3.2 để kiểm tra trùng |
| Nút **Lưu** mờ, không nhấn được | Còn trường bắt buộc chưa điền | Tìm ô có viền đỏ, điền nốt |

> **Bảng này lấy đúng số dòng mà bằng chứng cho phép — 1 dòng cũng hợp lệ.** Xem quy tắc *Không độn bảng* bên dưới.

### 3.2 …

## 4. Câu hỏi thường gặp
## 5. Bảng thuật ngữ            ← khi tài liệu dài
## 6. Vùng chưa xác minh        ← nội bộ, GỠ trước khi phát hành ra ngoài
| Nội dung | Vì sao chưa xác minh |
|---|---|
| Xuất Excel (mục 3.4) | Requirements có nêu nhưng không có ảnh và không thao tác được — chưa có quyền |
```

> **Mục 6 là mục nội bộ.** Giữ khi lưu trong repo để lần sau biết còn nợ gì; **gỡ** ở bản gửi khách hàng. Đừng gửi khách một tài liệu tự khai chỗ nào chưa kiểm.

---

## Quality Gate (cơ sở: ISO/IEC/IEEE 26513)

Trước khi xuất, tự rà đủ 10 tiêu chí:

- [ ] **1. Tổ chức theo việc, không theo màn hình** — mục lục mục 2 đọc lên là *"Tôi muốn…"*, không phải tên màn hình
- [ ] **2. Mọi bước truy được về nguồn** — ảnh cụ thể / REQ ID / thao tác thật. Không truy được → gắn `⚠️ Chưa xác minh` và liệt kê ở mục 6
- [ ] **3. Nhãn nguyên văn** — mọi tên nút, menu, thông báo chép đúng chữ trên màn hình, in đậm
- [ ] **4. Mỗi bước một hành động**, đánh số, giọng mệnh lệnh, thì hiện tại
- [ ] **5. Bước cuối mỗi thủ tục nói rõ người dùng thấy gì** khi thành công
- [ ] **6. Mỗi thủ tục ≤ 10 bước**; dài hơn thì đã tách
- [ ] **7. Cảnh báo đúng mức, đặt ngay trước bước gây hậu quả** — không lạm dụng 🛑/⚠️
- [ ] **8. 🔒 Ảnh không có dữ liệu thật** — mở lại **từng** ảnh kiểm tra tên người, email, số điện thoại, số tiền. Đây là tiêu chí duy nhất không được bỏ qua vì hết giờ
- [ ] **9. Thuật ngữ nhất quán** — grep một khái niệm, chỉ ra một từ
- [ ] **10. Không độn bảng** — với **từng dòng** của mọi bảng *"Nếu không được"* và mục FAQ, chỉ ra được ảnh / REQ / lần chạy làm bằng chứng. Chỉ không ra → xoá dòng. Bảng 1 dòng hợp lệ; không có dòng nào thì bỏ hẳn bảng

**Phép thử cuối:** đưa tài liệu cho người **chưa từng dùng hệ thống**, bảo họ làm một việc bất kỳ trong mục 2. Họ làm xong mà không hỏi câu nào → đạt. Đây là phép thử của 26513, và không có cách nào thay thế bằng rà soát trên giấy.

---

## Anti-Patterns (NGHIÊM CẤM)

| ❌ Sai | ✅ Đúng |
|---|---|
| Chép `requirements_<module>.md` rồi đổi giọng | Viết lại theo việc người dùng cần làm |
| Tổ chức theo màn hình: *"Màn hình danh sách khách hàng gồm các thành phần…"* | Tổ chức theo việc: *"Thêm một khách hàng mới"* |
| Liệt kê **mọi** ràng buộc validation của mọi trường | Chỉ nêu trường bắt buộc + lỗi người dùng hay gặp |
| Viết hướng dẫn cho tính năng chưa nhìn thấy bao giờ | Ghi vào *Vùng chưa xác minh* |
| *"Nhấn nút lưu"* trong khi nút ghi *"Cập nhật"* | Chép nguyên văn: **Cập nhật** |
| Ảnh full-page có tên và số điện thoại khách hàng thật | Chụp đúng vùng, dùng dữ liệu mẫu |
| Gắn ⚠️ cho mẹo dùng phím tắt | 💡 Lưu ý. Giữ ⚠️ cho mất dữ liệu thật |
| *"Người dùng sẽ tiến hành thực hiện việc nhấn…"* | *"Nhấn **Lưu**."* |
| Giải thích hệ thống gọi API nào, lưu bảng nào | Bỏ hẳn — người dùng không cần |
| Gửi khách bản còn mục *Vùng chưa xác minh* | Gỡ mục 6 ở bản phát hành |
| Thêm dòng lỗi nghe hợp lý vào bảng *"Nếu không được"* cho bảng đỡ trống | Giữ đúng số dòng có bằng chứng — 1 dòng, hoặc bỏ hẳn bảng |
| Nghĩ ra câu hỏi để mục FAQ có nội dung | Chỉ viết câu người dùng thật sự hỏi, hoặc vấn đề đã ghi nhận trong requirements |

---

## Bảo Trì — UI đổi thì sửa gì

Tài liệu hướng dẫn **hỏng âm thầm**: UI đổi, tài liệu vẫn nằm đó trông như còn đúng.

| Đổi gì | Sửa gì |
|---|---|
| Đổi nhãn nút / tên menu | grep nguyên văn nhãn cũ trong toàn bộ `docs/user-guides/` — sửa hết, kể cả ảnh |
| Đổi luồng thao tác | Sửa thủ tục tương ứng, chụp lại ảnh của **các bước đổi**, không chụp lại cả mục |
| Gỡ tính năng | Xoá mục + xoá dòng ở mục lục mục 2 + xoá ảnh không còn ai tham chiếu |
| Thêm tính năng | Thêm mục + **thêm dòng vào mục lục mục 2** — quên dòng này thì người dùng không bao giờ tìm ra |

Mỗi lần sửa: tăng **Phiên bản tài liệu** và cập nhật **Áp dụng cho phiên bản phần mềm** ở đầu file. Hai dòng này là cách duy nhất người đọc biết tài liệu còn dùng được hay không.

---

## Rules References

- Nguồn dữ liệu: `docs/requirements/<module>/` do `skills-requirements-analyzer` sinh — **không** viết hướng dẫn cho module chưa recon
- Quy tắc mở evidence trước khi viết: cùng cơ chế với `skills-rbt-manual-testing` mục *Quy Tắc Đối Chiếu Evidence*
- 🔒 Không đưa giá trị bí mật, dữ liệu khách hàng thật vào `docs/` — `CLAUDE.md` mục 6b
