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
| **Docker** | Postgres + Flyway (ve isteğe bağlı tam stack) |
| **Payara Server 7** | Yerel geliştirme için (Jakarta EE 11 uyumlu sürüm) |

---

## Geliştirici ortamı (önerilen): DB Docker’da, uygulama yerelde

Bu akışta yalnızca **PostgreSQL** ve **Flyway** konteynerde çalışır; **Payara’yı IDE veya yerel kurulumda** çalıştırırsınız.

### 1. Ortam dosyası

```bash
cp .env.example .env
```

`.env` içindeki `MODE` ve `COMPOSE_PROFILES` değerleri **bilgilendirme / Compose profili** içindir (MODE tek başına Compose’u otomatik değiştirmez).

### 2. Veritabanı ve migrasyonları kaldırma

Proje kökünde:

```bash
docker compose up -d
```

[`docker-compose.yaml`](docker-compose.yaml) içinde **`app` servisi yalnızca `production` profilindedir**; profil vermeden çalıştırdığınızda **postgres** ve **flyway** ayakta olur, uygulama konteyneri **başlamaz**.

Eşdeğer alternatif:

```bash
docker compose -f docker-compose.dev.yaml up -d
```

Flyway bir kez migrasyonları uygular ve çıkar (`restart: "no"`). Postgres sürekli çalışır.

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

**Context path:** Yerelde genelde uygulama adıyla deploy edilir (`http://localhost:8080/accounting/...`). Docker production imajında [`Dockerfile`](Dockerfile) **`ROOT.war`** kullandığı için kök context (`/`) olabilir. Giriş URL’sini kendi deploy adınıza göre uyarlayın:

- Örnek: `http://localhost:8080/accounting/login.xhtml`

### 6. Varsayılan seed kullanıcı

Migrasyonlar sonrası:

| Alan | Değer |
|------|--------|
| E-posta | `admin@example.com` |
| Şifre | `admin` |

Ek olarak [`V2__admin_scope_roles.sql`](src/main/resources/db/migration/V2__admin_scope_roles.sql) ile admin kullanıcıya **ROLE** ve **ROLE_GROUP** scope menüleri için roller bağlanır.

---

## Production / tam stack Docker

Uygulama konteynerinin de kalkması için **Compose production profili** gerekir:

```bash
export COMPOSE_PROFILES=production
docker compose up --build
```

veya tek satır:

```bash
docker compose --profile production up --build
```

- Payara konteyneri Flyway başarıyla bittikten sonra başlar.
- DB bağlantısı Docker ağında **`postgres:5432`** üzerinden yapılandırılır ([`docker/payara-entrypoint.sh`](docker/payara-entrypoint.sh)).
- HTTP portu: `.env` içinde `APP_HTTP_PORT` (varsayılan `8080`).

---

## Önemli URL’ler (context köküne göre önekleyin)

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
| Giriş sonrası 404 | Deploy **context path**’ini kontrol edin (`/accounting` vs `/`). |

---

## Lisans ve katkı

Proje yapılandırmasına göre bu bölümü güncelleyin.
