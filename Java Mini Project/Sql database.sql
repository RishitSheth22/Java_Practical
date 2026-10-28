-- ============================================================
-- Theme Park Management System - Seed Data
-- Run AFTER schema.sql
-- Passwords verified with actual BCrypt hashes:
--   admin  -> admin123
--   staff1 -> staff123
--   staff2 -> staff123
-- ============================================================

USE theme_park_db;

-- ============================================================
-- Users (roles: ADMIN, STAFF)
-- Hashes generated and verified at build time
-- ============================================================
INSERT INTO users (username, password_hash, full_name, email, role, active) VALUES
('admin',  '$2a$12$Ya13b4ABcF5.mp/b8rDISeG1UzWhTGUP2OLDgrrLwjA6Rh69vk/Qe', 'Admin User',  'admin@themepark.com',  'ADMIN', TRUE),
('staff1', '$2a$12$f4m.V1MKeA/OMEdnggP59OmvhP/SE.GbKExz2GbwZ6PjYC.9GTkpK', 'Raj Mehta',   'raj@themepark.com',    'STAFF', TRUE),
('staff2', '$2a$12$f4m.V1MKeA/OMEdnggP59OmvhP/SE.GbKExz2GbwZ6PjYC.9GTkpK', 'Pooja Singh', 'pooja@themepark.com',  'STAFF', TRUE)
ON DUPLICATE KEY UPDATE username=username;

-- ============================================================
-- Ticket Types
-- ============================================================
INSERT INTO ticket_types (type_name, description, unit_price, active) VALUES
('Regular', 'Standard adult entry ticket',          500.00,  TRUE),
('Child',   'Entry ticket for children under 12',   300.00,  TRUE),
('VIP',     'VIP ticket with priority access',      1000.00, TRUE),
('Senior',  'Discounted ticket for senior citizens', 400.00, TRUE),
('Student', 'Discounted ticket with student ID',    350.00,  TRUE)
ON DUPLICATE KEY UPDATE type_name=type_name;

-- ============================================================
-- Visitors  (from original SQL schema)
-- ============================================================
INSERT INTO visitors (visitor_code, full_name, gender, age, dob, phone_number, email) VALUES
('VIS-0001', 'Amit Sharma',  'Male',   22, '2004-01-15', '9876543210', 'amit@gmail.com'),
('VIS-0002', 'Neha Patil',   'Female', 21, '2005-03-20', '9876543211', 'neha@gmail.com'),
('VIS-0003', 'Rahul Joshi',  'Male',   25, '2001-06-10', '9876543212', 'rahul@gmail.com'),
('VIS-0004', 'Priya Shah',   'Female', 23, '2003-08-12', '9876543213', 'priya@gmail.com'),
('VIS-0005', 'Rohan Mehta',  'Male',   27, '1999-04-18', '9876543214', 'rohan@gmail.com'),
('VIS-0006', 'Sneha Desai',  'Female', 24, '2002-09-25', '9876543215', 'sneha@gmail.com'),
('VIS-0007', 'Karan Singh',  'Male',   26, '2000-11-30', '9876543216', 'karan@gmail.com')
ON DUPLICATE KEY UPDATE visitor_code=visitor_code;

-- ============================================================
-- Rides  (from original SQL schema + extra fields)
-- ============================================================
INSERT INTO rides (ride_name, description, category, capacity_per_slot, duration_minutes,
                   unit_price, opening_time, closing_time, operational_status, active, location_name) VALUES
('Roller Coaster', 'High-speed thrill ride with loops and drops', 'Thrill',  30, 5,  150.00, '09:00:00', '21:00:00', 'OPEN', TRUE, 'Adventure Land'),
('Ferris Wheel',   'Classic giant wheel with scenic views',       'Family',  40, 15, 100.00, '09:00:00', '22:00:00', 'OPEN', TRUE, 'Fantasy World'),
('Water Slide',    'Exciting water slide for cool fun',           'Water',   25, 8,  120.00, '10:00:00', '18:00:00', 'OPEN', TRUE, 'Water Zone'),
('Mini Train',     'Gentle train ride for kids and families',     'Kids',    20, 10, 80.00,  '09:00:00', '20:00:00', 'OPEN', TRUE, 'Kids World'),
('Bumper Cars',    'Fun bumping cars for all ages',               'Family',  24, 7,  100.00, '10:00:00', '21:00:00', 'OPEN', TRUE, 'Main Plaza'),
('Haunted House',  'Scary haunted house experience',              'Horror',  15, 12, 130.00, '14:00:00', '22:00:00', 'OPEN', TRUE, 'Horror Zone'),
('Pirate Ship',    'Swinging pirate ship thrill ride',            'Thrill',  35, 6,  140.00, '09:00:00', '21:00:00', 'OPEN', TRUE, 'Adventure Land'),
('Drop Tower',     'Freefall drop for extreme thrill seekers',    'Thrill',  16, 4,  160.00, '10:00:00', '20:00:00', 'OPEN', TRUE, 'Adventure Land')
ON DUPLICATE KEY UPDATE ride_name=ride_name;
