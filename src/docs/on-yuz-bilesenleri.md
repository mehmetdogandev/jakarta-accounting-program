# Ön yüz bileşenleri (Facelets ve PrimeFaces)

## Facelets kompozisyonu

- **`ui:composition`**: Sayfa kökü; `template` ile üst şablona bağlanır.
- **`ui:define`**: Şablondaki `ui:insert` bölgelerinin içeriği (`title`, `content`, …).
- **`ui:include`**: Parça yükleme (ör. kenar çubuğu).
- **`ui:repeat`**: Basit döngü.

Admin şablonu: [`WEB-INF/templates/admin.xhtml`](../main/webapp/WEB-INF/templates/admin.xhtml).

## RBAC ve EL

[`RbacBean`](../main/java/bean/RbacBean.java) (`@Named("rbac")`, `@RequestScoped`) oturumdaki `userId` ile [`AuthorizationServiceLocal`](../main/java/service/AuthorizationServiceLocal.java) çağırır:

```text
#{rbac.can('USER','ACCESS')}
```

İlk argüman [`Scope`](../main/java/enums/Scope.java) enum adı, ikinci [`Permission`](../main/java/enums/Permission.java) enum adı (string olarak `valueOf`).

Kenar çubuğu ve düğmeler `rendered="#{rbac.can(...)}"` ile koşullu gösterilir.

## PrimeFaces kullanım kalıpları (projede geçenler)

Projede kullanılan PrimeFaces bileşenleri (prefix `p:`):

| Bileşen | Tipik kullanım |
|---------|----------------|
| `p:dataTable` | Listeler (kullanıcı, rol, cari, fiş, stok, …) |
| `p:dialog` | Modal düzenleme |
| `p:inputText`, `p:password`, `p:inputTextarea` | Form alanları |
| `p:selectOneMenu`, `p:selectManyCheckbox`, `p:selectBooleanCheckbox` | Seçim |
| `p:autoComplete` | Ürün/cari tamamlama (`completeMethod`) |
| `p:datePicker` | Tarih filtreleri ve form |
| `p:ajax` | Kısmi güncelleme (`listener`, `update`, `event`) |
| `p:tabView` | Gelir/gider sekmeleri |
| `p:dataList` | Kasa/banka hesap listesi |
| `p:chart` | Dashboard çizgi grafikleri |
| `p:commandButton`, `p:commandLink` | Aksiyonlar |
| `p:outputPanel` | Koşullu bölgeler |

JSF çekirdeği (`h:`): `h:link`, `h:commandButton`, `h:outputText`, `h:panelGroup`, `f:selectItems`, `f:view`.

## Sunucu tarafı RBAC guard

Bean’ler içinde [`RbacProcedureBean`](../main/java/procedure/RbacProcedureBean.java) ile `require(Scope, Permission)` veya string overload kullanılabilir (ör. [`AdminReportBean`](../main/java/bean/AdminReportBean.java) başlangıçta `REPORT` + `ACCESS`).

## Yardımcı bean’ler

- **`adminUi`** ([`AdminUiBean`](../main/java/bean/AdminUiBean.java)): `@ApplicationScoped` — avatar baş harfleri, kısa tarih formatı (İstanbul TZ).
- **`authBean`**: Çıkış aksiyonu.
