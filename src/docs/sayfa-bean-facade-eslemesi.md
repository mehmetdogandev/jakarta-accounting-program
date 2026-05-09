# Sayfa — bean — facade eşlemesi

CDI varsayılan bean adı: sınıf adının ilk harfi küçük (`AdminUsersBean` → `adminUsersBean`). Özel isimler: `rbac`, `adminUi`, `authBean`, `adminDevice`.

## Özet tablo

| XHTML (view) | Ana CDI bean(ler) | Bağlı EJB facade / servis (bean üzerinden) |
|----------------|-------------------|---------------------------------------------|
| `/index.xhtml` | `frontendBean` | `UserFacadeLocal` |
| `/login.xhtml` | `loginBean` | `UserFacadeLocal`, `AuditService` (login akışı) |
| `/register.xhtml` | `registerBean` | `UserFacadeLocal` |
| `/user.xhtml` | `userBean` | `UserFacadeLocal` |
| `/student.xhtml` | `studentBean` | (örnek form; facade bağlantısı bean koduna bakınız) |
| `/panel/index.xhtml` | `panelBean` | `AuthorizationServiceLocal` |
| `/app/index.xhtml` | — | Statik içerik |
| `/admin/dashboard.xhtml` | `adminDashboardBean`, `rbac` | `CashAccountFacadeLocal`, `BankAccountFacadeLocal`, `CurrentAccountFacadeLocal`, `ExpenseFacadeLocal`, `ProductFacadeLocal`, `StockMovementFacadeLocal`, `ChequeFacadeLocal`, `ReportFacadeLocal` |
| `/admin/users.xhtml` | `adminUsersBean`, `adminUi`, `rbac` | `UserFacadeLocal`, `UserAssignmentFacadeLocal`, `RoleFacadeLocal`, `RoleGroupFacadeLocal` |
| `/admin/roles.xhtml` | `adminRolesBean`, `rbac` | `RoleFacadeLocal` |
| `/admin/role-groups.xhtml` | `adminRoleGroupsBean`, `rbac` | `RoleGroupFacadeLocal`, `RoleFacadeLocal` |
| `/admin/audit-log.xhtml` | `adminAuditLogBean`, `rbac` | `AuditService` |
| `/admin/current-accounts.xhtml` | `adminCurrentAccountBean`, `rbac` | `CurrentAccountFacadeLocal` |
| `/admin/journal-entries.xhtml` | `adminJournalEntryBean`, `rbac` | `JournalEntryFacadeLocal`, `CurrentAccountFacadeLocal` |
| `/admin/cash-accounts.xhtml` | `adminCashAccountBean`, `rbac` | `CashAccountFacadeLocal`, `CurrentAccountFacadeLocal` |
| `/admin/bank-accounts.xhtml` | `adminBankAccountBean`, `rbac` | `BankAccountFacadeLocal`, `CurrentAccountFacadeLocal` |
| `/admin/products.xhtml` | `adminProductBean`, `rbac` | `ProductFacadeLocal`, `StockMovementFacadeLocal` |
| `/admin/stock-movements.xhtml` | `adminStockMovementBean`, `rbac` | `ProductFacadeLocal`, `StockMovementFacadeLocal` |
| `/admin/cheques.xhtml` | `adminChequeBean`, `rbac` | `ChequeFacadeLocal`, `CurrentAccountFacadeLocal`, `BankAccountFacadeLocal` |
| `/admin/expenses.xhtml` | `adminExpenseBean`, `rbac` | `ExpenseFacadeLocal`, `CashAccountFacadeLocal`, `BankAccountFacadeLocal`, `CurrentAccountFacadeLocal` |
| `/admin/reports.xhtml` | `adminReportBean`, `rbac` | `ReportFacadeLocal`, `RbacProcedureBean` |

## Şablon ve ortak bean’ler

- Tüm `/admin/*` sayfaları [`admin.xhtml`](../main/webapp/WEB-INF/templates/admin.xhtml) şablonunu kullanır.
- [`admin-sidebar.xhtml`](../main/webapp/WEB-INF/includes/admin-sidebar.xhtml) her admin sayfasında dahil edilir; `rbac`, `authBean` kullanır.

## Converter kullanımı

Formlarda cari/ürün/banka nesne seçimi için JSF `converter` id’leri kullanılır (ör. `currentAccountConverter`). Ayrıntı: [backend-diger.md](backend-diger.md).
