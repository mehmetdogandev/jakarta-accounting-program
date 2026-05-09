# Backend — Converter, filter, bootstrap, util

## Converter (`converter`)

JSF `FacesConverter(managed = true)` ile CDI/EJB enjekte edilir.

| Sınıf | Kayıt id | Davranış |
|-------|-----------|-----------|
| [`CurrentAccountConverter`](../main/java/converter/CurrentAccountConverter.java) | `currentAccountConverter` | String UUID ↔ `CurrentAccountFacadeLocal.findById` |
| [`BankAccountConverter`](../main/java/converter/BankAccountConverter.java) | `bankAccountConverter` | UUID ↔ `BankAccountFacadeLocal.findById` |
| [`ProductConverter`](../main/java/converter/ProductConverter.java) | `productConverter` | UUID ↔ `ProductFacadeLocal.findById` |
| [`ProductCategoryConverter`](../main/java/converter/ProductCategoryConverter.java) | `productCategoryConverter` | UUID ↔ `EntityManager.find(ProductCategory)` |

**Metodlar (her biri):** `getAsObject`, `getAsString` — boş/geçersiz UUID için null/boş string.

## Filter (`filter`)

[`SessionFilter`](../main/java/filter/SessionFilter.java): oturum ve login/register yönlendirme. Özet: [mimari-ve-istek-akisi.md](mimari-ve-istek-akisi.md).

## Bootstrap (`bootstrap`)

[`SeedAdminSuperAdminBootstrap`](../main/java/bootstrap/SeedAdminSuperAdminBootstrap.java):

- `@Singleton` `@Startup`
- **`ensureSeedAdminSuperAdminGroup()`** (`@PostConstruct`): `SEED_ADMIN_ENSURE_SUPER_ADMIN` ortam değişkeni truthy ise seed admin kullanıcıya süper admin rol grubu atanır (`UserAssignmentFacadeLocal.assignRoleGroup`).
- Sabit id’ler: seed kullanıcı `00000000-0000-4000-8000-000000000001`, grup `40000000-0000-4000-8000-000000000001` (V3 ile uyumlu).

## Util (`util`)

[`StockMovementTypeLabels`](../main/java/util/StockMovementTypeLabels.java):

- **`public static String tr(String code)`**: DB’de saklanan hareket kodunu Türkçe etikete çevirir (`IN`, `OUT`, `ADJUSTMENT`, …).
