# TheOnlyException

**System Crash** is a roguelike survival game inspired by *Soul Knight* and *HoloCure*, developed in Java using JavaFX and FXGL. Players battle waves of endlessly spawning enemies using melee and ranged attacks to survive as long as possible. The game features dynamic combat, power-ups, leaderboards, and user authentication—designed with a strong emphasis on object-oriented programming and clean, interactive UI.

For QA Testers: this branch has been refactored for easier access for you guys. simply clone this project into your preferred IDE (in our case, we used Intellij), and switch to this branch (offline-version). If everything went as is, you shouldn't encounter any problems. If you guys ever do, please reach out to us so we can address the problem swiftly.

---

## 🎮 Project Overview

Survive in a confined arena as enemies spawn from the edges and hunt you down. Maximize your survival time using a range of weapons and power-ups. The game features:

- A dynamic and scalable enemy spawning system.
- Power-ups and upgrade mechanics.
- Full user authentication and leaderboard system.
- UI scenes including login, register, pause, settings, game over, and leaderboard—all connected to a database.

---

## ✨ Key Features

- **Survival Mode**: Endless enemy waves increase in difficulty.
- **Combat System**:
  - Melee: `SwordComponent`
  - Ranged: `LaserComponent`, `VoltChainComponent`
- **Health System**: Each component tracks health; the game ends when player health reaches 0.
- **Arena Mechanics**: Managed by `WaveManager`, enemies spawn from arena edges.
- **Power-Ups** (6 total):
  - `AutoHealComponent`
  - `ExplosiveMinesComponent`
  - `FireTrailComponent`
  - `LightningStrikeComponent`
  - `PoisonAuraComponent`
  - `ShieldComponent`
- **User System**:
  - Login/Register (`LoginScene`, `RegisterScene`)
  - Secure password hashing with `PasswordUtils`
- **Leaderboard**:
  - Shows top players’ survival times and damage.
- **Settings & Pause**:
  - Volume sliders for music and SFX.
  - Pause/resume functionality.

---

## ⚙️ Tech Stack

- **Java**: OOP, generics, multithreading
- **JavaFX**: UI and scene management
- **FXGL**: Game loop and ECS-based design
- **MySQL**: Persistent storage for users and leaderboards

---

## 🧠 Data Structures & Patterns

- **Collections**:
  - `Map<String, List<EventListener>>` in `ObservableComponent`
  - `List<SpawnCommand>` for wave management
- **Flyweight Pattern**: `EnemyFlyweightFactory` for enemy reuse
- **Observer Pattern**: Event-driven logic via `IObservable<T>`
- **Singletons**:
  - `LeaderboardDatabase`, `AuthManager`, `DatabaseManager`, `SoundManager`, `UpgradeRegistry`, `EnemyFlyweightFactory`

---

### 🔹 Object-Oriented Programming

- **Encapsulation**: Data in classes like `GameComponent` is protected via private fields and public getters/setters.
- **Inheritance**:
  - `GameComponent` → `Component`
  - `WeaponComponent` → `UpgradableComponent` → `LaserComponent`, `SwordComponent`, etc.
- **Polymorphism**:
  - Interfaces like `IDamageable`, `IWeapon`, `IUpgradable`, etc.
- **Abstraction**:
  - Abstract behavior in base classes and interfaces
- **Class Relationships**:
  - Composition: `WaveManager` ∘→ `SpawnCommand`
  - Associations: `PlayerComponent` → `GameApp`

### 🔹 Java Generics

- **Collections**:
  - Type-safe data management with `Map<String, List<EventListener>>`, `List<UpgradeOption>`, etc.
- **Generic Interface**:
  - `IObservable<T>` for event listeners
- **Reusability**:
  - Generic methods in `UpgradeRegistry`, e.g., `getRandomUpgradeOptions(...)`

### 🔹 Multithreading & Concurrency

- **FXGL Game Loop**: `onUpdate(double tpf)` for components
- **JavaFX Event Thread**: UI event handling (e.g., `handleLogin`, `resumeGame`)
- **Synchronization**:
  - `EnemyFlyweightFactory` uses synchronized cache
  - Thread-safe singleton access for managers

### 🔹 Graphical User Interface

- **JavaFX + FXML**:
  - All scenes defined with `.fxml` and styled via `.css`
- **Controllers**: Use `@FXML` to manage UI logic
- **User-Friendly UX**:
  - Responsive UI, hover effects, error messages
  - Leaderboards and upgrade selection menus

### 🔹 Database Connectivity (MySQL via XAMPP)

#### **Database System**
- **MySQL Server** (hosted locally via XAMPP)
- **JDBC Driver**: `mysql-connector-java`
- **Connection URL**: `jdbc:mysql://localhost:3306/dbtheonlyexception`

#### **Database Operations**
- **CRUD Implementation**:
  - **Create**: `INSERT` for player registration
  - **Read**: `SELECT` for login authentication and leaderboard data
  - **Update**: `UPDATE` player stats after each game session
  - **Delete**: (Optional) Player account removal

#### **Security Features**
- **Password Security**:
  - Hashing with `PasswordUtils.hashPassword()`
  - Verification with `PasswordUtils.verifyPassword()`
- **Input Validation**:
  - Sanitized user inputs
  - Error feedback for invalid credentials

#### **XAMPP Configuration**
1. Start MySQL service in XAMPP Control Panel
2. Database schema automatically initializes on first run
3. Access phpMyAdmin at `http://localhost/phpmyadmin`

#### **Key Classes**
- `DatabaseManager`: Handles all SQL operations
- `AuthManager`: Manages authentication flows
- `LeaderboardDatabase`: Processes ranking data

### 🔹 UML Diagrams

![currentClassDiagram](https://github.com/user-attachments/assets/db216f79-7706-4b02-aac6-b186663daddd)

---

## 📁 Project Setup

### 🔸 Prerequisites

- Java 17+
- IntelliJ IDEA
- FXGL & JavaFX plugins
- MySQL

### 🔸 Installation

```bash
git clone https://github.com/fictionithink/TheOnlyException.git

```
---
## 🎮 Gameplay Showcase
![Screenshot 2025-05-11 142111](https://github.com/user-attachments/assets/106ec013-4e38-4c2a-b95b-36f51dc5d8e1)

![Screenshot 2025-05-11 142213](https://github.com/user-attachments/assets/e534ed73-9e72-410b-a834-8db6c380f91d)

![Screenshot 2025-05-11 142305](https://github.com/user-attachments/assets/503f1d20-a360-4eb7-9a6e-15f5ec2ddee1)

![Screenshot 2025-05-11 142326](https://github.com/user-attachments/assets/96da80ba-6789-4759-a98a-4ea57d41ba41)

![Screenshot 2025-05-11 142501](https://github.com/user-attachments/assets/8622b473-af94-458a-a264-bcfd308045b0)

![Screenshot 2025-05-11 142606](https://github.com/user-attachments/assets/fd874b09-5fa6-4062-875c-54e05eb047f4)

---

## 👥 Team Members

- **Kurt Derrick Basalo**  
- **Kesha Jane L. Ceniza**  
- **John Kheinzy Mandawe**  
- **John Niko Merenillo**  
- **Primo Christian Montejo**

---
