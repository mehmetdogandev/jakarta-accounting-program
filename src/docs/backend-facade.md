# Backend — Facade (`facade` / `facadeLocal`)

## AbstractFacade

[`AbstractFacade`](../main/java/facade/AbstractFacade.java): `@Stateless` soyut sınıf değildir ama ortak `@PersistenceContext(unitName = "testPU") EntityManager entityManager` sağlar.

## Local arayüzler

[`facadeLocal/*FacadeLocal.java`](../main/java/facadeLocal) EJB yerel görünümleridir; ilgili `facade/*Facade.java` sınıfları bu arayüzleri **implement** eder ve CDI/`@EJB` ile bean’lere enjekte edilir. İmzalar implementasyonla birebir aynıdır.

## UserFacade

[`UserFacade`](../main/java/facade/UserFacade.java) → [`UserFacadeLocal`](../main/java/facadeLocal/UserFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `createUser(AppUser u)` | Yeni kullanıcı |
| `editUser(AppUser entity)` | Güncelleme |
| `remove(AppUser entity)` | Silme |
| `usersList()` | Liste |
| `login(String email, String password)` | Kimlik doğrulama |
| `findById(String id)` | Id ile bul |
| `emailExists(String email, String excludeUserId)` | E-posta çakışması |
| `softDeleteUser(String targetUserId, String actorUserId)` | Soft delete |

## RoleFacade

[`RoleFacade`](../main/java/facade/RoleFacade.java) → [`RoleFacadeLocal`](../main/java/facadeLocal/RoleFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `listByScope(Scope scope)` | Kapsama göre roller |
| `findById(UUID id)` | Tek rol |
| `listPermissions(UUID roleId)` | Rolün izinleri |
| `save(Role role, List<Permission> permissions, String actorUserId)` | Rol + izinler kaydı |
| `softDelete(UUID roleId, String actorUserId)` | Soft delete |

## RoleGroupFacade

[`RoleGroupFacade`](../main/java/facade/RoleGroupFacade.java) → [`RoleGroupFacadeLocal`](../main/java/facadeLocal/RoleGroupFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `listActive()` | Aktif gruplar |
| `findById(UUID id)` | Grup |
| `linkedRoleIds(UUID groupId)` | Bağlı rol id’leri |
| `listRolesForAssignment()` | Atama için rol listesi |
| `save(RoleGroup group, List<UUID> roleIds, String actorUserId)` | Grup ve üye roller |
| `softDelete(UUID groupId, String actorUserId)` | Soft delete |

## UserAssignmentFacade

[`UserAssignmentFacade`](../main/java/facade/UserAssignmentFacade.java) → [`UserAssignmentFacadeLocal`](../main/java/facadeLocal/UserAssignmentFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `listAssignedRoles(String userId)` | Atanmış roller |
| `listAssignedRoleGroups(String userId)` | Atanmış gruplar |
| `searchRolesForPicker(...)`, `countRolesForPicker(...)` | Rol seçici arama/sayım |
| `searchRoleGroupsForPicker(...)`, `countRoleGroupsForPicker(...)` | Grup seçici |
| `assignRole(...)`, `removeRoleAssignment(...)` | Rol ata/kaldır |
| `assignRoleGroup(...)`, `removeRoleGroupAssignment(...)` | Grup ata/kaldır |

## CurrentAccountFacade

[`CurrentAccountFacade`](../main/java/facade/CurrentAccountFacade.java) → [`CurrentAccountFacadeLocal`](../main/java/facadeLocal/CurrentAccountFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll(boolean includeDeleted)`, `findById`, `findByCode`, `findByType`, `searchByName` | Sorgular |
| `save(CurrentAccount account)` | Kaydet |
| `softDelete(UUID id)` | Soft delete |

## JournalEntryFacade

[`JournalEntryFacade`](../main/java/facade/JournalEntryFacade.java) → [`JournalEntryFacadeLocal`](../main/java/facadeLocal/JournalEntryFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findByDateRange`, `findByStatus`, `findById` | Sorgular |
| `save(JournalEntry entry)` | Taslak kayıt |
| `post(UUID entryId)` | Fişleştirme |
| `cancel(UUID entryId)` | İptal |
| `generateEntryNumber()` | Yeni fiş numarası |
| `softDelete(UUID id)` | Soft delete |

## CashAccountFacade

[`CashAccountFacade`](../main/java/facade/CashAccountFacade.java) → [`CashAccountFacadeLocal`](../main/java/facadeLocal/CashAccountFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findById`, `findByCode`, `findActive`, `save` | Kasa kartları |
| `addTransaction(UUID cashAccountId, ...)` | Hareket ekle (bakiye günceller) |
| `listTransactions(UUID cashAccountId)` | Hareket listesi |
| `getBalance(UUID id)` | Bakiye |

## BankAccountFacade

[`BankAccountFacade`](../main/java/facade/BankAccountFacade.java) → [`BankAccountFacadeLocal`](../main/java/facadeLocal/BankAccountFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findById`, `findByCode`, `findActive`, `save` | Banka kartları |
| `addTransaction(UUID bankAccountId, ...)` | Hareket |
| `listTransactions(UUID bankAccountId)` | Hareketler |
| `getBalance(UUID id)` | Bakiye |

## ProductFacade

[`ProductFacade`](../main/java/facade/ProductFacade.java) → [`ProductFacadeLocal`](../main/java/facadeLocal/ProductFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findById`, `findByCode`, `findByCategory`, `searchByNameOrCode`, `findLowStock` | Sorgular |
| `save(Product product)` | Kayıt |
| `softDelete(UUID id)` | Soft delete |

## StockMovementFacade

[`StockMovementFacade`](../main/java/facade/StockMovementFacade.java) → [`StockMovementFacadeLocal`](../main/java/facadeLocal/StockMovementFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `addMovement(UUID productId, ...)` | Stok hareketi + ürün miktarı |
| `getMovements(UUID productId, LocalDate from, LocalDate to)` | Hareket geçmişi |

## ChequeFacade

[`ChequeFacade`](../main/java/facade/ChequeFacade.java) → [`ChequeFacadeLocal`](../main/java/facadeLocal/ChequeFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findById`, `findByType`, `findByStatus`, `findDueWithin` | Sorgular |
| `save(Cheque cheque)` | Kayıt |
| `deposit(UUID id, UUID bankAccountId)` | Bankaya yatır |
| `collect(UUID id)` | Tahsil (banka bakiyesi artar) |
| `returnCheque(UUID id)` | İade |
| `protest(UUID id)` | Protesto |
| `pay(UUID id)` | Verilmiş çek ödemesi (banka bakiyesi azalır) |

## ExpenseFacade

[`ExpenseFacade`](../main/java/facade/ExpenseFacade.java) → [`ExpenseFacadeLocal`](../main/java/facadeLocal/ExpenseFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findAll`, `findById`, `findByType`, `findByDateRange`, `findByStatus` | Sorgular |
| `findCategoriesByType(String type)` | Kategori listesi |
| `save(Expense expense)` | Kayıt |
| `approve(UUID id, String approvedByUserId)` | Onay |
| `reject(UUID id)` | Red |
| `softDelete(UUID id)` | Soft delete |
| `getSummaryByCategory(LocalDate from, LocalDate to)` | Özet |

## ReportFacade

[`ReportFacade`](../main/java/facade/ReportFacade.java) → [`ReportFacadeLocal`](../main/java/facadeLocal/ReportFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `getTrialBalance(LocalDate asOf)` | Mizan |
| `getProfitLoss(LocalDate from, LocalDate to)` | Kar/zarar |
| `getCashFlowSummary(LocalDate from, LocalDate to)` | Nakit akış özeti |
| `getReceivables()`, `getPayables()` | Alacak/borç listeleri |
| `getAgedReceivables()` | Vade analizi |
| `getStockValuation()` | Stok değeri |

## AuditLogFacade

[`AuditLogFacade`](../main/java/facade/AuditLogFacade.java) → [`AuditLogFacadeLocal`](../main/java/facadeLocal/AuditLogFacadeLocal.java)

| Metod | Açıklama |
|-------|-----------|
| `findRecent(int limit)` | Son kayıtlar |
| `findByUser(String userId, LocalDate from, LocalDate to)` | Kullanıcıya göre |
| `findByEntityType(String entityType)` | Entity tipine göre |
| `countByAction(String action, LocalDate from, LocalDate to)` | Sayım |

**Not:** Yönetim ekranı [`AuditService.findAll`](../main/java/service/AuditService.java) kullanır; facade ek sorgular için mevcut.

## StudentFacade

[`StudentFacade`](../main/java/facade/StudentFacade.java): `createStudent(Student entity)` — öğrenci oluşturma.
