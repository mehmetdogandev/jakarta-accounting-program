# Webapp klasör yapısı (`src/main/webapp`)

Aşağıdaki ağaç, statik vendor dosyalarının çoğunu listeler; dokümantasyon odaklıdır.

```
webapp/
├── WEB-INF/
│   ├── web.xml                    # Servlet/JSF/context parametreleri
│   ├── templates/
│   │   └── admin.xhtml            # Admin layout şablonu
│   └── includes/
│       └── admin-sidebar.xhtml    # Sol menü + çıkış
├── admin/                         # Yönetim sayfaları (çoğu admin şablonunu kullanır)
├── app/
│   └── index.xhtml                # Minimal demo sayfa
├── panel/
│   └── index.xhtml                # RBAC test paneli
├── resources/
│   ├── css/                       # Bootstrap + proje stilleri
│   └── js/                        # Bootstrap bundle + admin-shell.js
├── auth-template.xhtml            # Giriş/kayıt düzeni
├── template.xhtml                 # Basit Bootstrap şablon (demo)
├── index.xhtml                    # Karşılama (frontendBean listesi)
├── login.xhtml                    # Oturum açma
├── register.xhtml                 # Kayıt
├── user.xhtml                     # Kullanıcı CRUD demo
└── student.xhtml                  # Öğrenci formu demo
```

## WEB-INF

### web.xml

[`WEB-INF/web.xml`](../main/webapp/WEB-INF/web.xml): Faces servlet eşlemesi, oturum, MIME eşlemeleri. Özet: [mimari-ve-istek-akisi.md](mimari-ve-istek-akisi.md).

### templates/admin.xhtml

[`WEB-INF/templates/admin.xhtml`](../main/webapp/WEB-INF/templates/admin.xhtml) admin kabuğunu tanımlar:

- `f:view locale="tr"`.
- `h:head`: Google Fonts (Inter), `bootstrap.min.css`, `admin-shell.css`, `admin-pages.css`, `bootstrap.bundle.min.js`.
- Bootstrap **offcanvas** kenar çubuğu (`#adminSidebar`); mobilde hamburger; masaüstünde collapse düğmeleri `data-admin-sidebar-action`.
- Bölümler: `ui:insert name="title"`, `topbar`, `topbarActions`, `content`.
- `<ui:include src="/WEB-INF/includes/admin-sidebar.xhtml"/>`.
- Sayfa sonu: `admin-shell.js`.

### includes/admin-sidebar.xhtml

[`admin-sidebar.xhtml`](../main/webapp/WEB-INF/includes/admin-sidebar.xhtml):

- Marka ve dashboard linki.
- **Sistem yönetimi**: dashboard, kullanıcılar, roller, rol grupları, denetim — her link `rendered="#{rbac.can(...)}` ile korunur.
- **Muhasebe işlemleri**: cari, fişler, kasa, banka, ürün, stok, çek/senet, gelir-gider, raporlar — yine RBAC.
- `view.viewId` ile aktif nav sınıfı (`admin-nav-link active`).
- Çıkış: `h:commandLink action="#{authBean.logout()}"`.

## Kök ve ortak şablonlar

| Dosya | Şablon | İçerik özeti |
|-------|--------|----------------|
| [`template.xhtml`](../main/webapp/template.xhtml) | Kendi | Basit `ui:insert mainContent`; Bootstrap CSS/JS |
| [`auth-template.xhtml`](../main/webapp/auth-template.xhtml) | Kendi | İki sütun auth düzeni; `authAside`, `authCard` bölgeleri; `auth.css` |
| [`index.xhtml`](../main/webapp/index.xhtml) | `template.xhtml` | `frontendBean.users` listesi |
| [`login.xhtml`](../main/webapp/login.xhtml) | `auth-template.xhtml` | `loginBean` email/şifre, `login()` |
| [`register.xhtml`](../main/webapp/register.xhtml) | `auth-template.xhtml` | `registerBean.register()` |
| [`user.xhtml`](../main/webapp/user.xhtml) | `template.xhtml` | `userBean` CRUD |
| [`student.xhtml`](../main/webapp/student.xhtml) | `template.xhtml` | `studentBean.student.user.name` örneği |
| [`panel/index.xhtml`](../main/webapp/panel/index.xhtml) | — | `panelBean` ile READ/ACCESS izin göstergesi |
| [`app/index.xhtml`](../main/webapp/app/index.xhtml) | — | Statik “APP” placeholder |

## admin/*.xhtml

Tüm admin sayfaları `template="/WEB-INF/templates/admin.xhtml"` kullanır (standard `ui:composition`).

| Sayfa | Ana bean | PrimeFaces / not |
|-------|-----------|------------------|
| [`dashboard.xhtml`](../main/webapp/admin/dashboard.xhtml) | `adminDashboardBean`, `rbac` | `p:chart` çizgi grafikleri, KPI kartları, `h:link` hızlı erişim |
| [`users.xhtml`](../main/webapp/admin/users.xhtml) | `adminUsersBean`, `adminUi`, `rbac` | `p:dataTable`, `p:dialog`, rol/rol grubu picker, `p:ajax` |
| [`roles.xhtml`](../main/webapp/admin/roles.xhtml) | `adminRolesBean`, `rbac` | Arama, tablo, izin çoklu seçim, detay/düzenleme diyalogları |
| [`role-groups.xhtml`](../main/webapp/admin/role-groups.xhtml) | `adminRoleGroupsBean`, `rbac` | Grup listesi, rol atama |
| [`audit-log.xhtml`](../main/webapp/admin/audit-log.xhtml) | `adminAuditLogBean`, `rbac` | Filtreler, tablo, seçimle detay paneli |
| [`current-accounts.xhtml`](../main/webapp/admin/current-accounts.xhtml) | `adminCurrentAccountBean`, `rbac` | Tablo + düzenleme formu |
| [`journal-entries.xhtml`](../main/webapp/admin/journal-entries.xhtml) | `adminJournalEntryBean`, `rbac` | Fiş satırları `p:dataTable`, `p:autoComplete`, post/iptal |
| [`cash-accounts.xhtml`](../main/webapp/admin/cash-accounts.xhtml) | `adminCashAccountBean`, `rbac` | `p:dataList`, işlem diyaloğu |
| [`bank-accounts.xhtml`](../main/webapp/admin/bank-accounts.xhtml) | `adminBankAccountBean`, `rbac` | Kasaya benzer; banka işlemleri |
| [`products.xhtml`](../main/webapp/admin/products.xhtml) | `adminProductBean`, `rbac` | Ürün kartları, stok |
| [`stock-movements.xhtml`](../main/webapp/admin/stock-movements.xhtml) | `adminStockMovementBean`, `rbac` | `p:autoComplete` ürün, `p:datePicker`, hareket tablosu |
| [`cheques.xhtml`](../main/webapp/admin/cheques.xhtml) | `adminChequeBean`, `rbac` | Çek durum aksiyonları |
| [`expenses.xhtml`](../main/webapp/admin/expenses.xhtml) | `adminExpenseBean`, `rbac` | `p:tabView` gelir/gider sekmeleri, onay/red |
| [`reports.xhtml`](../main/webapp/admin/reports.xhtml) | `adminReportBean`, `rbac` | Rapor tipi, tarih, tablo/özet, dışa aktarma düğmeleri |

## resources/css

| Dosya | Rol |
|-------|-----|
| `bootstrap*.css` / `.min.css` / `.rtl.*` | Bootstrap 5 vendor — responsive grid, bileşenler |
| [`admin-shell.css`](../main/webapp/resources/css/admin-shell.css) | Admin layout, kenar çubuğu, topbar |
| [`admin-pages.css`](../main/webapp/resources/css/admin-pages.css) | Sayfa kartları, tablolar, diyaloglar |
| [`auth.css`](../main/webapp/resources/css/auth.css) | Giriş/kayıt sayfası |

`.map` dosyaları kaynak haritasıdır; çalışma zamanında zorunlu değildir.

## resources/js

| Dosya | Rol |
|-------|-----|
| `bootstrap.bundle*.js` | Bootstrap JS (vendor) |
| [`admin-shell.js`](../main/webapp/resources/js/admin-shell.js) | Proje: `lg` ve üzeri ekranda kenar çubuğu daraltma; `localStorage` anahtarı `admin-sidebar-collapsed`; küçük ekranda sınıf temizlenir (offcanvas ile uyum). |

**Akış:** `DOMContentLoaded` → depodan durumu uygula; `data-admin-sidebar-action="collapse|open"` tıklanınca `admin-shell--sidebar-collapsed` sınıfı; `resize` ile breakpoint senkronu; `syncAria` ile `aria-expanded`.

## URL ve uzantı

`AUTOMATIC_EXTENSIONLESS_MAPPING` etkin olduğu için navigasyon linkleri çoğu yerde `/admin/users` gibi uzantısız `outcome` kullanır; sunucu uygun `*.xhtml` görünümünü eşler.
