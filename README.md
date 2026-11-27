# Інформаційна система моніторингу якості води

Курсова робота з кросплатформеного програмування, варіант № 8, група 202-ТН.
Програма веде облік пунктів спостереження, проб води, вимірювань і перевірок.
JavaFX-клієнт дозволяє виконувати CRUD, розраховувати інтегральний показник,
керувати життєвим циклом проб та імпортувати CSV у фоновому режимі.

## Що потрібно

- Java Development Kit 21 і Maven.
- Apache Tomcat 9.x з Java 21.
- MySQL або сумісний сервер із XAMPP.
- JavaFX 21.0.2, Jackson 2.18.2 та JUnit Jupiter 5.11.4 (через Maven).

## Структура

- `server/` — Servlet API, Service, DAO, JDBC та модульні тести.
- `client/` — JavaFX, HTTP-клієнт, CSV та тести парсера.
- `database.sql` — схема і демонстраційні дані.
- `client/sample-data/measurements.csv` — два нові контрольні вимірювання.
- `diagrams/` — архітектура, схема бази та діаграма станів.

## Як запустити

1. У XAMPP увімкніть MySQL. Якщо імпортуєте через phpMyAdmin, увімкніть також
   Apache й відкрийте http://localhost/phpmyadmin.
2. Імпортуйте кореневий файл database.sql у новій навчальній базі. Скрипт видаляє
   і заново створює kpp_water_quality_v08. Перед повторним виконанням збережіть
   резервну копію потрібних даних. Початкова схема містить чотири таблиці:
   observation_point, water_sample, measurement і sample_check.
3. У терміналі кореневої папки виконайте «mvn -f server/pom.xml clean package».
   Готовий файл — server/target/water-quality-server.war.
4. Зупиніть попередній екземпляр Tomcat, якщо він уже запущений, і скопіюйте
   актуальний WAR у папку webapps. Запустіть bin/catalina.bat run
   на Windows або bin/catalina.sh run на Linux та macOS.
5. Відкрийте http://localhost:8080/water-quality-server/points. Правильна
   відповідь — JSON-масив із пунктами спостереження.
6. В іншому терміналі виконайте «mvn -f client/pom.xml javafx:run».

Параметри JDBC за замовчуванням — localhost:3306, користувач root і порожній
пароль. Для власного середовища задайте KPP_DB_URL, KPP_DB_USER і
KPP_DB_PASSWORD перед запуском Tomcat. Для клієнта KPP_API_URL змінює базову
адресу http://localhost:8080/water-quality-server.

В IntelliJ IDEA можна окремо імпортувати server/pom.xml і client/pom.xml як
Maven-проєкти з JDK 21. Збірка сервера доступна через Lifecycle → package.
Для клієнта у вікні Maven виконайте javafx:run.

## Запуск у Windows через IntelliJ IDEA (покроково)

1. У XAMPP запустіть **MySQL**. Apache потрібен лише для phpMyAdmin.
   Не запускайте вбудований Tomcat XAMPP, якщо використовуєте окремий Tomcat 9:
   два сервери не повинні одночасно використовувати порт 8080.
2. Імпортуйте `database.sql` через phpMyAdmin тільки для початкової підготовки
   бази. Повторний імпорт видаляє попередні записи цієї бази.
3. У панелі Maven розгорніть **server → Lifecycle → package** або виконайте
   з кореня проєкту:

```powershell
mvn -f server/pom.xml clean package
```

   Після `BUILD SUCCESS` з'явиться `server/target/water-quality-server.war`.
   Якщо команда mvn недоступна у терміналі, скористайтеся Maven-панеллю IntelliJ IDEA.

4. Зупиніть попередній запуск Tomcat (Ctrl+C у його консолі).
   У терміналі PowerShell, відкритому в корені проєкту, виконайте
   (шляхи наведено як приклад; замініть їх на свої, якщо потрібно):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\latest\jdk-21"
$tomcat = "C:\apache-tomcat-9.0.124"
Copy-Item ".\server\target\water-quality-server.war" "$tomcat\webapps\"
& "$tomcat\bin\catalina.bat" run
```

   У JAVA_HOME вказують папку JDK, а не її підпапку bin.
   Команда Copy-Item у разі успіху зазвичай не виводить повідомлень.
   Не перейменовуйте WAR: його ім'я визначає контекст /water-quality-server.
   Залиште термінал відкритим після повідомлення `Server startup in ... milliseconds`.

5. Відкрийте [контрольний URL](http://localhost:8080/water-quality-server/points).
   Очікується HTTP 200 з JSON-масивом, а не просто стартова сторінка Tomcat.
6. У Maven-панелі розгорніть **client → Plugins → javafx → javafx:run**
   або в іншому терміналі з кореня проєкту виконайте:

```powershell
mvn -f client/pom.xml javafx:run
```

   Якщо видно лише server, додайте client/pom.xml як окремий Maven-проєкт.
7. У вкладці «Вимірювання» натисніть «Імпортувати CSV» та виберіть
   `client/sample-data/measurements.csv` у системному вікні вибору файла.
   Відкриття цього вікна є штатною поведінкою. На початкових даних очікується
   3 → 5 записів: два збережені рядки, жодного пропущеного.

### Діагностика запуску

| Симптом | Що перевірити |
| --- | --- |
| Neither the JAVA_HOME nor the JRE_HOME environment variable is defined | Задати JAVA_HOME в тому ж терміналі перед запуском Tomcat |
| HTTP 404 на /water-quality-server/points | Перевірити файл water-quality-server.war у webapps, контекст застосунку та журнал розгортання |
| HTTP 500 | Перевірити MySQL, наявність бази та реквізити JDBC |
| Connection refused | Перевірити, чи працює Tomcat, порт і KPP_API_URL |
| Порт 8080 зайнятий | Не запускати одночасно два Tomcat; зупинити зайвий сервер або налаштувати інший порт і KPP_API_URL |
| javafx:run відсутній | Підключити client/pom.xml, оновити Maven та перевірити JDK 21 |
| Повторний імпорт CSV відхилено | Перевірити, чи ці вимірювання вже існують у пробі; дублювання заборонене |

## HTTP-маршрути

| Шлях | Дані |
| --- | --- |
| /points | Пункти спостереження |
| /samples | Проби води |
| /measurements | Вимірювання |
| /checks | Перевірки |

GET повертає JSON-масив. Параметр id дозволяє отримати один запис. Для
змін використовуйте POST з application/x-www-form-urlencoded і параметром
action зі значенням add, update або delete. Для update і delete
потрібен id. При створенні відповідь має HTTP 201 та поле «id».

GET /samples?action=risk&id=1 обчислює показник для проби S-001.
Для початкових даних I = 50.00. POST /samples з action=transition, id і
state виконує зміну стану. Бізнес-правила перевіряє сервісний рівень.

## CSV

Виберіть «Вимірювання» → «Імпортувати CSV» і відкрийте
`client/sample-data/measurements.csv` (або `sample-data/measurements.csv`,
якщо клієнт запущений із каталогу client). Заголовок має точно відповідати
`sample_id;parameter_name;unit;measured_value;limit_value`.
Формат — UTF-8, роздільник «;», десяткова крапка.

Task обробляє всі непорожні рядки. Індикатор показує частку опрацьованих
рядків, а лічильник збережених збільшується лише після успішної відповіді HTTP.
Рядки з некоректним числом або кількістю полів пропускаються. Неправильний
заголовок, недоступний файл або HTTP-помилка припиняють імпорт. Після команди
«Скасувати» завершується поточний запит, і наступні рядки не надсилаються.
Уже збережені записи залишаються в БД. Унікальність (sample_id, parameter_name)
захищає від дублювання, тому повторний імпорт тих самих рядків буде відхилено.

Для чистої демонстраційної бази таблиця measurement має 3 записи.
Після успішного імпорту двох контрольних рядків — 5. Logger виводить події в
консоль клієнта та сервера. Після завершення або помилки таблиця оновлюється.

## Тестування

З кореня репозиторію виконайте:

```powershell
mvn -f server/pom.xml clean test
mvn -f client/pom.xml clean test
mvn -f server/pom.xml clean package
```

27 серверних тестів перевіряють формулу, граничні значення, переходи станів
та валідацію. 9 клієнтських тестів перевіряють CSV-парсер, UTF-8, заголовок
і некоректні поля. Для цих модульних тестів MySQL і Tomcat не потрібні.
Звіти Maven формуються в target/surefire-reports.

Для контрольної інтеграційної перевірки запустіть БД, Tomcat і JavaFX.
Перегляньте чотири вкладки, створіть тимчасовий пункт та пробу, додайте вимірювання
і перевірку PASSED. Стани проби змінюються REGISTERED → VALIDATED → ANALYZED →
ARCHIVED. Перевірте також відхилення пропущеного переходу. У початковому
наборі проба S-001 має F1 = 50, F2 = 50, F3 = 50 та I = 50.00.

