# 06 — Git chuyên nghiệp: commit chuẩn, branch, tag, CI/CD

> Note tra cứu. Không cần đọc một lượt — tra theo mục khi cần.
> Mục tiêu: dùng Git như team quốc tế, và trả lời được câu hỏi phỏng vấn về Git.

---

## 1. Commit message chuẩn — Conventional Commits

Chuẩn phổ biến nhất hiện nay, được Angular, Vue, và phần lớn dự án mã nguồn mở dùng.

### Cấu trúc

```
<type>(<scope>): <subject>
                                    ← dòng trống BẮT BUỘC
<body — giải thích TẠI SAO, không phải CÁI GÌ>

<footer — breaking change, mã ticket>
```

Ví dụ thật:

```
fix(ledger): prevent negative balance under concurrent transfers

Two concurrent transfers could both read the same balance before
either committed, letting the account go negative. Added a
SELECT ... FOR UPDATE on the account row so the second transfer
blocks until the first commits.

Closes #142
```

### Các `type` chuẩn

| Type | Dùng khi |
|---|---|
| `feat` | thêm tính năng mới cho **người dùng** |
| `fix` | sửa bug |
| `docs` | chỉ thay đổi tài liệu |
| `style` | format, dấu chấm phẩy, khoảng trắng — **không đổi logic** |
| `refactor` | đổi code nhưng **không** thêm tính năng, **không** sửa bug |
| `perf` | cải thiện hiệu năng |
| `test` | thêm/sửa test |
| `build` | hệ thống build, dependency (Maven, npm) |
| `ci` | cấu hình CI (GitHub Actions…) |
| `chore` | việc vặt không thuộc các loại trên |
| `revert` | hoàn tác một commit trước đó |

**`scope`** (tuỳ chọn) = vùng bị ảnh hưởng: `feat(ledger)`, `fix(api)`, `ci(github-actions)`.

### Bảy quy tắc cho dòng subject

1. **Thức mệnh lệnh** (imperative): `add`, không phải `added`/`adds`.
   Mẹo kiểm tra: *"If applied, this commit will \_\_\_"* → `...will add retry logic` ✅
2. **Không quá 50 ký tự** (tối đa 72).
3. **Không có dấu chấm** ở cuối.
4. **Chữ thường** sau dấu hai chấm.
5. **Body cách subject một dòng trống** — thiếu dòng này thì Git coi tất cả là subject.
6. **Body xuống dòng ở cột 72.**
7. **Body giải thích TẠI SAO, không phải CÁI GÌ.** Cái gì thì đọc diff là biết; tại sao thì chỉ có trong đầu bạn lúc đó.

### Breaking change

```
feat(api)!: change payment response to include fee breakdown

BREAKING CHANGE: the `amount` field is now an object with
`gross`, `fee`, and `net` instead of a plain number.
```

Dấu `!` sau scope **và** footer `BREAKING CHANGE:` → công cụ tự động tăng số MAJOR.

### So sánh

| ❌ Tệ | ✅ Tốt |
|---|---|
| `update code` | `refactor(ledger): extract balance check into its own method` |
| `fix bug` | `fix(api): return 409 instead of 500 on duplicate request id` |
| `Added new feature for payments.` | `feat(payments): add idempotency key support` |
| `asdfgh` | *(không bao giờ)* |

### Vì sao phải theo chuẩn

- **Changelog tự sinh** — công cụ đọc `feat`/`fix` để dựng release note.
- **Semantic versioning tự động** — `fix` → tăng PATCH, `feat` → MINOR, `BREAKING CHANGE` → MAJOR.
- **Lịch sử tra cứu được** — `git log --grep="^fix(ledger)"`.
- **Review nhanh hơn** — reviewer biết ngay phạm vi trước khi mở diff.

---

## 2. Atomic commit — một commit một việc

> **Mỗi commit là một thay đổi logic hoàn chỉnh, tự đứng được một mình.**

Vì sao quan trọng:

- **Revert được** — muốn bỏ tính năng X mà không đụng tính năng Y.
- **`git bisect` hoạt động** — tìm commit gây lỗi bằng nhị phân; commit lẫn lộn thì vô dụng.
- **Review được** — 1 commit 50 dòng thì review kỹ; 1 commit 3000 dòng thì bấm Approve cho xong.

Công cụ tách thay đổi trong cùng một file:

```bash
git add -p          # duyệt từng đoạn, chọn đoạn nào vào commit này
```

**Sai lầm phổ biến:** sửa bug, tiện tay format lại file, tiện tay đổi tên biến — rồi commit một cục. Reviewer không tách nổi phần nào là sửa bug thật.

---

## 3. Chiến lược chia branch

### Ba trường phái

| | **Git Flow** | **GitHub Flow** | **Trunk-Based** |
|---|---|---|---|
| Branch dài hạn | `main` + `develop` | chỉ `main` | chỉ `main` |
| Branch tạm | `feature/`, `release/`, `hotfix/` | `feature/` | branch rất ngắn (< 1 ngày) |
| Deploy khi nào | theo đợt release | mỗi lần merge vào `main` | liên tục |
| Độ phức tạp | Cao | Thấp | Thấp nhưng cần kỷ luật |
| Hợp với | phần mềm có phiên bản, on-premise | web app, SaaS | team mạnh, CI tốt |
| Nhược | nặng nề, merge đau | không hợp nhiều phiên bản song song | cần feature flag + test tự động tốt |

**Xu hướng hiện nay:** phần lớn team hiện đại dùng **GitHub Flow** hoặc **Trunk-Based**. Git Flow bị coi là quá nặng cho sản phẩm web — chính tác giả của nó cũng đã viết ghi chú khuyên cân nhắc.

### Quy ước đặt tên branch

```
feat/ledger-double-entry
fix/duplicate-callback-500
chore/bump-spring-boot-4.1
docs/adr-outbox-pattern

# team có ticket:
feature/PAY-1234-idempotency-key
```

Quy tắc: **chữ thường, gạch ngang, có tiền tố, mô tả được việc gì.** Không đặt `test`, `mybranch`, `fix2`.

### Branch sống càng ngắn càng tốt

Branch để lâu = code trên `main` chạy xa dần = merge càng ngày càng đau. Gọi là **merge hell**. Mục tiêu: branch sống dưới **2–3 ngày**.

---

## 4. Merge vs Rebase — câu hỏi phỏng vấn kinh điển

### `git merge`

Tạo một **merge commit** có hai cha. Lịch sử **giữ nguyên sự thật**: branch này rẽ ra lúc nào, nhập lại lúc nào.

```
main    A───B───────M
             \     /
feat          C───D
```

### `git rebase`

**Viết lại** commit của bạn thành **commit mới** đặt lên đầu `main`. Lịch sử thành đường thẳng.

```
main    A───B───C'───D'
```

⚠️ `C'` và `D'` là **commit MỚI**, hash khác `C` và `D`. Bản cũ không mất ngay nhưng không còn nằm trong nhánh.

### So sánh

| | `merge` | `rebase` |
|---|---|---|
| Lịch sử | có nhánh, đúng sự thật | thẳng, dễ đọc |
| Hash commit | giữ nguyên | **thay đổi** |
| An toàn với branch đã đẩy lên | ✅ | ❌ |
| Truy vết "code này từ PR nào" | dễ | khó hơn |

### 🔴 Quy tắc vàng của rebase

> **Không bao giờ rebase một branch mà người khác đang dùng.**

Vì rebase đổi hash → lịch sử của bạn và của họ khác nhau → họ `pull` sẽ ra một đống rối. Rebase chỉ dùng cho branch **riêng của bạn, chưa ai khác dựa vào**.

### Cách dùng thực tế phổ biến nhất

```bash
git switch feat/my-feature
git fetch origin
git rebase origin/main      # dọn branch riêng cho thẳng, TRƯỚC khi mở PR
```

Rồi merge PR vào `main` bằng merge commit hoặc squash. Vừa sạch, vừa không phá lịch sử chung.

### Interactive rebase — dọn dẹp trước khi mở PR

```bash
git rebase -i HEAD~5
```

Trong trình soạn thảo, đổi `pick` thành:

| Lệnh | Tác dụng |
|---|---|
| `reword` | sửa commit message |
| `squash` | gộp vào commit trước, **giữ cả hai message** |
| `fixup` | gộp vào commit trước, **vứt message** |
| `drop` | xoá commit |
| `edit` | dừng lại để sửa nội dung |

Dùng để biến 8 commit lộn xộn (`wip`, `fix typo`, `oops`) thành 2 commit sạch trước khi cho người khác xem.

### Ba kiểu merge PR trên GitHub

| Kiểu | Kết quả | Hợp khi |
|---|---|---|
| **Merge commit** | giữ mọi commit + 1 merge commit | muốn lịch sử đầy đủ |
| **Squash and merge** | gộp cả PR thành **1 commit** trên `main` | phổ biến nhất — `main` sạch, 1 PR = 1 commit |
| **Rebase and merge** | đặt từng commit lên `main`, không merge commit | muốn thẳng nhưng giữ từng commit |

---

## 5. Tag và Semantic Versioning

### Hai loại tag

```bash
git tag v1.0.0                              # lightweight — chỉ là con trỏ, ĐỪNG dùng cho release
git tag -a v1.0.0 -m "Release 1.0.0"        # annotated — có tác giả, ngày, message, ký được
git push origin v1.0.0                      # tag KHÔNG tự đẩy lên, phải đẩy riêng
git push origin --tags                      # đẩy tất cả
```

> **Release luôn dùng annotated (`-a`).** Nó là một object thật trong Git, có metadata; lightweight chỉ là cái nhãn dán.

### SemVer — `MAJOR.MINOR.PATCH`

| Phần | Tăng khi | Ví dụ |
|---|---|---|
| **MAJOR** | thay đổi phá vỡ tương thích | `1.4.2` → `2.0.0` |
| **MINOR** | thêm tính năng, **vẫn tương thích ngược** | `1.4.2` → `1.5.0` |
| **PATCH** | sửa bug, tương thích ngược | `1.4.2` → `1.4.3` |

Tiền phát hành: `2.0.0-rc.1`, `2.0.0-beta.3`.

**Nối với mục 1:** commit `fix` → PATCH · `feat` → MINOR · `BREAKING CHANGE` → MAJOR. Đó là lý do Conventional Commits và SemVer luôn đi cùng nhau — công cụ đọc commit để tự quyết số phiên bản.

---

## 6. Các lệnh ngoài commit/push/pull/merge

### `reset` vs `revert` — phân biệt sống còn

| | `git reset` | `git revert` |
|---|---|---|
| Làm gì | **dời** con trỏ branch về sau, xoá commit khỏi lịch sử | tạo **commit mới** làm ngược lại commit cũ |
| Lịch sử | **bị viết lại** | được giữ nguyên, chỉ thêm vào |
| Dùng cho | branch **riêng, chưa push** | branch **chung, đã push** |

```bash
git reset --soft HEAD~1    # bỏ commit, GIỮ thay đổi trong staging
git reset --mixed HEAD~1   # bỏ commit, giữ thay đổi ở working dir (mặc định)
git reset --hard HEAD~1    # bỏ commit VÀ XOÁ thay đổi ⚠️ mất dữ liệu

git revert abc123          # an toàn trên branch chung — thêm commit đảo ngược
```

> **Quy tắc:** đã đẩy lên remote chung → **luôn dùng `revert`**, không bao giờ `reset`.

### `stash` — cất tạm việc đang làm

```bash
git stash push -m "wip ledger"   # cất, working dir sạch trở lại
git stash list
git stash pop                    # lấy ra và xoá khỏi stash
git stash apply                  # lấy ra nhưng giữ trong stash
```

Dùng khi đang code dở mà phải nhảy sang branch khác gấp.

### `cherry-pick` — bê một commit sang branch khác

```bash
git cherry-pick abc123
```

Điển hình: hotfix vừa sửa trên `main`, cần đưa sang nhánh release cũ.

### `reflog` — mạng lưới an toàn

```bash
git reflog                 # nhật ký MỌI lần HEAD dịch chuyển, kể cả commit đã "mất"
git reset --hard abc123    # quay về trạng thái tìm được trong reflog
```

> Lỡ `reset --hard` mất commit? **Chưa mất đâu.** `reflog` giữ khoảng 90 ngày. Đây là câu trả lời ăn điểm khi phỏng vấn hỏi *"lỡ xoá commit thì làm sao"*.

### `bisect` — tìm commit gây lỗi bằng nhị phân

```bash
git bisect start
git bisect bad                 # commit hiện tại đang lỗi
git bisect good v1.2.0         # bản này thì tốt
# Git checkout commit ở giữa → bạn test → trả lời good/bad
git bisect good     (hoặc bad)
# lặp lại... 1000 commit chỉ cần ~10 lần thử
git bisect reset
```

Đây là lý do **atomic commit** ở mục 2 quan trọng: commit lẫn lộn thì bisect chỉ ra một cục 3000 dòng, vô dụng.

### `blame` — ai sửa dòng này, ở commit nào

```bash
git blame -L 40,60 src/main/java/.../PaymentService.java
```

Dùng để **tìm ngữ cảnh** (đọc commit message của dòng đó), không phải để đổ lỗi.

### `switch` / `restore` — lệnh mới, rõ nghĩa hơn `checkout`

```bash
git switch main                 # đổi branch          (thay cho: git checkout main)
git switch -c feat/new          # tạo + đổi branch    (thay cho: git checkout -b)
git restore file.java           # bỏ thay đổi ở file  (thay cho: git checkout -- file)
git restore --staged file.java  # bỏ khỏi staging     (thay cho: git reset HEAD file)
```

`checkout` làm quá nhiều việc khác nhau nên gây nhầm. Git đã tách ra thành hai lệnh rõ nghĩa. **Dùng `switch`/`restore` trong code mới.**

### `worktree` — nhiều branch cùng lúc trên đĩa

```bash
git worktree add ../hotfix main
```

Mở thêm một thư mục làm việc cho branch khác mà không phải stash. Hữu ích khi đang dở việc mà cần hotfix gấp.

---

## 7. Git và CI/CD

### Branch protection — cấu hình trên GitHub, không phải trong Git

Cài trên `main`:

- ❌ Cấm push thẳng — mọi thay đổi phải qua **Pull Request**
- ✅ Bắt buộc CI xanh mới merge được (`required status checks`)
- ✅ Bắt buộc ≥1 người approve
- ✅ Bắt buộc branch cập nhật với `main` trước khi merge
- ❌ Cấm force-push

> Đây là câu hỏi phỏng vấn hay gặp: *"làm sao ngăn code hỏng vào `main`?"* → branch protection + required checks, **không** phải trông vào kỷ luật con người.

### Luồng làm việc chuẩn

```
1. git switch -c feat/xyz         tạo branch từ main
2. commit nhỏ, atomic, message chuẩn
3. git rebase origin/main         dọn cho thẳng trước khi mở PR
4. git push -u origin feat/xyz
5. Mở Pull Request
6. CI tự chạy: lint → test → build
7. Người khác review
8. Squash and merge vào main
9. Xoá branch
```

### Sự kiện Git nào kích hoạt cái gì

| Sự kiện Git | CI/CD thường làm gì |
|---|---|
| Push lên branch bất kỳ | chạy lint + unit test |
| Mở/cập nhật Pull Request | chạy full test, báo trạng thái lên PR |
| Merge vào `main` | build + deploy lên môi trường staging |
| **Đẩy tag `v*`** | build artifact + deploy **production** + tạo GitHub Release |

Đó là lý do tag không chỉ là cái nhãn — nó là **cò súng cho release**.

### Git hooks — chặn lỗi trước khi commit

Script chạy tự động ở các thời điểm nhất định:

| Hook | Chạy khi | Dùng để |
|---|---|---|
| `pre-commit` | trước khi commit thành hình | chạy formatter, linter |
| `commit-msg` | sau khi nhập message | **kiểm tra đúng Conventional Commits** |
| `pre-push` | trước khi push | chạy test |

Công cụ phổ biến: **commitlint** (kiểm message), **husky** (quản hook, hệ JS), **pre-commit** (hệ Python), **Spotless** (format Java trong Maven).

> ⚠️ Hook nằm ở máy cá nhân, người khác có thể bỏ qua bằng `--no-verify`. **Hook là để giúp mình, CI mới là chốt chặn thật.**

### Tự động hoá release

Có Conventional Commits rồi thì công cụ (`semantic-release`, `release-please`, `maven-semantic-release`) tự làm được:

1. Đọc mọi commit từ tag gần nhất
2. Tính số phiên bản mới theo SemVer (`fix`→PATCH, `feat`→MINOR, `BREAKING`→MAJOR)
3. Sinh CHANGELOG
4. Tạo tag + GitHub Release
5. Deploy

Không ai gõ số phiên bản bằng tay nữa. **Đây là câu trả lời đầy đủ cho "vì sao commit message phải chuẩn".**

---

## Câu hỏi phỏng vấn từ phần này

1. `merge` khác `rebase` thế nào? Khi nào **không được** rebase? → *khi branch đã chia sẻ với người khác*
2. `reset` khác `revert` thế nào? Trên branch chung dùng cái nào? → *`revert`*
3. `reset --soft` / `--mixed` / `--hard` khác nhau ra sao?
4. Lỡ `reset --hard` mất commit thì cứu bằng gì? → *`git reflog`*
5. Conventional Commits là gì, mang lại lợi ích gì? → *changelog + versioning tự động*
6. Vì sao commit phải atomic? → *revert, bisect, review*
7. `git bisect` dùng để làm gì?
8. Tag lightweight khác annotated thế nào? Release dùng loại nào?
9. Semantic Versioning — khi nào tăng MAJOR/MINOR/PATCH?
10. Git Flow vs GitHub Flow vs Trunk-Based — chọn cái nào cho SaaS, vì sao?
11. Làm sao ngăn code hỏng vào `main`? → *branch protection + required status checks*
12. `git fetch` khác `git pull` thế nào? → *`pull` = `fetch` + `merge` (hoặc `rebase`)*
13. `cherry-pick` dùng khi nào?
14. Git hook có đủ để đảm bảo chất lượng không? → *không, bỏ qua được bằng `--no-verify`; CI mới là chốt chặn*

---

## Áp dụng vào chính repo này

Hiện tại repo mới dùng `main` + commit thẳng. Từ Phase 1 sẽ nâng dần:

- [ ] Mỗi increment = một branch `feat/...`, mở PR, tự review, squash merge
- [ ] Bật branch protection trên `main`
- [ ] GitHub Actions: Maven build + test chạy trên mỗi PR *(Phase 1)*
- [ ] `commitlint` kiểm Conventional Commits
- [ ] Đánh tag `v0.1.0` khi Phase 1 xong

## Liên quan

- [../PROGRESS.md](../PROGRESS.md) — tiến độ theo phase
- Sách: **Pro Git** — Chacon & Straub (miễn phí). Ch.3 Branching · Ch.7.6 Rewriting History · Ch.7.7 Reset Demystified
