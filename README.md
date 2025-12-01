# 🎬 Java Cinema
![Java](https://img.shields.io/badge/-Java-orange?style=flat&logo=java&logoColor=white)
![Docker](https://img.shields.io/badge/-Docker-blue?style=flat&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/-Kubernetes-purple?style=flat&logo=kubernetes&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/-GitHub%20Actions-black?style=flat&logo=github&logoColor=white)

A multi-threaded simulation of a cinema ticket reservation system in Java (Version 25). Originally developed as a 2nd year course homework using C ([original repo](https://github.com/PapageorgiouMarios/Booking-Theater-Seats-POSIX-Threads)) 
but now it's extended with advanced concurrency handling, logging, and deployment support.

<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/d00a6272-8f66-45ae-84a2-d7896ffcf120" />
<img width="100" height="100" alt="image" src="https://github.com/user-attachments/assets/36a34615-d750-4214-962a-54f68bf29510" />
<img width="90" height="90" alt="image" src="https://github.com/user-attachments/assets/245ba176-dcec-45e1-a060-5447713971a2" />

---

## ✅ Goal

Simulate a cinema ticket reservation system with:

- Multiple customers booking seats concurrently  
- Operators and Cashiers as limited resources  
- Realistic seat search, reservation, and payment flow  
- Concurrency safety using `ReentrantLock`, `Condition`, and atomic operations  
- Detailed logging for monitoring and analysis  

---

## 🧩 Project Structure

| File | Description |
|------|-------------|
| `Cinema.java` | Defines cinema zones, seats, costs, and constants |
| `CustomerRequest.java` | Represents a customer’s booking request |
| `ISyncControl.java` | Interface for all synchronized operations |
| `SyncControl.java` | Implements `ISyncControl` with concurrency control and payment processing |
| `Main.java` | Entry point: initializes cinema simulation and customer threads |

---

## 📚 Libraries

This project uses SLF4J + Logback for structured logging instead of simple console output:

- **SLF4J (Simple Logging Facade for Java)** – A logging **API abstraction**. It allows your code to be independent of the actual logging implementation, meaning you can switch logging frameworks (e.g., Logback, Log4j) without changing your code. In this project, SLF4J is the interface used in all classes.

- **Logback Classic** – The **implementation of SLF4J** used here. Logback Classic is responsible for actually writing logs to the console or files according to the logging configuration. It is fast, reliable, and supports features like log formatting, filtering, and rolling log files.

- **Logback Core** – A **dependency required by Logback Classic**. It provides the low-level functionality for appending log events and managing logging configurations. It’s necessary for Logback Classic to work.

> Logging configuration is defined in `logback.xml`  

---

## 💻 Run via Command Line

Compile:

```bash
javac -cp "lib/*;." -d out src/model/*.java src/syncControl/*.java src/Main.java
```
Run:
```bash
java -cp "out;lib/*" Main <number_of_customers> <RNG_seed>
```

## 🐳 Docker Deployment

Build Docker image:
```bash
docker build -t javacinema:1.0 .
```
Run Docker container:
```bash
docker run -it javacinema:1.0 <number_of_customers> <RNG_seed>
```
## ☸️ Kubernetes Deployment

Apply deployment YAML:
```bash
kubectl apply -f deployment.yml
```

## ⚙️ GitHub Actions

A CI/CD workflow build-test.yml is included to:
- Compile all Java sources
- Run unit tests (if implemented)
- Build Docker image automatically on push
- Optionally push image to a container registry

This automates builds and ensures your project is always in a runnable state.
