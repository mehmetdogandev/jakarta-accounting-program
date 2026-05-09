# Jakarta Muhasebe (Accounting)

Jakarta EE 11 tabanlı bir **muhasebe / yönetim** iskeleti: **JSF (Facelets)** arayüz, **PrimeFaces** bileşenleri, **PostgreSQL** veritabanı, **Flyway** ile şema migrasyonları ve **RBAC** (rol / rol grubu / izin) katmanı.

## Özellikler

- **Kimlik:** Kayıt (`/register.xhtml`), giriş (`/login.xhtml`), oturum filtresi.
- **Admin:** Sol menüde **Sistem Yönetimi** (Bootstrap collapse): Kullanıcılar, Roller, Rol grupları; tablo + diyalog ile CRUD ve **soft delete**.
- **Yetkilendirme:** `AuthorizationService` ile etkin rollerin birleştirilmesi; sunucu tarafında `RbacProcedureBean.require(scope, permission)`; arayüzde `#{rbac.can('USER','ACCESS')}` gibi EL ifadeleri.
- **Dağıtım:** WAR (`accounting.war`), yerelde veya Docker içinde **Payara Server 7** (Jakarta EE 11).

## Mimari (kısa)

| Katman | Paket / konum |
|--------|----------------|
| Sunum | `bean/`, `src/main/webapp/**/*.xhtml` |
| RBAC yardımcıları | `procedure/RbacProcedureBean`, `bean/RbacBean`, `service/` |
| Kalıcılık | `facade/`, `facadeLocal/`, `entity/` |
| Şema | `src/main/resources/db/migration/` (Flyway) |
| JTA veri kaynağı | `jdbc/testPSQL` → [`persistence.xml`](src/main/resources/META-INF/persistence.xml) |

## Gereksinimler

| Araç | Not |
|------|-----|
| **JDK** | 17+ ([`pom.xml`](pom.xml) `maven.compiler.release`) |
| **Maven** | 3.9+ önerilir |
| **Docker** | Varsayılan geliştirme: DB + uygulama konteynerde |
| **Payara Server 7** | İsteğe bağlı: yalnızca DB Docker’da, WAR’ı IDE’den çalıştırırken |

---

## Hızlı başlat (`pnpm run dev` / Next.js benzeri tek komut)

Bu proje **Node değil**; yine de **tek komutla** Postgres + migrasyon + Payara’yı ayağa kaldırmak için:

```bash
cp .env.example .env   # ilk sefer
pnpm dev               # veya: npm run dev
```

Eşdeğer (Compose doğrudan):

```bash
docker compose up --build -d
```

- İlk build biraz sürebilir (Payara imajı + Maven).
- Tarayıcı: **`http://localhost:8080/login.xhtml`** ([`Dockerfile`](Dockerfile) `ROOT.war` kullandığı için genelde kök context).

**Önemli:** Next.js’teki gibi **anında HMR yok**. Java/XHTML değişince genelde yeniden:

```bash
pnpm dev
```

(yeniden build + konteyner güncellemesi) veya sadece `docker compose up --build -d`.

**Sadece veritabanı** (yerel Payara + IDE deploy kullanacaksan):

```bash
pnpm dev:db
# veya: docker compose -f docker-compose.dev.yaml up -d
```

---

## Geliştirici ortamı (alternatif): DB Docker’da, uygulama yerelde

Payara’yı **bilgisayarında** çalıştırıp yalnızca PostgreSQL’i Docker’da istiyorsan:

### 1. Ortam dosyası

```bash
cp .env.example .env
```

### 2. Yalnızca Postgres + Flyway

```bash
docker compose -f docker-compose.dev.yaml up -d
```

### 3. PostgreSQL bağlantı bilgisi

Varsayılanlar [`.env.example`](.env.example) ile uyumludur:

- Host (makinenizden): `localhost`
- Port: `5432` (`.env` ile `POSTGRES_PORT` değiştirilirse buna göre)
- Veritabanı: `accounting`
- Kullanıcı / şifre: `.env` içindeki `POSTGRES_USER` / `POSTGRES_PASSWORD`

### 4. Payara’da JNDI: `jdbc/testPSQL`

Uygulama sabit olarak **`jdbc/testPSQL`** JNDI adını bekler; bunu tanımlamazsanız deploy sonrası JPA başlatılamaz.

**PostgreSQL sürücüsü:** [JDBC driver](https://jdbc.postgresql.org/download/) JAR dosyasını Payara domain klasörüne koyun, örneğin:

`payara7/glassfish/domains/domain1/lib/postgresql.jar`

#### Seçenek A — `asadmin` (komut satırı)

Payara’nın `bin` klasöründe, domain çalışırken (değerleri kendi `.env` ile değiştirin):

```bash
asadmin create-jdbc-connection-pool \
  --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
  --restype javax.sql.DataSource \
  --property serverName=localhost:portNumber=5432:databaseName=accounting:user=postgres:password=postgres \
  AccountingDevPool

asadmin create-jdbc-resource \
  --connectionpoolid AccountingDevPool \
  jdbc/testPSQL
```

Havuz veya kaynak zaten varsa önce silin veya farklı havuz adı kullanın:

```bash
asadmin delete-jdbc-resource jdbc/testPSQL
asadmin delete-jdbc-connection-pool AccountingDevPool
```

#### Seçenek B — Admin Console

1. **Payara Admin Console** → **Resources** → **JDBC** → **JDBC Connection Pools** → **New**
   - Pool Name: örn. `AccountingDevPool`
   - Resource Type: `javax.sql.DataSource`
   - Database Driver Vendor: **PostgreSQL** (veya custom)
   - Ek özellikler: `serverName=localhost`, `portNumber=5432`, `databaseName=accounting`, `user`, `password`
2. **JDBC Resources** → **New**
   - JNDI Name: **`jdbc/testPSQL`** (tam olarak bu isim)
   - Pool Name: yukarıdaki havuz

### 5. Derleme ve deploy

```bash
mvn clean package -DskipTests
```

Çıktı: `target/accounting.war`

Bu WAR dosyasını Payara’ya deploy edin (IDE artefact, `autodeploy` klasörü veya Admin Console **Applications**).

**Context path:** Yerelde genelde uygulama adıyla deploy edilir (`http://localhost:8080/accounting/...`). Docker tam stack’te [`Dockerfile`](Dockerfile) **`ROOT.war`** kullandığı için kök context (`/`) olabilir. Giriş URL’sini kendi deploy adınıza göre uyarlayın:

- Örnek: `http://localhost:8080/accounting/login.xhtml`

### 6. Varsayılan seed kullanıcı

Migrasyonlar sonrası:

| Alan | Değer |
|------|--------|
| E-posta | `admin@example.com` |
| Şifre | `admin` |

Ek olarak [`V2__admin_scope_roles.sql`](src/main/resources/db/migration/V2__admin_scope_roles.sql) ile admin kullanıcıya **ROLE** ve **ROLE_GROUP** scope menüleri için roller bağlanır.

---

## Docker ile tam stack (varsayılan `pnpm dev`)

[`docker-compose.yaml`](docker-compose.yaml) dosyasında **postgres**, **flyway** ve **app** (Payara) birlikte tanımlıdır. `pnpm dev` veya `docker compose up --build -d` sonrası:

- Flyway migrasyonları uygulanır.
- Payara konteyneri DB’ye Docker ağından **`postgres:5432`** ile bağlanır ([`docker/payara-entrypoint.sh`](docker/payara-entrypoint.sh) ile `jdbc/testPSQL`).
- HTTP: `.env` içindeki `APP_HTTP_PORT` (varsayılan **8080**).

---

## Önemli URL’ler (Docker ROOT deploy)

| Sayfa | Path |
|--------|------|
| Giriş | `/login.xhtml` |
| Kayıt | `/register.xhtml` |
| Admin özet | `/admin/dashboard.xhtml` |
| Kullanıcılar | `/admin/users.xhtml` |
| Roller | `/admin/roles.xhtml` |
| Rol grupları | `/admin/role-groups.xhtml` |

Menü öğeleri ilgili **scope** için **`ACCESS`** izni ile görünür; satır işlemleri **CREATE / READ / UPDATE / DELETE** ile hem arayüzde hem sunucu aksiyonlarında kontrol edilir.

---

## Migrasyonlar

- Konum: [`src/main/resources/db/migration/`](src/main/resources/db/migration/)
- Compose içinde **Flyway** image ile çalıştırılır; SQL dosyaları volume ile bağlanır.

Temiz bir veritabanı için volume’u silmek (veri kaybı):

```bash
docker compose down -v
```

---

## Sorun giderme

| Sorun | Öneri |
|--------|--------|
| Port **5432** dolu | `.env` içinde `POSTGRES_PORT` değiştirin; Payara pool `portNumber` ile aynı olmalı. |
| Flyway hata verdi | Logları kontrol edin; şema değiştiyse volume sıfırlama veya onarıcı migration gerekebilir. |
| IDE: “pom güncel değil” | Maven projesini yeniden yükle / **Reload Window** / Java Language Server yenilemesi. |
| `jdbc/testPSQL` bulunamadı | JNDI adının tam **`jdbc/testPSQL`** olduğundan ve PostgreSQL JAR’ın domain `lib` altında olduğundan emin olun. |
| Giriş sonrası 404 | Docker’da kök: `/login.xhtml`. Yerel deploy’da context `/accounting` olabilir. |

---

## Lisans ve katkı

Proje yapılandırmasına göre bu bölümü güncelleyin.
