# Інформаційна система моніторингу якості води

Курсова робота з кросплатформеного програмування, варіант № 8, група 202-ТН.
Матеріали другої контрольної точки.

## Структура

- `server/` — Maven-проєкт Java 21, моделі, CRUD через JDBC і сервісний рівень.
- `client/` — окремий Maven-проєкт Java 21 з початковим файлом `sample-data/measurements.csv`.
- `database.sql` — схема MySQL і демонстраційні дані.
- `diagrams/` — вихідні файли схем diagrams.net і зображення PNG.

## Перевірка Maven-проєктів

Перевірте версії: `java -version`, `javac -version` і `mvn -version`.
У всіх трьох командах Java має бути версії 21. З кореня проєкту виконайте:

```powershell
mvn -f server/pom.xml clean compile
mvn -f client/pom.xml clean compile
```

У XAMPP запустіть Apache і MySQL. Відкрийте `http://localhost/phpmyadmin`,
виберіть «Імпорт» і завантажте `database.sql`. Скрипт видаляє і заново створює
лише навчальну базу `kpp_water_quality_v08`. Використовуйте його для демонстраційних даних.
Після імпорту в БД має бути 4 таблиці: `observation_point` (2 записи),
`water_sample` (2), `measurement` (3) і `sample_check` (1).

`DBUtil.getConnection()` за замовчуванням підключається до
`jdbc:mysql://localhost:3306/kpp_water_quality_v08` як локальний користувач `root`
із порожнім паролем. Інші параметри можна задати в середовищі процесу Java
через `KPP_DB_URL`, `KPP_DB_USER` і `KPP_DB_PASSWORD`.
Якщо IntelliJ IDEA використовує окрему конфігурацію запуску, ці змінні можна
задати в її полі Environment variables.

## Початкові дані CSV

У `client/sample-data/measurements.csv` — два додаткові вимірювання для
зареєстрованої проби `S-002` (ID 2 у демонстраційній БД). Кодування UTF-8,
роздільник — крапка з комою, десятковий знак — крапка. Вимірювання і контрольні
межі в SQL та CSV є навчальними значеннями.

## Серверні операції другого етапу

У пакеті `ua.edu.nupp.kpp.server` класи `ObservationPointService`,
`WaterSampleService`, `MeasurementService` і `SampleCheckService` надають
операції `add`, `getAll`, `getById`, `update` і `delete`.
`WaterSampleService.calculateRisk(1)` для початкової проби `S-001`
повертає показник `50.00`. `WaterSampleService.changeState` перевіряє
допустимість переходу й умови для наступного стану.

