# 🛡️ VaultDesk
A modern desktop security platform that lets users securely store passwords, notes, and documents behind a single encrypted master vault — with multi-factor authentication, intrusion protection, and full activity logging.

---

## 🚀 Features
* Master Password Authentication (PBKDF2-hashed, never stored in plaintext)
* Multi-Factor Authentication (TOTP — Google Authenticator / Authy compatible)
* AES-256-GCM Encrypted Password Manager with built-in Generator
* Encrypted Secure Notes with Tags & Categories
* Encrypted Document Locker (PDF/Image) with SHA-256 Integrity Verification
* Account Lockout after Repeated Failed Login Attempts
* Security Score Dashboard (strength, reuse, and age analysis)
* Tag-based & Category-based Search
* Trash / Recovery for Deleted Items
* Encrypted Vault Backup & Restore
* Full Security Event Log (logins, edits, deletes, MFA changes)
* Secure Clipboard (auto-clears copied passwords after 30 seconds)
* Auto-Lock on Inactivity
* Dark "Classic Vault" Themed UI

---

## 🛠 Tech Stack
### Application:
* Java 17
* JavaFX (FXML + CSS)
* MVC / DAO / Service Architecture

### Data & Security:
* SQLite (embedded, zero-config)
* JDBC
* AES-256-GCM Encryption
* PBKDF2 Password Hashing
* TOTP (RFC 6238) Multi-Factor Authentication

### Build & Packaging:
* Maven
* jpackage (native Windows/macOS/Linux installers)

---

## ⚙️ How to Run Locally
Clone the repository:
```bash
git clone https://github.com/YashMehra15/VaultDesk.git
```

Navigate to project folder:
```bash
cd VaultDesk
```

Run the app (Maven downloads all dependencies automatically):
```bash
mvn clean javafx:run
```

No database setup required — VaultDesk creates its own local encrypted database automatically at:
```
%USERPROFILE%\.vaultdesk\vaultdesk.db
```

### 📦 Building a Standalone Installer
```bash
mvn clean package
jpackage --type app-image --name VaultDesk --input target ^
  --main-jar VaultDesk-standalone.jar --main-class com.vaultdesk.Launcher --dest dist
```

---

## 🌐 Requirements
* JDK 17+
* Maven 3.9+

---


## 🚀 Download VaultDesk

Download the latest Windows release here:

[![Download VaultDesk](https://img.shields.io/badge/Download-VaultDesk-blue?style=for-the-badge&logo=windows)](https://github.com/YashMehra15/VaultDesk/releases/latest)

**Version:** 2.0.0  
**Platform:** Windows 64-bit

## 🎯 Purpose
This project demonstrates a complete desktop security platform — covering authentication design, applied cryptography, multi-factor authentication, secure file handling, and clean layered architecture (MVC/DAO/Service) using Java and JavaFX.

---

## 👨‍💻 Developed by
Yash Mehra

## ⭐ Support
If you like this project, give it a ⭐ on GitHub and share it!
