# Automated Warehouse Restock System (AWRS)

**CS 4395 — Independent Study | Texas State University**
**Student:** Tatenda Machirori (`qgr28@txstate.edu`)
**Supervisor:** Dr. Klepetko Randall
**Semester:** Summer 2026

---

## Overview

AWRS is a Java desktop application that tracks warehouse inventory in real-time and automatically triggers restocking workflows. It targets small-to-medium warehouse operations and is developed without integration to physical hardware — all warehouse actions (receiving, picking, restocking) are simulated through a GUI.

Built to the [IEEE 830-1998](https://ieeexplore.ieee.org/document/720574) SRS standard.

---

## Tech Stack

| Layer        | Technology                    |
|--------------|-------------------------------|
| Language     | Java 17                       |
| Build Tool   | Maven 3.x                     |
| Database     | SQLite (via `sqlite-jdbc`)    |
| Testing      | JUnit 5 + Mockito             |
| IDE          | VS Code + Extension Pack for Java |
| Version Control | Git + GitHub              |

---

## Project Structure

```
awrs/
├── pom.xml
└── src/
    ├── main/
    │   └── java/com/awrs/
    │       ├── model/
    │       │   ├── User.java
    │       │   ├── Item.java
    │       │   ├── WarehouseLocation.java
    │       │   ├── InventoryRecord.java
    │       │   ├── RestockTask.java
    │       │   └── AuditLog.java
    │       ├── service/
    │       │   ├── AuthService.java
    │       │   ├── InventoryService.java
    │       │   └── RestockService.java
    │       └── repository/
    │           ├── UserRepository.java
    │           ├── ItemRepository.java
    │           ├── InventoryRepository.java
    │           ├── RestockTaskRepository.java
    │           └── AuditLogRepository.java
    └── test/
        └── java/com/awrs/
            ├── model/
            │   ├── UserTest.java               ← Demo 1
            │   ├── ItemTest.java               ← Demo 1
            │   └── WarehouseLocationTest.java  ← Demo 1
            └── service/
                ├── AuthServiceTest.java        ← Demo 1
                ├── InventoryServiceTest.java   ← Demo 2
                └── RestockServiceTest.java     ← Demo 3 (Final)
```

---

## Demo Iterations

### Demo 1 — Authentication & Catalog
**SRS Coverage:** §2.1 (Auth/RBAC) + §2.2 (Item Catalog + Location Definitions)

| Test File | Tests | What It Covers |
|---|---|---|
| `UserTest.java` | 10 | Model construction, role hierarchy, active flag |
| `ItemTest.java` | 11 | SKU catalog, thresholds, constraints |
| `WarehouseLocationTest.java` | 9 | Hierarchical location tree, path resolution |
| `AuthServiceTest.java` | 20 | Login/logout, RBAC, user creation, deactivation |

---

### Demo 2 — Inventory Workflows
**SRS Coverage:** §2.3 (Receive Shipments) + §2.4 (Fulfill Orders) + §2.5 (Adjustments)

| Test File | Tests | What It Covers |
|---|---|---|
| `InventoryServiceTest.java` | 18 | Receive, fulfill, adjust, quantity query, audit logs |

---

### Demo 3 (Final) — Restocking Engine + Predictive Analytics
**SRS Coverage:** §2.6 (Restocking Engine) + §2.7 (Predictive Analytics) + §2.8 (Dashboard/Reporting)

| Test File | Tests | What It Covers |
|---|---|---|
| `RestockServiceTest.java` | 24 | Batch scan, priority, assign, complete, SMA prediction, alert thresholds |

---

## Running Tests

### Prerequisites
- Java 17+
- Maven 3.8+
- VS Code with **Extension Pack for Java** (includes Maven for Java)

### Install dependencies & run all tests
```bash
mvn clean test
```

### Run tests for a specific demo
```bash
# Demo 1 only
mvn test -Dtest="UserTest,ItemTest,WarehouseLocationTest,AuthServiceTest"

# Demo 2 only
mvn test -Dtest="InventoryServiceTest"

# Demo 3 (Final) only
mvn test -Dtest="RestockServiceTest"
```

### Run tests in VS Code
1. Open the project folder in VS Code
2. Open any `*Test.java` file
3. Click the **▶ Run Test** gutter icon next to any `@Test` method or class
4. Or use the **Testing** sidebar (beaker icon) to run all tests at once

---

## Setting Up VS Code

1. Install [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
2. Clone the repo and open in VS Code:
   ```bash
   git clone https://github.com/<your-username>/awrs.git
   cd awrs
   code .
   ```
3. VS Code will auto-detect the Maven project. Wait for it to finish indexing.
4. Run `mvn clean install` once in the integrated terminal to pull all dependencies.

---

## Key Design Decisions

- **Repository pattern** — all data access abstracted behind interfaces; Mockito stubs them in tests, real SQLite implementations wire in at runtime
- **Service layer** — all business logic isolated from the GUI; fully unit-testable without a database
- **Mockito** — used throughout tests to stub repositories, isolating service logic and keeping tests fast and deterministic
- **SHA-256 password hashing** — implemented in `AuthService.hash()`, no plaintext passwords stored
- **Audit log** — every inventory transaction writes an immutable `AuditLog` record (verified in tests via `verify(mockAuditRepo).save(any())`)
- **Predictive analytics** — simple moving average (SMA) over daily usage history; `predictDaysUntilStockout()` and `shouldFirePredictiveAlert()` are pure functions — easy to unit test

---

## SRS Reference

The full Software Requirements Specification document is included in the repository root as `CS4395_SRS_AWRS.pdf`.

Functional requirements are mapped directly to test classes:

| SRS Section | Feature | Test Class |
|---|---|---|
| §2.1 | Auth + RBAC | `AuthServiceTest` |
| §2.2 | Catalog + Locations | `ItemTest`, `WarehouseLocationTest` |
| §2.3 | Receive Shipments | `InventoryServiceTest` |
| §2.4 | Fulfill Orders | `InventoryServiceTest` |
| §2.5 | Inventory Adjustments | `InventoryServiceTest` |
| §2.6 | Restocking Engine | `RestockServiceTest` |
| §2.7 | Predictive Analytics | `RestockServiceTest` |

---

## License

Academic project — Texas State University, CS 4395, Summer 2026.
