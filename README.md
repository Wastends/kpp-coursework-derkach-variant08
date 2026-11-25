# Інформаційна система моніторингу якості води

Курсова робота з кросплатформеного програмування, варіант № 8, група 202-ТН.
На третьому етапі сервер працює в Apache Tomcat 9, а настільний інтерфейс — у JavaFX.

## Що потрібно

- Java Development Kit 21 і Maven.
- Apache Tomcat 9.x з Java 21.
- MySQL або сумісний сервер із XAMPP.

## Як запустити

1. У XAMPP увімкніть MySQL. Якщо імпортуєте через phpMyAdmin, увімкніть також
   Apache й відкрийте http://localhost/phpmyadmin.
2. Імпортуйте кореневий файл database.sql. Він заново створює навчальну базу
   kpp_water_quality_v08 та записує демонстраційні дані.
3. У терміналі кореневої папки виконайте «mvn -f server/pom.xml clean package».
   Готовий файл — server/target/water-quality-server.war.
4. Скопіюйте WAR у папку webapps вашого Tomcat. Запустіть bin/catalina.bat run
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

Виберіть «Вимірювання» → «Імпортувати 1 рядок CSV» і відкрийте
client/sample-data/measurements.csv. У таблиці з’явиться перше вимірювання
з файла. Формат — UTF-8, роздільник «;», десяткова крапка. Обробка всього
файла через JavaFX Task передбачена четвертим етапом.

