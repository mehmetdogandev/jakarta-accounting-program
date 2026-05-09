# Backend — Servis ve güvenlik

## AuthorizationService

[`AuthorizationService`](../main/java/service/AuthorizationService.java) implements [`AuthorizationServiceLocal`](../main/java/service/AuthorizationServiceLocal.java) (`@Stateless`).

| Metod | Açıklama |
|-------|-----------|
| `boolean can(String userId, Permission permission)` | İzin; scope filtresi yok (`scope == null` kabul mantığı) |
| `boolean can(String userId, Permission permission, Scope scope)` | Verilen kapsam için izin |

**Mantık:** `EffectiveRoleResolver.resolveEffectiveRoleIds(userId)` ile doğrudan `user_role` ve rol grupları üzerinden efektif rol id kümesi; native SQL ile `role_permission` + `role.scope` satırları okunur; eşleşen `(permission, scope)` varsa `true`.

## EffectiveRoleResolver

[`EffectiveRoleResolver`](../main/java/service/EffectiveRoleResolver.java) (`@Stateless`)

| Metod | Açıklama |
|-------|-----------|
| `Set<UUID> resolveEffectiveRoleIds(String userId)` | `user_role` UNION `user_role_group` → `role_group_role` ile türetilen tüm rol id’leri |

## AuditService

[`AuditService`](../main/java/service/AuditService.java) (`@Stateless`)

| Metod | Açıklama |
|-------|-----------|
| `log(...)` | Genel denetim satırı (`REQUIRES_NEW`); IP/User-Agent istekten |
| `logLogin(...)` | Başarılı/başarısız giriş |
| `logAction(...)` | Entity getter’larından JSON benzeri özet |
| `findAll(LocalDate from, LocalDate to, String entityType, String userId)` | JPQL filtreli liste |
| `findByEntity(String entityType, UUID entityId)` | Entity bazlı |

Kalıcı yazım çoğunlukla native `INSERT INTO audit_log`.

## RbacProcedureBean

[`RbacProcedureBean`](../main/java/procedure/RbacProcedureBean.java) (`@Stateless`, faces-aware)

| Metod | Açıklama |
|-------|-----------|
| `Optional<String> currentUserId()` | Oturum `userId` |
| `boolean require(Scope scope, Permission permission)` | Yetkisizse `FacesMessage` + `false`; FacesContext yoksa test için `true` dönebilir |
| `boolean require(String scopeName, String permissionName)` | Enum parse ederek `require` |

## RbacBean (Facelets)

[`RbacBean`](../main/java/bean/RbacBean.java): EL `#{rbac.can('SCOPE','PERMISSION')}`. Sunucu aksiyonlarında `RbacProcedureBean` tercih edilir.
