-- ====================================================================
--  THEME PARK MANAGEMENT SYSTEM  -  MySQL schema
--  Based on DBMS Experiment 03 (DDL), extended with the tables and
--  columns the Java application needs (marked "added").
--  Safe to re-run: every statement is CREATE TABLE IF NOT EXISTS.
-- ====================================================================

CREATE TABLE IF NOT EXISTS VISITOR (
    VisitorID INT AUTO_INCREMENT PRIMARY KEY,
    Name      VARCHAR(50)  NOT NULL,
    Gender    VARCHAR(10),
    Age       INT,
    DOB       DATE,
    PhoneNo   VARCHAR(15),
    Email     VARCHAR(100) NOT NULL,
    Password  VARCHAR(200) NOT NULL,              -- added: login
    UNIQUE KEY uq_visitor_email (Email)           -- index: VISITOR(email)
);

CREATE TABLE IF NOT EXISTS DEPARTMENT (
    DeptID   INT AUTO_INCREMENT PRIMARY KEY,
    DeptName VARCHAR(50) NOT NULL,
    HOD      VARCHAR(50),
    Location VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS LOCATION (
    LocationID   INT AUTO_INCREMENT PRIMARY KEY,
    LocationName VARCHAR(50) NOT NULL,
    Description  VARCHAR(255),
    Area         DECIMAL(10,2)
);

CREATE TABLE IF NOT EXISTS `TRANSACTION` (
    TransactionID   INT AUTO_INCREMENT PRIMARY KEY,
    Amount          DECIMAL(10,2),                -- negative amount = refund
    PaymentMethod   VARCHAR(30),
    TransactionDate DATE,
    INDEX idx_txn_date (TransactionDate)          -- index: daily revenue
);

CREATE TABLE IF NOT EXISTS STAFF (
    StaffID  INT AUTO_INCREMENT PRIMARY KEY,
    Name     VARCHAR(50) NOT NULL,
    Role     VARCHAR(50),
    Email    VARCHAR(100),
    PhoneNo  VARCHAR(15),
    Age      INT,
    Password VARCHAR(200) NOT NULL DEFAULT '',    -- added: admin login
    DeptID   INT NULL,                            -- added: STAFF <-> DEPARTMENT
    UNIQUE KEY uq_staff_email (Email),
    FOREIGN KEY (DeptID) REFERENCES DEPARTMENT(DeptID)
);

CREATE TABLE IF NOT EXISTS TICKET_TYPE (          -- added: ticket catalogue
    TypeID      INT AUTO_INCREMENT PRIMARY KEY,
    TypeName    VARCHAR(30)  NOT NULL,
    Price       DECIMAL(10,2) NOT NULL,
    Description VARCHAR(150),
    UNIQUE KEY uq_ticket_type_name (TypeName)
);

CREATE TABLE IF NOT EXISTS TICKET_PURCHASE (
    PurchaseID    INT AUTO_INCREMENT PRIMARY KEY,
    PurchaseDate  DATE,
    PaymentMode   VARCHAR(30),
    TotalAmount   DECIMAL(10,2),
    VisitorID     INT,
    TransactionID INT NULL,                       -- added: link to payment
    FOREIGN KEY (VisitorID)     REFERENCES VISITOR(VisitorID),
    FOREIGN KEY (TransactionID) REFERENCES `TRANSACTION`(TransactionID),
    INDEX idx_purchase_visitor (VisitorID, PurchaseDate)
);

CREATE TABLE IF NOT EXISTS TICKET (
    TicketID    INT AUTO_INCREMENT PRIMARY KEY,
    TicketType  VARCHAR(30),
    Price       DECIMAL(10,2),
    IssueDate   DATE,
    ValidToDate DATE,
    VisitorID   INT,
    DeptID      INT NULL,
    PurchaseID  INT NULL,                         -- added: ticket <- purchase
    FOREIGN KEY (VisitorID)  REFERENCES VISITOR(VisitorID),
    FOREIGN KEY (DeptID)     REFERENCES DEPARTMENT(DeptID),
    FOREIGN KEY (PurchaseID) REFERENCES TICKET_PURCHASE(PurchaseID),
    INDEX idx_ticket_valid (VisitorID, ValidToDate)   -- "is my ticket valid?"
);

CREATE TABLE IF NOT EXISTS RIDE (
    RideID     INT AUTO_INCREMENT PRIMARY KEY,
    RideName   VARCHAR(50) NOT NULL,
    Type       VARCHAR(30),
    Capacity   INT NOT NULL DEFAULT 0,
    PurchaseID INT NULL,
    LocationID INT NULL,
    Price      DECIMAL(10,2) NOT NULL DEFAULT 0,                 -- added: per-seat price
    Status     ENUM('OPEN','CLOSED','MAINTENANCE') NOT NULL DEFAULT 'OPEN',  -- added
    Archived   TINYINT(1) NOT NULL DEFAULT 0,                    -- added: soft delete
    CHECK (Capacity >= 0),
    FOREIGN KEY (PurchaseID) REFERENCES TICKET_PURCHASE(PurchaseID),
    FOREIGN KEY (LocationID) REFERENCES LOCATION(LocationID)
);

CREATE TABLE IF NOT EXISTS RIDE_SLOT (            -- added
    SlotID    INT AUTO_INCREMENT PRIMARY KEY,
    RideID    INT  NOT NULL,
    SlotDate  DATE NOT NULL,
    StartTime TIME NOT NULL,
    EndTime   TIME NOT NULL,
    Capacity  INT  NOT NULL,
    CHECK (Capacity > 0),
    FOREIGN KEY (RideID) REFERENCES RIDE(RideID),
    UNIQUE KEY uq_slot (RideID, SlotDate, StartTime)   -- index: RIDE_SLOT(ride, date, start)
);

CREATE TABLE IF NOT EXISTS RIDE_BOOKING (         -- added
    BookingID           INT AUTO_INCREMENT PRIMARY KEY,
    VisitorID           INT NOT NULL,
    SlotID              INT NOT NULL,
    PurchaseID          INT NULL,
    Seats               INT NOT NULL,
    Amount              DECIMAL(10,2) NOT NULL DEFAULT 0,
    Status              ENUM('CONFIRMED','REVIEW','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    BookingDate         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    RefundTransactionID INT NULL,
    CHECK (Seats > 0),
    FOREIGN KEY (VisitorID)           REFERENCES VISITOR(VisitorID),
    FOREIGN KEY (SlotID)              REFERENCES RIDE_SLOT(SlotID),
    FOREIGN KEY (PurchaseID)          REFERENCES TICKET_PURCHASE(PurchaseID),
    FOREIGN KEY (RefundTransactionID) REFERENCES `TRANSACTION`(TransactionID),
    INDEX idx_booking_slot    (SlotID, Status),          -- overbooking / seats left
    INDEX idx_booking_visitor (VisitorID, BookingDate)   -- "my bookings"
);

CREATE TABLE IF NOT EXISTS FEEDBACK (
    FeedbackID   INT AUTO_INCREMENT PRIMARY KEY,
    FeedbackText VARCHAR(500),
    Rating       INT,
    FeedbackDate DATE,
    VisitorID    INT,
    RideID       INT NULL,                        -- added: rating per ride
    CHECK (Rating BETWEEN 1 AND 5),
    FOREIGN KEY (VisitorID) REFERENCES VISITOR(VisitorID),
    FOREIGN KEY (RideID)    REFERENCES RIDE(RideID)
);

CREATE TABLE IF NOT EXISTS PARK_ZONE (
    ZoneID     INT AUTO_INCREMENT PRIMARY KEY,
    ZoneName   VARCHAR(50) NOT NULL,
    Theme      VARCHAR(50),
    Capacity   INT,
    DeptID     INT,
    LocationID INT UNIQUE,
    FOREIGN KEY (DeptID)     REFERENCES DEPARTMENT(DeptID),
    FOREIGN KEY (LocationID) REFERENCES LOCATION(LocationID)
);

CREATE TABLE IF NOT EXISTS RIDE_LOG (
    LogID         INT AUTO_INCREMENT PRIMARY KEY,
    LogTime       DATETIME,
    OperationType VARCHAR(50),
    RideID        INT,
    TransactionID INT NULL,
    StaffID       INT,
    Details       VARCHAR(255),                   -- added: note about the action
    FOREIGN KEY (RideID)        REFERENCES RIDE(RideID),
    FOREIGN KEY (TransactionID) REFERENCES `TRANSACTION`(TransactionID),
    FOREIGN KEY (StaffID)       REFERENCES STAFF(StaffID),
    INDEX idx_ridelog (RideID, LogTime)           -- audit queries
);
