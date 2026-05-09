# Veritabanı migration (Flyway)

Migration dizini: [`src/main/resources/db/migration`](../main/resources/db/migration). Dosya adı sırası uygulanır.

| Dosya | Özet |
|-------|------|
| V1__initial_schema.sql | `app_permission` / `app_scope` enum’ları; `app_user`, `session`, `account`, `verification`, `role`, `role_group`, `role_permission`, `user_role`, `user_role_group`, `role_group_role`, `student` |
| V2__admin_scope_roles.sql | Rol modülü için ek roller ve seed kullanıcıya atama |
| V3__super_admin_rbac_seed.sql | Granüler RBAC; süper admin rol grubu; seed admin güncelleme |
| V4__current_account_scope.sql | `app_scope` genişletme: `CURRENT_ACCOUNT` |
| V5__current_account_seed.sql | Cari ile ilgili seed |
| V6__current_account_table.sql | `current_account` tablosu |
| V7__journal_entry_scope.sql | `JOURNAL_ENTRY` scope |
| V8__journal_entry_seed.sql | Fiş seed |
| V9__journal_entry_tables.sql | `journal_entry`, `journal_entry_line` |
| V10__cash_bank_scope.sql | `CASH_ACCOUNT`, `BANK_ACCOUNT` scope |
| V11__cash_bank_seed.sql | Kasa/banka seed |
| V12__cash_bank_accounts_tables.sql | `cash_account`, `bank_account` |
| V13__cash_bank_transactions_tables.sql | `cash_transaction`, `bank_transaction` |
| V14__product_scope.sql | `PRODUCT`, `STOCK_MOVEMENT` scope |
| V15__product_seed.sql | Ürün seed |
| V16__product_tables.sql | `product_category`, `product`, `stock_movement` |
| V17__cheque_scope.sql | `CHEQUE` scope |
| V18__cheque_seed.sql | Çek seed |
| V19__cheque_table.sql | `cheque` |
| V20__expense_income_scope.sql | `EXPENSE`, `INCOME` scope |
| V21__expense_income_seed.sql | Gelir/gider seed |
| V22__expense_income_tables.sql | `expense_category`, `expense` |
| V23__report_scope.sql | `REPORT` scope |
| V24__report_seed.sql | Rapor seed |
| V25__audit_scope.sql | `AUDIT_LOG` scope |
| V26__audit_seed.sql | Denetim seed |
| V27__audit_log_table.sql | `audit_log` tablosu ve indeksler |

**Not:** Tam DDL için ilgili `.sql` dosyasına bakınız. [`persistence.xml`](../main/resources/META-INF/persistence.xml) içinde EclipseLink DDL otomatik oluşturma kapalıdır (yorum satırı).
