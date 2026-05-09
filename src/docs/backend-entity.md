# Backend — Entity (`entity`)

Paket: `entity`. JPA varlıkları `testPU` altında taranır (`exclude-unlisted-classes=false`). Özet: tablo adı, rol; alan listesi tam sınıf kaynağında.

| Sınıf | Tablo / not | İlişkiler ve önemli alanlar |
|-------|-------------|-----------------------------|
| [`AppUser`](../main/java/entity/AppUser.java) | `app_user` | String `id` (UUID); email, şifre; denetim alanları; soft delete |
| [`Role`](../main/java/entity/Role.java) | `role` | `name`, `scope` (enum); `role_permission` ile bağlantı (DB) |
| [`RoleGroup`](../main/java/entity/RoleGroup.java) | `role_group` | Başlık/açıklama; üyelik `role_group_role`, `user_role_group` |
| [`UserRole`](../main/java/entity/UserRole.java) | `user_role` | Kullanıcı–rol doğrudan atama |
| [`UserRoleGroup`](../main/java/entity/UserRoleGroup.java) | `user_role_group` | Kullanıcı–rol grubu |
| [`RoleGroupRole`](../main/java/entity/RoleGroupRole.java) | `role_group_role` | Grup–rol köprü tablosu |
| [`CurrentAccount`](../main/java/entity/CurrentAccount.java) | `current_account` | Cari kod, tip, bakiye, kredi limiti; soft delete |
| [`JournalEntry`](../main/java/entity/JournalEntry.java) | `journal_entry` | Fiş no, tarih, durum, toplam borç/alacak; satırlar `JournalEntryLine` |
| [`JournalEntryLine`](../main/java/entity/JournalEntryLine.java) | `journal_entry_line` | Hesap kodu/adı, borç/alacak; opsiyonel `CurrentAccount` |
| [`CashAccount`](../main/java/entity/CashAccount.java) | `cash_account` | Kod, bakiye, para birimi |
| [`BankAccount`](../main/java/entity/BankAccount.java) | `bank_account` | IBAN, şube, bakiye |
| [`CashTransaction`](../main/java/entity/CashTransaction.java) | `cash_transaction` | Kasa hareketi; opsiyonel cari |
| [`BankTransaction`](../main/java/entity/BankTransaction.java) | `bank_transaction` | Banka hareketi; opsiyonel cari |
| [`ProductCategory`](../main/java/entity/ProductCategory.java) | `product_category` | Hiyerarşi `parent` |
| [`Product`](../main/java/entity/Product.java) | `product` | Stok, fiyatlar; `ProductCategory`; `StockMovement` koleksiyonu |
| [`StockMovement`](../main/java/entity/StockMovement.java) | `stock_movement` | Ürün, miktar, birim maliyet, hareket tipi |
| [`Cheque`](../main/java/entity/Cheque.java) | `cheque` | Cari, banka hesabı, durum, vadeler |
| [`ExpenseCategory`](../main/java/entity/ExpenseCategory.java) | `expense_category` | Gelir/gider tipi ile kategori |
| [`Expense`](../main/java/entity/Expense.java) | `expense` | İşlem tipi (gelir/gider), tutar, KDV, cari, kasa/banka, onay |
| [`AuditLog`](../main/java/entity/AuditLog.java) | `audit_log` | Kullanıcı, aksiyon, entity tip/id, eski/yeni değer (metin) |
| [`Student`](../main/java/entity/Student.java) | JPA varsayılan tablo adı `Student` (şemada `student` — migrasyonla uyumlu kullanım için entity güncellemesi gerekebilir) | `AppUser` ile `@ManyToOne`; öğrenci numarası |

**Not:** `Student` sınıfında açık `@Table` yok; üretim şemasında [`V1__initial_schema.sql`](../main/resources/db/migration/V1__initial_schema.sql) `student` tablosu tanımlıdır. JPA varsayılan tablo adı ile şema uyumu için entity üzerinde `@Table(name = "student")` kullanımı değerlendirilmelidir.

İş mantığı çoğunlukla facade katmanında; entity’lerde özel metodlar sınırlıdır (çoğu getter/setter).
