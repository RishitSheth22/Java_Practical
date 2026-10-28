# Theme Park Management System (Java + Spring Boot + MySQL)

Java mini project based on your DBMS Experiment 03 schema and the project slides.
Two modules: **Ticket Purchase + Ride Booking** (visitors) and **Ride Management + Ride Log** (admin/staff).

## 1. Requirements
- JDK 25 or newer (latest LTS)
- Maven 3.8+ (or open the folder in IntelliJ / Eclipse / VS Code, which handle Maven for you)
- MySQL 8.0.16+ running locally (CHECK constraints need 8.0.16+)

## 2. Connect it to YOUR database
Open `src/main/resources/application.properties` and set:

    spring.datasource.url=jdbc:mysql://localhost:3306/themepark?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    spring.datasource.username=root
    spring.datasource.password=YOUR_PASSWORD

(or set the env variables `DB_URL`, `DB_USER`, `DB_PASSWORD`). The database `themepark` is created
automatically. On every start the app runs `schema.sql` (CREATE TABLE IF NOT EXISTS) and `data.sql`
(INSERT IGNORE seed data); both are safe to re-run. Set `DB_INIT=never` to turn that off.
You can also run those two files by hand in MySQL Workbench.

## 3. Run

    mvn spring-boot:run

Then open http://localhost:8080 (visitor) and http://localhost:8080/admin.html (staff).

| Role | Login |
|---|---|
| Visitor | visitor@example.com / visitor123 (or register a new account) |
| Admin | admin@themepark.com / admin123 |
| Operator | operator@themepark.com / operator123 |

The demo visitor account also has sample ticket purchases and ride bookings seeded in the database:
`visitor@example.com` / `visitor123`. Seeded rides, slots, purchases, tickets, and bookings use repeatable
demo records and appear in the corresponding visitor and staff pages after login.

## 4. How it maps to the slides
Architecture: `controller` -> `service` -> `repository` (Spring JdbcTemplate / JDBC) -> MySQL.

**Visitor flow** (`BookingService`): login -> ticket type -> quantity -> visit date -> ride -> slot ->
capacity check -> confirm -> transaction. Capacity = slot capacity minus CONFIRMED/REVIEW seats, read from MySQL
inside one transaction with the ride and slot rows locked (`SELECT ... FOR UPDATE`, READ COMMITTED).

Combined ticket-and-ride prices use the selected ticket category and ride: the ticket's listed entry price plus
50% (Child), 100% (Standard), 150% (Express), or 200% (VIP) of that ride's listed price, per visitor.
The quantity multiplies the per-visitor total.

Edge cases implemented: insufficient capacity (rejected with a message) · last-seat race (row lock, second
request re-checked) · CLOSED/MAINTENANCE ride blocks booking before the capacity check · past/outside-window
date · quantity 0/negative/over 10 · payment failure rolls back the hold (use the "simulate payment failure"
checkbox) · session timeout (30 min, redirect to login) · duplicate booking for the same visitor + slot ·
cancellation returns the seat and records a refund transaction (negative amount).

**Admin flow** (`RideService`): add/update ride, manage slots, set status OPEN/CLOSED/MAINTENANCE, ride log.
Every status/slot/capacity change writes a `RIDE_LOG` row in the same DB transaction, so a change can't exist
without its log. Edge cases: closing a ride flags upcoming bookings as REVIEW (admin cancels = refund; reopening
restores them) · capacity reduced below confirmed bookings keeps existing bookings and applies to new requests only ·
simultaneous status updates are serialised by a row lock and both are logged · reopening needs capacity > 0 ·
log entries must stay chronological · logging for an archived ride is rejected.

## 5. Changes to your Experiment-03 schema
Same tables and columns, plus (marked "added" in `schema.sql`): `AUTO_INCREMENT` ids, `Password` on VISITOR and STAFF,
`STAFF.DeptID`, `RIDE.Price/Status/Archived`, `FEEDBACK.RideID`, `TICKET.PurchaseID`, `TICKET_PURCHASE.TransactionID`,
and new tables `TICKET_TYPE`, `RIDE_SLOT`, `RIDE_BOOKING`. The indexes from slide 16 are included.

## 6. Notes / not implemented
- Passwords are salted SHA-256, fine for a mini project; use BCrypt (Spring Security) for production.
- Payment is simulated (no real gateway). "Ride assigned to a staff member outside their department"
  (slide 10, case 8) is not enforced because the ER diagram has no ride-department link.
