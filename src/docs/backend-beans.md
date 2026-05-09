# Backend — Beans (`bean`)

CDI kapsamları: `@ViewScoped` sayfa ömrü (Serializable); `@RequestScoped` kısa ömür; `@ApplicationScoped` uygulama geneli (`AdminUiBean`).

## Kimlik ve oturum

### LoginBean (`loginBean`)

[`LoginBean`](../main/java/bean/LoginBean.java) — `@ViewScoped`

- **`login()`**: `UserFacadeLocal.login`; başarıda oturuma `user`, `userId`; başarısız denemeler IP bazlı sayılır (5 deneme / 15 dk kilit); `AuditService.logLogin`.
- **Bağımlılıklar**: `UserFacadeLocal`, `AuditService`, `FacesContext` inject.

### RegisterBean (`registerBean`)

[`RegisterBean`](../main/java/bean/RegisterBean.java) — `@ViewScoped`

- **`register()`**: E-posta/şifre doğrulama; `emailExists` ile çakışma kontrolü; `userFacade.createUser(user)`; başarı mesajı ve `login.xhtml` yönlendirme.

### AuthBean (`authBean`)

[`AuthBean`](../main/java/bean/AuthBean.java) — `@RequestScoped`

- **`logout()`**: `AuditService.log` ile LOGOUT; oturumu invalidate; `login.xhtml` redirect.

### PanelBean (`panelBean`)

[`PanelBean`](../main/java/bean/PanelBean.java) — `@ViewScoped`

- **`isCanReadDashboard`**, **`isCanAccessPanel`**, **`isCanReadAsUserScope`**: `AuthorizationServiceLocal` ile demo izin bayrakları.

## Demo / örnek sayfalar

### FrontendBean (`frontendBean`)

[`FrontendBean`](../main/java/bean/FrontendBean.java) — `@ViewScoped`

- **`getUsers()`**: Sabit demo kullanıcı listesi (veritabanı yok).

### UserBean (`userBean`)

[`UserBean`](../main/java/bean/UserBean.java) — `@ViewScoped`

- **`createUser`, `editUser`, `clearForm`, `updateForm`, `deleteUser`**, liste getter/setter: CRUD demo (`UserFacadeLocal`).

### StudentBean (`studentBean`)

[`StudentBean`](../main/java/bean/StudentBean.java) — `@ViewScoped`

- Öğrenci form state (facade bağlantısı yok; örnek).

## Yardımcı UI

### AdminUiBean (`adminUi`)

[`AdminUiBean`](../main/java/bean/AdminUiBean.java) — `@Named("adminUi")`, `@ApplicationScoped`

- **`initials(AppUser)`**, **`formatShortDate(Instant)`**: Liste ve diyalog sunumu.

### RbacBean (`rbac`)

[`RbacBean`](../main/java/bean/RbacBean.java) — `@RequestScoped`

- **`can(String scopeName, String permissionName)`**: Facelets RBAC.

### AdminDeviceBean (`adminDevice`)

[`AdminDeviceBean`](../main/java/bean/AdminDeviceBean.java) — `@RequestScoped`

- User-Agent’tan kabaca mobil/tablet/desktop ve **`getCategory`**, **`getUserAgentPreview`** — şablonda şu an kullanılmıyor; `rendered="#{adminDevice.mobile}"` gibi için hazır.

## Admin — kullanıcı ve rol

### AdminUsersBean (`adminUsersBean`)

[`AdminUsersBean`](../main/java/bean/AdminUsersBean.java) — `@ViewScoped`

- **`init`, `refresh`**, **`openNew`, `openEdit`, `openDetail`**, **`save`, `softDelete`**
- Rol/grup atama: **`assignPickerRole`**, **`removeAssignedRole`**, **`assignPickerRoleGroup`**, **`removeAssignedRoleGroup`**, picker sayfalama ve arama metodları
- **`canManageAssignments`**, **`profileEditable`**, **`detailMode`** vb.

### AdminRolesBean (`adminRolesBean`)

[`AdminRolesBean`](../main/java/bean/AdminRolesBean.java) — `@ViewScoped`

- **`openDetail`, `openNew`, `openEdit`**, **`save`, `softDelete`**, **`permissionsFor`**, **`getFilteredRoles`**, izin/seçim getter/setter.

### AdminRoleGroupsBean (`adminRoleGroupsBean`)

[`AdminRoleGroupsBean`](../main/java/bean/AdminRoleGroupsBean.java) — `@ViewScoped`

- Grup CRUD, **`linkedRolesTopThree`**, **`filteredAssignableRoles`**, **`save`**, **`softDelete`**.

## Admin — muhasebe

### AdminDashboardBean (`adminDashboardBean`)

[`AdminDashboardBean`](../main/java/bean/AdminDashboardBean.java) — `@ViewScoped`

- **`init`, `refresh`**: KPI ve grafik verilerini facade’lerden toplar.
- **`formatCurrency`**, KPI getter’lar (`totalCashBalance`, `pendingExpenseCount`, …).
- **`getCashFlowChart`, `getIncomeExpenseChart`**, **`getAlertMessages`**, **`isAllClear`**.

### AdminCurrentAccountBean (`adminCurrentAccountBean`)

[`AdminCurrentAccountBean`](../main/java/bean/AdminCurrentAccountBean.java) — `@ViewScoped`

- **`openNew`, `openEdit`, `save`, `softDelete`**, **`typeOptions`**, **`formatCurrency`**.

### AdminJournalEntryBean (`adminJournalEntryBean`)

[`AdminJournalEntryBean`](../main/java/bean/AdminJournalEntryBean.java) — `@ViewScoped`

- **`openNew`, `openEdit`**, **`addLine`, `removeLine`**, **`save`**
- **`post`, `cancel`, `softDelete`** (satır fişleri)
- **`totalDebit`, `totalCredit`, `balanced`**, **`completeCurrentAccount`**.

### AdminCashAccountBean (`adminCashAccountBean`)

[`AdminCashAccountBean`](../main/java/bean/AdminCashAccountBean.java) — `@ViewScoped`

- **`openNewAccount`, `saveAccount`, `selectAccount`**, **`openNewTransaction`, `saveTransaction`**
- **`completeCurrentAccount`**, **`formatCurrency`**, **`transactionBadgeClass`**.

### AdminBankAccountBean (`adminBankAccountBean`)

[`AdminBankAccountBean`](../main/java/bean/AdminBankAccountBean.java) — `@ViewScoped`

- Kasa bean ile aynı desen; banka hesabı ve banka işlemleri.

### AdminProductBean (`adminProductBean`)

[`AdminProductBean`](../main/java/bean/AdminProductBean.java) — `@ViewScoped`

- **`openNew`, `openEdit`, `save`, `softDelete`**, **`openStockMovement`**, **`formatCurrency`**, **`unitOptions`**.

### AdminStockMovementBean (`adminStockMovementBean`)

[`AdminStockMovementBean`](../main/java/bean/AdminStockMovementBean.java) — `@ViewScoped`

- **`refreshMovements`, `saveMovement`**, **`completeProduct`**, **`movementTypeLabel`, `movementBadgeClass`**, **`formatCurrency`**.

### AdminChequeBean (`adminChequeBean`)

[`AdminChequeBean`](../main/java/bean/AdminChequeBean.java) — `@ViewScoped`

- **`openNew`, `save`**, **`deposit`, `collect`, `returnCheque`, `protest`, `pay`**
- **`completeCurrentAccount`, `completeBankAccount`**, rozet yardımcıları, **`canDeposit`** vb.

### AdminExpenseBean (`adminExpenseBean`)

[`AdminExpenseBean`](../main/java/bean/AdminExpenseBean.java) — `@ViewScoped`

- **`onTabChange`, `refresh`, `openNew`, `save`**, **`approve`, `reject`, `softDelete`**
- Filtre/tablo/kategori getter’ları; kasa/banka seçimi.

### AdminReportBean (`adminReportBean`)

[`AdminReportBean`](../main/java/bean/AdminReportBean.java) — `@ViewScoped`

- **`init`**: `RbacProcedureBean.require(REPORT, ACCESS)`.
- **`generateReport`**: `REPORT` + `READ`; rapor tipine göre `ReportFacadeLocal`.
- **`exportToPdf`, `exportToExcel`**: İstemci tarafı tetikleme yardımcıları.
- **`getRows`, `getSummaryMap`**, **`getReportTypeLabel`**, **`formatCurrency`**.

### AdminAuditLogBean (`adminAuditLogBean`)

[`AdminAuditLogBean`](../main/java/bean/AdminAuditLogBean.java) — `@ViewScoped`

- **`init`, `refresh`**, **`badgeClass`, `shortId`**, filtre alanları; liste `AuditService.findAll`.

---

Bean başına tüm getter/setter burada tekrarlanmaz; IDE veya kaynak dosya tek doğruluk kaynağıdır.
