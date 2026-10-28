-- Seed data (INSERT IGNORE = safe to re-run).
-- Demo logins:
--   Admin    admin@themepark.com     / admin123
--   Operator operator@themepark.com  / operator123
--   Visitor  visitor@example.com     / visitor123

INSERT IGNORE INTO DEPARTMENT (DeptID, DeptName, HOD, Location) VALUES
 (1, 'Ride Operations', 'R. Mehta',  'Central Control Room'),
 (2, 'Ticketing',       'S. Iyer',   'Main Gate'),
 (3, 'Maintenance',     'A. Khan',   'Service Yard');

INSERT IGNORE INTO LOCATION (LocationID, LocationName, Description, Area) VALUES
 (1, 'Adventure Plaza', 'Thrill rides area',        4500.00),
 (2, 'Fantasy Garden',  'Family and kids rides',    3200.00),
 (3, 'Splash Bay',      'Water rides',              2800.00);

INSERT IGNORE INTO PARK_ZONE (ZoneID, ZoneName, Theme, Capacity, DeptID, LocationID) VALUES
 (1, 'Thrill Zone',  'Extreme',  800, 1, 1),
 (2, 'Family Zone',  'Fantasy',  600, 1, 2),
 (3, 'Water Zone',   'Aquatic',  500, 3, 3);

INSERT IGNORE INTO STAFF (StaffID, Name, Role, Email, PhoneNo, Age, Password, DeptID) VALUES
 (1, 'Admin User',    'ADMIN',    'admin@themepark.com',    '9000000001', 35,
    'a1b2c3d4e5f60718$9879a6c081970a685052bdaf0cba84347009c2f6f758c7a05257f5dcd6485ca8', 1),
 (2, 'Ride Operator', 'OPERATOR', 'operator@themepark.com', '9000000002', 28,
    '0f9e8d7c6b5a4321$229f65b444d08db7de643b1a14d6fb22bd364eb1a2e039924fe7106bc2cd7589', 1);

INSERT IGNORE INTO TICKET_TYPE (TypeID, TypeName, Price, Description) VALUES
 (1, 'Standard', 500.00,  'General park entry'),
 (2, 'Child',    300.00,  'Ages 3-12'),
 (3, 'Express',  900.00,  'Priority entry'),
 (4, 'VIP',      1500.00, 'All-access with priority queues');

INSERT IGNORE INTO RIDE (RideID, RideName, Type, Capacity, LocationID, Price, Status, Archived) VALUES
 (1, 'Sky Coaster',      'Thrill', 30, 1, 150.00, 'OPEN', 0),
 (2, 'Dragon Drop',      'Thrill', 24, 1, 120.00, 'OPEN', 0),
 (3, 'Fantasy Carousel', 'Family', 40, 2,  50.00, 'OPEN', 0),
 (4, 'Splash Rapids',    'Water',  20, 3, 100.00, 'OPEN', 0);

INSERT IGNORE INTO VISITOR (VisitorID, Name, Gender, Age, DOB, PhoneNo, Email, Password) VALUES
 (1, 'Demo Visitor', 'Other', 25, '2001-01-01', '9000000009', 'visitor@example.com',
    '1122334455667788$38af4fbb38b502cbf003243b6ee9e4b1b2371f71c25c6df1c175c8947033c7e5');

-- Additional demo catalogue and visitor history (reserved high IDs; safe to re-run).
INSERT IGNORE INTO RIDE (RideID, RideName, Type, Capacity, LocationID, Price, Status, Archived) VALUES
 (5, 'Thunder Loop', 'Thrill', 32, 1, 180.00, 'OPEN', 0),
 (6, 'Enchanted Boats', 'Family', 28, 2,  80.00, 'OPEN', 0),
 (7, 'Coral Splash', 'Water', 24, 3, 110.00, 'OPEN', 0);

INSERT IGNORE INTO RIDE_SLOT (SlotID, RideID, SlotDate, StartTime, EndTime, Capacity) VALUES
 (900001, 5, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '10:00:00', '11:00:00', 32),
 (900002, 6, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '11:00:00', '12:00:00', 28);

INSERT IGNORE INTO `TRANSACTION` (TransactionID, Amount, PaymentMethod, TransactionDate) VALUES
 (900001, 1360.00, 'CARD', CURRENT_DATE),
 (900002,  340.00, 'UPI', CURRENT_DATE);

INSERT IGNORE INTO TICKET_PURCHASE
 (PurchaseID, PurchaseDate, PaymentMode, TotalAmount, VisitorID, TransactionID)
SELECT 900001, CURRENT_DATE, 'CARD', 1360.00, VisitorID, 900001
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO TICKET_PURCHASE
 (PurchaseID, PurchaseDate, PaymentMode, TotalAmount, VisitorID, TransactionID)
SELECT 900002, CURRENT_DATE, 'UPI', 340.00, VisitorID, 900002
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO TICKET
 (TicketID, TicketType, Price, IssueDate, ValidToDate, VisitorID, PurchaseID)
SELECT 900001, 'Standard', 500.00, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), VisitorID, 900001
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO TICKET
 (TicketID, TicketType, Price, IssueDate, ValidToDate, VisitorID, PurchaseID)
SELECT 900002, 'Standard', 500.00, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), VisitorID, 900001
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO TICKET
 (TicketID, TicketType, Price, IssueDate, ValidToDate, VisitorID, PurchaseID)
SELECT 900003, 'Child', 300.00, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), VisitorID, 900002
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO RIDE_BOOKING
 (BookingID, VisitorID, SlotID, PurchaseID, Seats, Amount, Status)
SELECT 900001, VisitorID, 900001, 900001, 2, 360.00, 'CONFIRMED'
FROM VISITOR WHERE Email = 'visitor@example.com';

INSERT IGNORE INTO RIDE_BOOKING
 (BookingID, VisitorID, SlotID, PurchaseID, Seats, Amount, Status)
SELECT 900002, VisitorID, 900002, 900002, 1, 40.00, 'CONFIRMED'
FROM VISITOR WHERE Email = 'visitor@example.com';
